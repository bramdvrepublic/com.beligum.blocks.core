/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.beligum.blocks.filesystem.hdfs.impl;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.filesystem.hdfs.TX;
import com.beligum.blocks.filesystem.hdfs.impl.sql.BlobImpl;
import com.beligum.blocks.filesystem.hdfs.impl.sql.ConnectionPoolManager;
import com.beligum.blocks.filesystem.hdfs.impl.sql.AlwaysOpenConnection;
import com.beligum.blocks.filesystem.hdfs.impl.sql.TxConnection;
import com.beligum.blocks.filesystem.hdfs.impl.sql.sqlite.SqlXAResource;
import com.beligum.blocks.filesystem.hdfs.xattr.XAttrResolver;
import com.beligum.blocks.filesystem.ifaces.XAttrFS;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.fs.local.LocalConfigKeys;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.security.AccessControlException;
import org.apache.hadoop.util.Progressable;
import org.sqlite.JDBC;
import org.sqlite.SQLiteConfig;
import org.sqlite.javax.SQLiteConnectionPoolDataSource;

import javax.transaction.xa.XAResource;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("JpaQueryApiInspection")
public class SqlFS extends AbstractFileSystem implements Closeable, XAttrFS
{
    //-----CONSTANTS-----
    public static final URI NAME = URI.create("sql:///");
    public static final String SCHEME = NAME.getScheme();
    public static final String DEFAULT_FILENAME_EXT = "db";
    public static final String DEFAULT_DATABASE_FILENAME = "data." + DEFAULT_FILENAME_EXT;

    public static final String ENABLE_TX_SUPPORT_CONFIG = "blocks.core.fs.sql.txDisabled";

    protected static final SupportedDriver DRIVER = SupportedDriver.SQLITE;
    protected static final String TX_RW_RESOURCE_NAME = "SqlFSrw";
    protected static final String TX_RO_RESOURCE_NAME = "SqlFSro";

    protected static final int DEFAULT_CONNECTION_POOL_SIZE = 5;
    protected static final int DEFAULT_PORT = -1;
    protected static final String SQL_META_TABLE_NAME = "meta_data";
    protected static final String SQL_META_INDEX_NAME = "meta_index";
    protected static final String SQL_DATA_TABLE_NAME = "value_data";

    private static final String SQL_COLUMN_PATH_NAME = "path";
    //Note: "data" and "type" are probably reserved, let's avoid them
    private static final String SQL_COLUMN_FILETYPE_NAME = "filetype";
    private static final String SQL_COLUMN_PERMISSION_NAME = "permission";
    private static final String SQL_COLUMN_USERNAME_NAME = "username";
    private static final String SQL_COLUMN_GROUPNAME_NAME = "groupname";
    private static final String SQL_COLUMN_MTIME_NAME = "mtime";
    private static final String SQL_COLUMN_ATIME_NAME = "atime";
    private static final String SQL_COLUMN_LENGTH_NAME = "length";
    private static final String SQL_COLUMN_REPLICATION_NAME = "replication";
    private static final String SQL_COLUMN_BLOCKSIZE_NAME = "blocksize";
    private static final String SQL_COLUMN_VERIFY_NAME = "verify";
    private static final String SQL_COLUMN_CHECKSUM_ID_NAME = "checksum_type";
    private static final String SQL_COLUMN_CHECKSUM_SIZE_NAME = "checksum_size";

    private static final String SQL_COLUMN_BLOCK_NUMBER_NAME = "block_number";
    //Note: "data" and "type" are probably reserved, let's avoid them
    private static final String SQL_COLUMN_CONTENT_NAME = "content";

    private static final String DEFAULT_USERNAME = null;
    private static final String DEFAULT_GROUPNAME = null;
    private static final short DEFAULT_REPLICATION = 1;
    private static final boolean DEFAULT_VERIFY_CHECKSUM = false;

    private enum SupportedDriver
    {
        SQLITE();

        SupportedDriver()
        {
        }
    }

    private enum FileType
    {
        //keep these ids in sync with the valueOf implementation below
        FILE(1),
        DIRECTORY(2);

        //don't change the ids or the DB will be corrupt!
        private short id;
        FileType(int id)
        {
            this.id = (short) id;
        }
        public short getId()
        {
            return id;
        }

        public static FileType valueOf(int id)
        {
            int ordinal = id - 1;

            if (ordinal < 0 || ordinal >= values().length) {
                throw new IllegalArgumentException("id=" + id + " out of range [0, " + values().length + ")");
            }
            //let's just use the ordinal number internally
            return values()[ordinal];
        }
    }

    //-----VARIABLES-----
    // Ensures that close routine is invoked at most once.
    private final AtomicBoolean closeGuard = new AtomicBoolean();

    private final URI uri;
    private final boolean readOnly;
    private boolean enableTxSupport;
    private java.nio.file.Path dbFile;
    private boolean resumed;
    private ConnectionPoolManager roConnectionPoolManager;
    private SQLiteConnectionPoolDataSource roDatasource;
    private ConnectionPoolManager rwConnectionPoolManager;
    private SQLiteConnectionPoolDataSource rwDatasource;
    private Connection cachedRoConnection;
    private Object cachedRoConnectionLock = new Object();
    private Connection cachedRwConnection;
    private Object cachedRwConnectionLock = new Object();
    private XAttrResolver xAttrResolver;

    //-----CONSTRUCTORS-----
    /**
     * Based on https://github.com/apache/ignite/blob/master/modules/hadoop/src/main/java/org/apache/ignite/hadoop/fs/v2/IgniteHadoopFileSystem.java
     */
    public SqlFS(final URI uri, final Configuration conf) throws IOException, URISyntaxException
    {
        this(uri, conf, false);
    }
    protected SqlFS(final URI uri, final Configuration conf, final boolean readOnly) throws IOException, URISyntaxException
    {
        super(uri, uri.getScheme(), false, DEFAULT_PORT);

        this.uri = uri;
        this.readOnly = readOnly;

        this.initialize(uri, conf);
    }

    //-----PUBLIC METHODS-----
    @Override
    public URI getUri()
    {
        return this.uri;
    }
    @Override
    public int getUriDefaultPort()
    {
        return DEFAULT_PORT;
    }
    @Override
    public FsServerDefaults getServerDefaults() throws IOException
    {
        return LocalConfigKeys.getServerDefaults();
    }
    @Override
    public synchronized FSDataOutputStream createInternal(Path f, EnumSet<CreateFlag> flag, FsPermission absolutePermission, int bufferSize, short replication, long blockSize, Progressable progress,
                                                          Options.ChecksumOpt checksumOpt, boolean createParent)
                    throws AccessControlException, FileAlreadyExistsException, FileNotFoundException, ParentNotDirectoryException, UnsupportedFileSystemException, UnresolvedLinkException, IOException
    {
        this.enterBusy();

        this.checkAccess(f);

        Connection dbConnection = null;
        FSDataOutputStream retVal = null;
        boolean success = false;
        try {
            dbConnection = this.getReadWriteDbConnection("createInternal");

            // Default impl assumes that permissions do not matter
            // calling the regular create is good enough.
            // FSs that implement permissions should override this.

            if (!createParent) { // parent must exist.
                // since this.create makes parent dirs automatically
                // we must throw exception if parent does not exist.
                final FileStatus stat = getFileStatus(f.getParent());
                if (stat == null) {
                    throw new FileNotFoundException("Missing parent:" + f);
                }
                if (!stat.isDirectory()) {
                    throw new ParentNotDirectoryException("parent is not a dir:" + f);
                }
                // parent does exist - go ahead with create of file.
            }

            boolean exists = this.exists(dbConnection, f);
            boolean overwrite = flag.contains(CreateFlag.OVERWRITE);
            boolean append = flag.contains(CreateFlag.APPEND);
            boolean create = flag.contains(CreateFlag.CREATE);

            CreateFlag.validate(f, exists, flag);
            if (append) {
                CreateFlag.validateForAppend(flag);
            }

            // Default impl  assumes that permissions do not matter and
            // nor does the bytesPerChecksum  hence
            // calling the regular create is good enough.
            // FSs that implement permissions should override this.

            //append
            if (exists && append) {

                if (!exists) {
                    throw new FileNotFoundException("File " + f + " not found");
                }
                if (this.getFileStatus(f).isDirectory()) {
                    throw new IOException("Cannot append to a diretory (=" + f + " )");
                }
                retVal = new FSDataOutputStream(new BufferedOutputStream(new SQLOutputStream(dbConnection, f, exists, true, overwrite, null, blockSize, checksumOpt), bufferSize),
                                                this.statistics);
                success = true;
            }
            //create
            else {
                if (exists && !overwrite) {
                    throw new FileAlreadyExistsException("File already exists: " + f);
                }

                Path parent = f.getParent();
                //Note: since mkdir() throws an exception if the dir already exists, we need to check it
                if (parent != null && !this.exists(dbConnection, parent)) {
                    this.mkdir(parent, absolutePermission, true);
                }

                retVal = new FSDataOutputStream(new BufferedOutputStream(new SQLOutputStream(dbConnection, f, exists, false, overwrite, absolutePermission, blockSize, checksumOpt), bufferSize),
                                                this.statistics);
                success = true;
            }
        }
        catch (SQLException e) {
            throw new IOException("SQL exception while creating file " + f, e);
        }
        finally {
            // Close if failed during stream creation.
            if (!success) {
                IOUtils.closeQuietly(retVal);
                this.releaseConnection("createInternal", dbConnection);
            }

            leaveBusy();
        }

        return retVal;
    }
    @Override
    public synchronized void mkdir(Path dir, FsPermission permission, boolean createParent)
                    throws AccessControlException, FileAlreadyExistsException, FileNotFoundException, UnresolvedLinkException, IOException
    {
        this.enterBusy();

        this.checkAccess(dir);

        Connection dbConnection = null;
        try {
            dbConnection = this.getReadWriteDbConnection("mkdir");

            Path parentDir = dir.getParent();
            boolean parentExists = false;
            if (parentDir != null) {
                parentExists = this.exists(dbConnection, parentDir);
                if (parentExists && !this.getFileStatus(parentDir).isDirectory()) {
                    throw new ParentNotDirectoryException("Parent path is not a directory: " + parentDir);
                }
            }

            boolean exists = this.exists(dbConnection, dir);
            if (exists && !this.getFileStatus(dir).isDirectory()) {
                throw new FileNotFoundException("Destination exists and is not a directory: " + dir);
            }

            if (createParent && !parentExists && parentDir != null) {
                this.mkdir(parentDir, permission, createParent);
            }

            if (!exists) {
                this.doInsert(dbConnection, dir, null, FileType.DIRECTORY, permission);
            }
        }
        catch (SQLException e) {
            throw new IOException("Error while executing mkdir for " + dir, e);
        }
        finally {
            this.releaseConnection("mkdir", dbConnection);
            this.leaveBusy();
        }

    }
    @Override
    public synchronized boolean delete(Path f, boolean recursive) throws AccessControlException, FileNotFoundException, UnresolvedLinkException, IOException
    {
        this.enterBusy();

        this.checkAccess(f);

        boolean retVal;

        Connection dbConnection = null;
        try {
            dbConnection = this.getReadWriteDbConnection("delete");

            boolean exists = this.exists(dbConnection, f);
            if (!exists) {
                throw new FileNotFoundException("File " + f + " not found");
            }

            retVal = this.doDelete(dbConnection, f, recursive) > 0;
        }
        catch (SQLException e) {
            throw new IOException("Error while deleting " + f, e);
        }
        finally {
            this.releaseConnection("delete", dbConnection);
            this.leaveBusy();
        }

        return retVal;
    }
    @Override
    public synchronized FSDataInputStream open(Path f, int bufferSize) throws AccessControlException, FileNotFoundException, UnresolvedLinkException, IOException
    {
        this.enterBusy();

        this.checkAccess(f);

        Connection dbConnection = null;
        FSDataInputStream retVal = null;
        boolean success = false;
        try {
            dbConnection = this.getReadOnlyDbConnection("open");

            boolean exists = this.exists(dbConnection, f);
            if (!exists) {
                throw new FileNotFoundException("File " + f + " not found");
            }

            retVal = new FSDataInputStream(new BufferedFSInputStream(new SQLInputStream(dbConnection, f), bufferSize));
            success = true;
        }
        catch (SQLException e) {
            throw new IOException("Error while opening " + f, e);
        }
        finally {
            // Close if failed during stream creation.
            if (!success) {
                IOUtils.closeQuietly(retVal);
                this.releaseConnection("open", dbConnection);
            }

            this.leaveBusy();
        }

        return retVal;
    }
    @Override
    public synchronized boolean setReplication(Path f, short replication) throws AccessControlException, FileNotFoundException, UnresolvedLinkException, IOException
    {
        this.enterBusy();

        this.checkAccess(f);

        Connection dbConnection = null;
        boolean retVal = false;
        try {
            dbConnection = this.getReadWriteDbConnection("setReplication");

            boolean exists = this.exists(dbConnection, f);
            if (!exists) {
                throw new FileNotFoundException("File " + f + " not found");
            }

            try (PreparedStatement stmt = dbConnection.prepareStatement("UPDATE " + SQL_META_TABLE_NAME +
                                                                        " SET " + SQL_COLUMN_REPLICATION_NAME + "=?" +
                                                                        " WHERE " + SQL_COLUMN_PATH_NAME + "=?")) {
                stmt.setShort(1, replication);
                stmt.setString(2, pathToSql(f));
                retVal = stmt.executeUpdate() == 1;
            }
        }
        catch (SQLException e) {
            throw new IOException("Error while setting replication for " + f, e);
        }
        finally {
            this.releaseConnection("setReplication", dbConnection);
            this.leaveBusy();
        }

        return retVal;
    }
    @Override
    public synchronized void renameInternal(Path src, Path dst)
                    throws AccessControlException, FileAlreadyExistsException, FileNotFoundException, ParentNotDirectoryException, UnresolvedLinkException, IOException
    {
        this.enterBusy();

        this.checkAccess(src);

        Connection dbConnection = null;
        int retVal;
        try {
            dbConnection = this.getReadWriteDbConnection("renameInternal");

            boolean exists = this.exists(dbConnection, src);
            if (!exists) {
                throw new FileNotFoundException("File " + src + " not found");
            }

            if (this.getFileStatus(src).isDirectory()) {

                String pathName = pathToSql(src);
                if (pathName.equals(Path.SEPARATOR)) {
                    throw new IOException("Can't rename the root directory");
                }

                //Note: root exception doesn't count here; see check above
                String pathNameRec = pathName + Path.SEPARATOR;

                String partTwoSubquery = " SET " + SQL_COLUMN_PATH_NAME + "=? || " + this.getSqlSubstrFunction(SQL_COLUMN_PATH_NAME, "?") +
                                         " WHERE " + SQL_COLUMN_PATH_NAME + "=? OR " + this.getSqlGlobFunction(SQL_COLUMN_PATH_NAME, "?");

                String[] queries = {
                                "UPDATE " + SQL_META_TABLE_NAME + partTwoSubquery,
                                "UPDATE " + SQL_DATA_TABLE_NAME + partTwoSubquery,
                                };

                for (String query : queries) {
                    try (PreparedStatement stmt = dbConnection.prepareStatement(query)) {
                        stmt.setString(1, pathToSql(dst));
                        stmt.setInt(2, pathName.length() + 1);
                        stmt.setString(3, pathName);
                        stmt.setInt(4, pathNameRec.length() + 1);
                        stmt.setString(5, pathNameRec);

                        retVal = stmt.executeUpdate();
                    }
                }
            }
            else {

                String partTwoSubquery = " SET " + SQL_COLUMN_PATH_NAME + "=?" +
                                         " WHERE " + SQL_COLUMN_PATH_NAME + "=?";

                String[] queries = {
                                "UPDATE " + SQL_META_TABLE_NAME + partTwoSubquery,
                                "UPDATE " + SQL_DATA_TABLE_NAME + partTwoSubquery,
                                };

                for (String query : queries) {
                    try (PreparedStatement stmt = dbConnection.prepareStatement(query)) {
                        stmt.setString(1, pathToSql(dst));
                        stmt.setString(2, pathToSql(src));

                        retVal = stmt.executeUpdate();
                    }
                }
            }
        }
        catch (SQLException e) {
            throw new IOException("Error while renaming " + src + " to " + dst, e);
        }
        finally {
            this.releaseConnection("renameInternal", dbConnection);
            this.leaveBusy();
        }
    }
    @Override
    public synchronized void setPermission(Path f, FsPermission permission) throws AccessControlException, FileNotFoundException, UnresolvedLinkException, IOException
    {
        this.enterBusy();

        this.checkAccess(f);

        Connection dbConnection = null;
        try {
            dbConnection = this.getReadWriteDbConnection("setPermission");

            boolean exists = this.exists(dbConnection, f);
            if (!exists) {
                throw new FileNotFoundException("File " + f + " not found");
            }

            try (PreparedStatement stmt = dbConnection.prepareStatement("UPDATE " + SQL_META_TABLE_NAME +
                                                                        " SET " + SQL_COLUMN_PERMISSION_NAME + "=?" +
                                                                        " WHERE " + SQL_COLUMN_PATH_NAME + "=?")) {
                stmt.setShort(1, permission.toShort());
                stmt.setString(2, pathToSql(f));
            }
        }
        catch (SQLException e) {
            throw new IOException("Error while changing permission for " + f, e);
        }
        finally {
            this.releaseConnection("setPermission", dbConnection);
            this.leaveBusy();
        }
    }
    @Override
    public synchronized void setOwner(Path f, String username, String groupname) throws AccessControlException, FileNotFoundException, UnresolvedLinkException, IOException
    {
        this.enterBusy();

        this.checkAccess(f);

        Connection dbConnection = null;
        try {
            dbConnection = this.getReadWriteDbConnection("setOwner");

            boolean exists = this.exists(dbConnection, f);
            if (!exists) {
                throw new FileNotFoundException("File " + f + " not found");
            }

            try (PreparedStatement stmt = dbConnection.prepareStatement("UPDATE " + SQL_META_TABLE_NAME +
                                                                        " SET " + SQL_COLUMN_USERNAME_NAME + "=?, " + SQL_COLUMN_GROUPNAME_NAME + "=?" +
                                                                        " WHERE " + SQL_COLUMN_PATH_NAME + "=?")) {
                stmt.setString(1, username);
                stmt.setString(2, groupname);
                stmt.setString(3, pathToSql(f));
            }
        }
        catch (SQLException e) {
            throw new IOException("Error while changing owner data for " + f, e);
        }
        finally {
            this.releaseConnection("setOwner", dbConnection);
            this.leaveBusy();
        }
    }
    @Override
    public synchronized void setTimes(Path f, long mtime, long atime) throws AccessControlException, FileNotFoundException, UnresolvedLinkException, IOException
    {
        this.enterBusy();

        this.checkAccess(f);

        Connection dbConnection = null;
        try {
            dbConnection = this.getReadWriteDbConnection("setTimes");

            boolean exists = this.exists(dbConnection, f);
            if (!exists) {
                throw new FileNotFoundException("File " + f + " not found");
            }

            try (PreparedStatement stmt = dbConnection.prepareStatement("UPDATE " + SQL_META_TABLE_NAME +
                                                                        " SET " + SQL_COLUMN_MTIME_NAME + "=?, " + SQL_COLUMN_ATIME_NAME + "=?" +
                                                                        " WHERE " + SQL_COLUMN_PATH_NAME + "=?")) {
                stmt.setLong(1, mtime);
                stmt.setLong(2, atime);
                stmt.setString(3, pathToSql(f));
            }
        }
        catch (SQLException e) {
            throw new IOException("Error while changing times for " + f, e);
        }
        finally {
            this.releaseConnection("setTimes", dbConnection);
            this.leaveBusy();
        }
    }
    @Override
    public synchronized FileChecksum getFileChecksum(Path f) throws AccessControlException, FileNotFoundException, UnresolvedLinkException, IOException
    {
        //TODO
        return null;
    }
    @Override
    public synchronized FileStatus getFileStatus(Path f) throws AccessControlException, FileNotFoundException, UnresolvedLinkException, IOException
    {
        this.enterBusy();

        this.checkAccess(f);

        Connection dbConnection = null;
        FileStatus retVal = null;
        try {
            dbConnection = this.getReadOnlyDbConnection("getFileStatus");

            boolean exists = this.exists(dbConnection, f);
            if (!exists) {
                throw new FileNotFoundException("File " + f + " not found");
            }

            try (PreparedStatement stmt = dbConnection.prepareStatement("SELECT " +
                                                                        SQL_COLUMN_PATH_NAME + "," +
                                                                        SQL_COLUMN_FILETYPE_NAME + "," +
                                                                        SQL_COLUMN_REPLICATION_NAME + "," +
                                                                        SQL_COLUMN_BLOCKSIZE_NAME + "," +
                                                                        SQL_COLUMN_MTIME_NAME + "," +
                                                                        SQL_COLUMN_ATIME_NAME + "," +
                                                                        SQL_COLUMN_PERMISSION_NAME + "," +
                                                                        SQL_COLUMN_USERNAME_NAME + "," +
                                                                        SQL_COLUMN_GROUPNAME_NAME + "," +
                                                                        SQL_COLUMN_LENGTH_NAME +
                                                                        " FROM " + SQL_META_TABLE_NAME +
                                                                        " WHERE " + SQL_COLUMN_PATH_NAME + "=?")) {
                stmt.setString(1, pathToSql(f));

                ResultSet resultSet = stmt.executeQuery();
                if (resultSet.next()) {
                    Path path = new Path(resultSet.getString(SQL_COLUMN_PATH_NAME));
                    FileType fileType = FileType.valueOf(resultSet.getShort(SQL_COLUMN_FILETYPE_NAME));
                    short replication = resultSet.getShort(SQL_COLUMN_REPLICATION_NAME);
                    long blockSize = resultSet.getShort(SQL_COLUMN_BLOCKSIZE_NAME);
                    long mtime = resultSet.getShort(SQL_COLUMN_MTIME_NAME);
                    long atime = resultSet.getShort(SQL_COLUMN_ATIME_NAME);
                    FsPermission permission = new FsPermission(resultSet.getShort(SQL_COLUMN_PERMISSION_NAME));
                    String username = resultSet.getString(SQL_COLUMN_USERNAME_NAME);
                    String groupname = resultSet.getString(SQL_COLUMN_GROUPNAME_NAME);
                    long length = resultSet.getLong(SQL_COLUMN_LENGTH_NAME);

                    retVal = new FileStatus(length, fileType.equals(FileType.DIRECTORY), replication, blockSize, mtime, atime, permission, username, groupname, path);
                }
            }
        }
        catch (SQLException e) {
            throw new IOException("Error while getting file status for " + f, e);
        }
        finally {
            this.releaseConnection("getFileStatus", dbConnection);
            this.leaveBusy();
        }

        return retVal;
    }
    @Override
    public synchronized BlockLocation[] getFileBlockLocations(Path f, long start, long len) throws AccessControlException, FileNotFoundException, UnresolvedLinkException, IOException
    {
        //TODO
        //        checkPath(f);
        //
        //        if (p == null) {
        //            throw new NullPointerException();
        //        }
        //        FileStatus file = getFileStatus(p);
        //
        //        if (file == null) {
        //            return null;
        //        }
        //
        //        if (start < 0 || len < 0) {
        //            throw new IllegalArgumentException("Invalid start or len parameter");
        //        }
        //
        //        if (file.getLen() <= start) {
        //            return new BlockLocation[0];
        //
        //        }
        //        String[] name = { "localhost:50010" };
        //        String[] host = { "localhost" };
        //        return new BlockLocation[] {
        //                        new BlockLocation(name, host, 0, file.getLen())
        //        };

        return new BlockLocation[0];
    }
    @Override
    public synchronized FsStatus getFsStatus() throws AccessControlException, FileNotFoundException, IOException
    {
        //TODO this is a bit weird because I don't know what to reply for a database... It's endless, no?
        long capacity = Long.MAX_VALUE;
        long used = 0;

        return new FsStatus(capacity, used, capacity - used);
    }
    @Override
    public synchronized FileStatus[] listStatus(Path f) throws AccessControlException, FileNotFoundException, UnresolvedLinkException, IOException
    {
        this.enterBusy();

        this.checkAccess(f);

        ArrayList<FileStatus> retVal = new ArrayList<>();

        Connection dbConnection = null;
        try {
            dbConnection = this.getReadOnlyDbConnection("listStatus");

            boolean exists = this.exists(dbConnection, f);
            if (!exists) {
                throw new FileNotFoundException("File " + f + " not found");
            }

            //From the docs:
            // List the statuses of the files/directories in the given path
            // if the path is a directory.
            boolean isDir = this.getFileStatus(f).isDirectory();
            if (isDir) {
                String pathName = pathToSql(f);
                String pathNameRec = pathName.equals(Path.SEPARATOR) ? pathName : pathName + Path.SEPARATOR;

                //Note that we can't include the path itself, which would be the case if we query the root path "/"
                String whereSql = SQL_COLUMN_PATH_NAME + "<>?" +
                                  " AND " + this.getSqlGlobFunction(SQL_COLUMN_PATH_NAME, "?") +
                                  " AND NOT " + this.getSqlGlobFunction(SQL_COLUMN_PATH_NAME, "?");

                try (PreparedStatement stmt = dbConnection.prepareStatement("SELECT " +
                                                                            SQL_COLUMN_PATH_NAME + "," +
                                                                            SQL_COLUMN_FILETYPE_NAME + "," +
                                                                            SQL_COLUMN_REPLICATION_NAME + "," +
                                                                            SQL_COLUMN_BLOCKSIZE_NAME + "," +
                                                                            SQL_COLUMN_MTIME_NAME + "," +
                                                                            SQL_COLUMN_ATIME_NAME + "," +
                                                                            SQL_COLUMN_PERMISSION_NAME + "," +
                                                                            SQL_COLUMN_USERNAME_NAME + "," +
                                                                            SQL_COLUMN_GROUPNAME_NAME + "," +
                                                                            SQL_COLUMN_LENGTH_NAME +
                                                                            " FROM " + SQL_META_TABLE_NAME +
                                                                            " WHERE " + whereSql)) {

                    stmt.setString(1, pathName);
                    stmt.setString(2, pathNameRec + "*");
                    stmt.setString(3, pathNameRec + "*/*");

                    ResultSet resultSet = stmt.executeQuery();
                    while (resultSet.next()) {
                        Path path = new Path(resultSet.getString(SQL_COLUMN_PATH_NAME));
                        FileType fileType = FileType.valueOf(resultSet.getShort(SQL_COLUMN_FILETYPE_NAME));
                        short replication = resultSet.getShort(SQL_COLUMN_REPLICATION_NAME);
                        long blockSize = resultSet.getShort(SQL_COLUMN_BLOCKSIZE_NAME);
                        long mtime = resultSet.getShort(SQL_COLUMN_MTIME_NAME);
                        long atime = resultSet.getShort(SQL_COLUMN_ATIME_NAME);
                        FsPermission permission = new FsPermission(resultSet.getShort(SQL_COLUMN_PERMISSION_NAME));
                        String username = resultSet.getString(SQL_COLUMN_USERNAME_NAME);
                        String groupname = resultSet.getString(SQL_COLUMN_GROUPNAME_NAME);
                        long length = resultSet.getLong(SQL_COLUMN_LENGTH_NAME);

                        retVal.add(new FileStatus(length, fileType.equals(FileType.DIRECTORY), replication, blockSize, mtime, atime, permission, username, groupname, path));
                    }
                }
            }
        }
        catch (SQLException e) {
            throw new IOException("Error while listing status for " + f, e);
        }
        finally {
            this.releaseConnection("listStatus", dbConnection);
            this.leaveBusy();
        }

        //zero is better than size, see https://shipilev.net/blog/2016/arrays-wisdom-ancients/
        return retVal.toArray(new FileStatus[0]);
    }
    @Override
    public synchronized void setVerifyChecksum(boolean verifyChecksum) throws AccessControlException, IOException
    {
        //NOOP
    }
    @Override
    public synchronized void close() throws IOException
    {
        if (this.closeGuard.compareAndSet(false, true)) {

            synchronized (this.cachedRoConnectionLock) {
                if (this.cachedRoConnection != null) {
                    try {
                        if (this.cachedRoConnection instanceof AlwaysOpenConnection) {
                            Logger.info("Closing ro connection of SqlFS");
                            ((AlwaysOpenConnection) this.cachedRoConnection).forceClose();
                        }
                        else {
                            this.cachedRoConnection.close();
                        }
                    }
                    catch (SQLException e) {
                        Logger.error("Error while closing the cached ro connection;", e);
                    }
                    finally {
                        this.cachedRoConnection = null;
                    }
                }
            }

            synchronized (this.cachedRwConnectionLock) {
                if (this.cachedRwConnection != null) {
                    try {
                        if (this.cachedRwConnection instanceof AlwaysOpenConnection) {
                            Logger.info("Closing rw connection of SqlFS");
                            ((AlwaysOpenConnection) this.cachedRwConnection).forceClose();
                        }
                        else {
                            this.cachedRwConnection.close();
                        }
                    }
                    catch (SQLException e) {
                        Logger.error("Error while closing the cached rw connection;", e);
                    }
                    finally {
                        this.cachedRwConnection = null;
                    }
                }
            }

            if (this.roConnectionPoolManager != null) {
                try {
                    Logger.info("Closing ro connection pool of SqlFS");
                    this.roConnectionPoolManager.dispose();
                    int activeConn = this.roConnectionPoolManager.getActiveConnections();
                    if (activeConn > 0) {
                        Logger.error("Still got " + activeConn + " left after closing up the RO connection pool manager, hope this is ok...");
                    }
                }
                catch (SQLException e) {
                    Logger.error("Error while closing the RO connection pool manager; " + this.getUri(), e);
                }
                finally {
                    this.roConnectionPoolManager = null;
                }
            }

            if (this.rwConnectionPoolManager != null) {
                try {
                    Logger.info("Closing rw connection pool of SqlFS");
                    this.rwConnectionPoolManager.dispose();
                    int activeConn = this.rwConnectionPoolManager.getActiveConnections();
                    if (activeConn > 0) {
                        Logger.error("Still got " + activeConn + " left after closing up the RW connection pool manager, hope this is ok...");
                    }
                }
                catch (SQLException e) {
                    Logger.error("Error while closing the RW connection pool manager; " + this.getUri(), e);
                }
                finally {
                    this.rwConnectionPoolManager = null;
                }
            }
        }
    }
    @Override
    public synchronized byte[] getXAttr(Path path, String name) throws IOException
    {
        if (this.xAttrResolver != null) {
            return this.xAttrResolver.getXAttr(path, name);
        }
        else {
            return super.getXAttr(path, name);
        }
    }
    @Override
    public synchronized Map<String, byte[]> getXAttrs(Path path) throws IOException
    {
        if (this.xAttrResolver != null) {
            return this.xAttrResolver.getXAttrs(path);
        }
        else {
            return super.getXAttrs(path);
        }
    }
    @Override
    public synchronized Map<String, byte[]> getXAttrs(Path path, List<String> names) throws IOException
    {
        if (this.xAttrResolver != null) {
            return this.xAttrResolver.getXAttrs(path, names);
        }
        else {
            return super.getXAttrs(path, names);
        }
    }
    @Override
    public synchronized void register(XAttrResolver xAttrResolver)
    {
        if (this.xAttrResolver != null) {
            Logger.warn("Overwriting an existing XAttrResolver, this is probably a mistake; " + this.xAttrResolver);
        }

        this.xAttrResolver = xAttrResolver;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    /**
     * Enter busy state.
     *
     * @throws IOException If file system is stopped.
     */
    private void enterBusy() throws IOException
    {
        if (this.closeGuard.get()) {
            throw new IOException("File system is stopped.");
        }
    }
    /**
     * Leave busy state.
     */
    private void leaveBusy()
    {
    }
    private void initialize(URI name, Configuration cfg) throws IOException
    {
        enterBusy();

        boolean booted = false;
        try {

            this.enableTxSupport = cfg.getBoolean(ENABLE_TX_SUPPORT_CONFIG, true);

            //The public scheme name is "sql", but internally, we'll replace it by "jdbc:xxx", where "xxx" is the specific SQL dialect
            //that's implemented by this class. In the future, we might consider changing it to "sql:xxx" with the same dialect, but for now,
            //we'll only support one: SQLite
            switch (DRIVER) {
                case SQLITE:

                    //start off with the path only, since we'll create a SQLite file URI
                    this.dbFile = Paths.get(this.getUri().getPath());
                    //an existing file means we're resuming an old session
                    this.resumed = Files.exists(this.dbFile);
                    //let's support both files and folders as URI
                    if (this.resumed) {
                        if (Files.isDirectory(this.dbFile)) {
                            this.dbFile = this.dbFile.resolve(DEFAULT_DATABASE_FILENAME);
                        }
                    }
                    else {
                        //here, we need to choose between using the supplied name as a file or a dir,
                        //let us use the extension to decide between both: if it ends with .db, it's a file, otherwise,
                        //it's a folder in which we'll create a new file
                        if (!this.dbFile.toString().endsWith("." + DEFAULT_FILENAME_EXT)) {
                            this.dbFile = this.dbFile.resolve(DEFAULT_DATABASE_FILENAME);
                        }
                    }

                    //let's double-check
                    if (Files.exists(this.dbFile) && !Files.isRegularFile(this.dbFile)) {
                        throw new IOException("The SQL connection URL path should point to a regular file; " + this.dbFile);
                    }

                    //make sure the parent exists
                    Files.createDirectories(this.dbFile.getParent());

                    //See for docs:
                    // https://gbatumbya.wordpress.com/tag/sqlite/
                    // that led to:
                    // http://www.source-code.biz/miniconnectionpoolmanager/
                    // https://sourceforge.net/p/sqlite-connpool/home/Home/
                    // but eventually, I used:
                    // https://bitbucket.org/cwdesautels/nexj-express-sqlite-adapter/src/sqlite/src/nexj/core/persistence/sql/pseudoxa
                    //
                    // Note that this doesn't really do the hard XA work; that part is implemented in getDbConnection()
                    String dataSourceUrl = JDBC.PREFIX + this.dbFile;
                    SQLiteConnectionPoolDataSource roDataSource = new SQLiteConnectionPoolDataSource();
                    roDataSource.setUrl(dataSourceUrl);
                    //Don't explicitly set this, it bugged (causes the wal file to be not deleted on cleanup)
                    //roDataSource.setReadOnly(true);

                    //save it for later use
                    this.roDatasource = roDataSource;

                    //Note: read this: https://sqlite.org/faq.html#q5
                    //this means we can't have two filesystems open and writing to the SQLite database at the same time,
                    //but we CAN have two of them reading from a SQLite database at the same time.
                    //--> that's why we decided to split up the connections into a ro pool and one rw; see below
                    //Note: while this bought us some flexibility, sometimes a lot of connections come it at once (eg; during reindexing or during recursive calls)
                    //where the connection pool would get saturated anyway (resulting in blocking behavior and a TimeoutException). To get around this,
                    //we decided to start from scratch and make things simple by implementing only one ro and one rw connection and see from there.
                    //this.roConnectionPoolManager = new MiniConnectionPoolManager(roDataSource, DEFAULT_CONNECTION_POOL_SIZE);

                    if (!this.readOnly) {
                        SQLiteConnectionPoolDataSource rwDataSource = new SQLiteConnectionPoolDataSource();

                        rwDataSource.setUrl(dataSourceUrl);

                        //set the write-ahead log journal mode (needed for XA transaction support?)
                        //Got it from the MiniConnectionPoolManager docs page: http://www.source-code.biz/miniconnectionpoolmanager/
                        //but also from the libsqlfs project with this explanation:
                        // WAL mode improves the performance of write operations (page data must only be
                        // written to disk one time) and improves concurrency by reducing blocking between
                        // readers and writers
                        rwDataSource.setJournalMode("WAL");

                        // This was here before, in v1, this is the doc in libsqlfs
                        //
                        // It is vitally important that write operations not fail to execute due
                        // to busy timeouts. Even using WAL, its still possible for a command to be
                        // blocked due to attempted concurrent write operations. If this happens
                        // without a busy handler, the write will fail and lead to corruption.
                        //
                        // Libsqlfs had attempted to do its own rudimentary busy handling via delay(),
                        // however, its implementation seems to pre-date the availablity of busy
                        // handlers in SQLite. Also, it is only used for some operations, and does not
                        // protect many operations from failure.
                        //
                        // Thus, it is preferable to register SQLite's default busy handler with a
                        // relatively high timeout to globally protect all operations. This is completely
                        // transparent to the caller, and ensure that while a write operation might be
                        // delayed for a period of time, it is unlikely that it will fail completely.
                        //
                        // An initial timeout for 10 seconds is set here, but could be increased to reduce
                        // the chances of failure under high load.
                        rwDataSource.getConfig().setBusyTimeout("10000");

                        //I assume immediate transactions is more the behavior what we expect from our TX handling
                        //for details, see https://sqlite.org/lang_transaction.html
                        //--> it goes hand in hand with our support for only one rw transaction; for now, if another transaction would be required
                        //while the main rw is already in a transaction, an exception will be thrown. If this would happen frequently,
                        //we should implement a back-off-and-retry system with a maximum timeout fallback.
                        rwDataSource.setTransactionMode(SQLiteConfig.TransactionMode.IMMEDIATE.name());

                        //taken over from libsqlfs
                        // WAL mode only performs fsync on checkpoint operation, which reduces overhead
                        // It should make it possible to run with synchronous set to NORMAL with less
                        // of a performance impact.
                        //
                        //and from the sqlite manual:
                        // Many applications choose NORMAL when in WAL mode.
                        rwDataSource.setSynchronous("NORMAL");

                        //save it for later use
                        this.rwDatasource = rwDataSource;

                        //see remark above for roConnectionPoolManager why this is commented out
                        //this.rwConnectionPoolManager = new MiniConnectionPoolManager(rwDataSource, 1);
                    }

                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported driver; " + DRIVER);
            }

            if (!this.readOnly) {

                //one-time quick and dirty, autocommitting connection to bootstrap the database structure
                try (Connection dbConnection = this.rwConnectionPoolManager != null ? this.rwConnectionPoolManager.getConnection() : this.rwDatasource.getConnection()) {

                    //In april 2018, we've optimized this implementation by splitting the metadata from the data
                    // because performance of very large databases (20GB+) was getting very bad
                    //Our inspiration: https://github.com/guardianproject/libsqlfs
                    //more specifically: https://github.com/guardianproject/libsqlfs/blob/9601168d8331e7ca488b70c1aa4111a779ac44c1/sqlfs.c#L3236
                    //with possible additional optimizations: https://stackoverflow.com/a/6533930

                    //create the main tables if they don't exist
                    ResultSet rs = dbConnection.getMetaData().getTables(null, null, SQL_META_TABLE_NAME, null);
                    if (!rs.next()) {
                        try (Statement stmt = dbConnection.createStatement()) {

                            stmt.executeUpdate("CREATE TABLE " + SQL_META_TABLE_NAME + " " +

                                               "(" + SQL_COLUMN_PATH_NAME + " TEXT NOT NULL," +
                                               " " + SQL_COLUMN_FILETYPE_NAME + " SMALLINT NOT NULL," +
                                               " " + SQL_COLUMN_PERMISSION_NAME + " INTEGER NOT NULL," +
                                               " " + SQL_COLUMN_USERNAME_NAME + " TEXT," +
                                               " " + SQL_COLUMN_GROUPNAME_NAME + " TEXT," +
                                               //SQLite: The value is a signed integer, stored in 1, 2, 3, 4, 6, or 8 bytes depending on the magnitude of the value,
                                               //so it's safe to use INTEGER for LONG (however, we've used the more traditional SQL names to facilitate other SQL implementations)
                                               " " + SQL_COLUMN_MTIME_NAME + " BIGINT NOT NULL," +
                                               " " + SQL_COLUMN_ATIME_NAME + " BIGINT NOT NULL," +
                                               " " + SQL_COLUMN_LENGTH_NAME + " BIGINT NOT NULL," +
                                               " " + SQL_COLUMN_REPLICATION_NAME + " SMALLINT NOT NULL," +
                                               " " + SQL_COLUMN_BLOCKSIZE_NAME + " BIGINT NOT NULL," +
                                               " " + SQL_COLUMN_VERIFY_NAME + " BOOLEAN NOT NULL," +
                                               " " + SQL_COLUMN_CHECKSUM_ID_NAME + " INTEGER," +
                                               " " + SQL_COLUMN_CHECKSUM_SIZE_NAME + " INTEGER," +

                                               " " + "PRIMARY KEY (" + SQL_COLUMN_PATH_NAME + ")," +
                                               " " + "UNIQUE(" + SQL_COLUMN_PATH_NAME + ")" +

                                               ")" +

                                               //See https://sqlite.org/withoutrowid.html --> "Benefits Of WITHOUT ROWID Tables"
                                               " WITHOUT ROWID" +

                                               " ;");

                            stmt.executeUpdate("CREATE INDEX " + SQL_META_INDEX_NAME + " ON" +

                                               " " + SQL_META_TABLE_NAME + "(" + SQL_COLUMN_PATH_NAME + ")" +

                                               " ;");
                        }
                    }

                    //create the main tables if they don't exist
                    rs = dbConnection.getMetaData().getTables(null, null, SQL_DATA_TABLE_NAME, null);
                    if (!rs.next()) {
                        try (Statement stmt = dbConnection.createStatement()) {

                            stmt.executeUpdate("CREATE TABLE " + SQL_DATA_TABLE_NAME + " " +

                                               "(" + SQL_COLUMN_PATH_NAME + " TEXT NOT NULL," +
                                               " " + SQL_COLUMN_BLOCK_NUMBER_NAME + " BIGINT NOT NULL," +
                                               " " + SQL_COLUMN_CONTENT_NAME + " BLOB NOT NULL," +

                                               " " + "PRIMARY KEY (" + SQL_COLUMN_PATH_NAME + ", " + SQL_COLUMN_BLOCK_NUMBER_NAME + ")," +
                                               " " + "UNIQUE(" + SQL_COLUMN_PATH_NAME + ", " + SQL_COLUMN_BLOCK_NUMBER_NAME + ")" +

                                               ")" +

                                               //See https://sqlite.org/withoutrowid.html --> "Benefits Of WITHOUT ROWID Tables"
                                               " WITHOUT ROWID" +

                                               " ;");
                        }
                    }
                }
            }

            booted = true;
        }
        catch (SQLException e) {
            throw new IOException("Error while booting up SQL connection; " + this.getUri(), e);
        }
        finally {
            //play safe and clean up if things go wrong
            if (!booted) {
                this.close();
            }

            leaveBusy();
        }
    }
    private Connection getReadOnlyDbConnection(String methodNameForDebugging) throws IOException
    {
        //Note: don't assume a read-only transaction should always return a read-only connection!
        //      when there's a RW-transaction active, it should return the RW-connection so all subsequent
        //      statement have access to the data in the transaction.
        return this.enableTxSupport ? this.getDbConnectionTX(true) : getDbConnectionNOTX(true);
    }
    private Connection getReadWriteDbConnection(String methodNameForDebugging) throws IOException
    {
        return this.enableTxSupport ? this.getDbConnectionTX(false) : getDbConnectionNOTX(false);
    }
    private Connection getDbConnectionTX(boolean readOnly) throws IOException
    {
        Connection retVal = null;

        TX tx = StorageFactory.getCurrentScopeTx();
        if (tx == null) {
            if (!readOnly) {
                throw new IOException("We're not in an active transaction context, so I can't instance an XA database connection inside the current transaction scope");
            }
            //This means we're not in a TX (so the need to possibly join a previous RW-connection is impossible),
            // but a simple read-only connection was requested, so we don't really need a TX.
            //Let's return a RO-connection to support simple RO-operations outside a transaction.
            //See https://github.com/republic-of-reinvention/com.stralo.framework/issues/48
            else {
                try {
                    if (this.roConnectionPoolManager != null) {
                        retVal = this.roConnectionPoolManager.getConnection();
                    }
                    else {
                        retVal = this.getCachedConnection(true);
                    }
                }
                catch (SQLException e) {
                    throw new IOException("Error while fetching TX SQL connection from the connection pool", e);
                }
            }
        }
        else {
            try {
                //I guess it makes sense to attach both the rw and ro connections to the transaction because it's where we expect them to be released
                String resourceName = readOnly ? TX_RO_RESOURCE_NAME : TX_RW_RESOURCE_NAME;

                //we need to synchronize on the TX object to make all access to it's resource methods atomic
                XAResource xaResource = null;
                synchronized (tx) {

                    //if there's a RW-transaction active, but a RO-transaction was requested,
                    //we return the RW-transaction to make the entire transaction session consistent
                    if (readOnly) {
                        xaResource = tx.getRegisteredResource(TX_RW_RESOURCE_NAME);
                    }

                    //note that we can stop short and opt to return a non-transactional connection here if a RO connection is requested, but no RW-transaction is active

                    //if it's still null, we follow our normal routine
                    if (xaResource == null) {
                        xaResource = tx.getRegisteredResource(resourceName);
                    }

                    if (xaResource == null) {
                        //select the right connection
                        Connection rawConn;
                        if (readOnly) {
                            if (this.roConnectionPoolManager != null) {
                                rawConn = this.roConnectionPoolManager.getConnection();
                            }
                            else {
                                rawConn = this.getCachedConnection(true);
                            }
                        }
                        else {
                            if (this.rwConnectionPoolManager != null) {
                                rawConn = this.rwConnectionPoolManager.getConnection();
                            }
                            else {
                                rawConn = this.getCachedConnection(false);
                            }
                        }

                        //Note that setAutoCommit(false) is set in SqlXAResource
                        //Note that we wrap the connection in a simple wrapper only to be able to detect it in releaseConnection()
                        tx.registerResource(resourceName, xaResource = new SqlXAResource(new TxConnection(rawConn)));
                    }
                }

                retVal = ((SqlXAResource) xaResource).getConnectionRef();
            }
            catch (SQLException e) {
                throw new IOException("Error while fetching TX SQL connection from the connection pool", e);
            }
        }

        return retVal;
    }
    private Connection getDbConnectionNOTX(boolean readOnly) throws IOException
    {
        Connection retVal;

        try {
            retVal = this.getCachedConnection(readOnly);
        }
        catch (SQLException e) {
            throw new IOException("Error while fetching NOTX SQL connection from the connection pool", e);
        }

        return retVal;
    }
    private Connection getCachedConnection(boolean readOnly) throws SQLException
    {
        Connection retVal;

        if (readOnly) {
            if (this.cachedRoConnection == null) {
                synchronized (this.cachedRoConnectionLock) {
                    if (this.cachedRoConnection == null) {
                        this.cachedRoConnection = this.roConnectionPoolManager != null ? this.roConnectionPoolManager.getConnection() : new AlwaysOpenConnection(this.roDatasource.getConnection());
                    }
                }
            }

            retVal = this.cachedRoConnection;
        }
        else {
            if (this.cachedRwConnection == null) {
                synchronized (this.cachedRwConnectionLock) {
                    if (this.cachedRwConnection == null) {
                        this.cachedRwConnection = this.rwConnectionPoolManager != null ? this.rwConnectionPoolManager.getConnection() : new AlwaysOpenConnection(this.rwDatasource.getConnection());
                    }
                }
            }

            retVal = this.cachedRwConnection;
        }

        return retVal;
    }
    private void releaseConnection(String method, Connection connection)
    {
        //note that a TxConnection is closed at the end of the TX, not here
        if (connection != null && !(connection instanceof TxConnection)) {
            try {
                if (this.cachedRoConnection != null && connection == this.cachedRoConnection) {
                    //NOOP, this is only closed in close()
                }
                else if (this.cachedRwConnection != null && connection == this.cachedRwConnection) {
                    //NOOP, this is only closed in close()
                }
                else {
                    connection.close();
                }
            }
            catch (SQLException e) {
                Logger.error("Error while releasing SQL connection; " + connection, e);
            }
        }
    }
    private void checkAccess(Path f) throws AccessControlException
    {
        //TODO
    }
    private boolean exists(Connection dbConnection, Path f) throws SQLException, IOException
    {
        boolean retVal = false;

        try (PreparedStatement stmt = dbConnection.prepareStatement("SELECT COUNT(*) FROM " + SQL_META_TABLE_NAME +
                                                                    " WHERE " + SQL_COLUMN_PATH_NAME + "=?" +
                                                                    //as soon as we find one, it exists
                                                                    " LIMIT 1"
        )) {
            stmt.setString(1, pathToSql(f));
            retVal = stmt.executeQuery().getLong(1) > 0;
        }

        return retVal;
    }
    private boolean isStreamingBlobSupported()
    {
        switch (DRIVER) {
            case SQLITE:
                //JDBC driver has no support
                return false;
            default:
                throw new UnsupportedOperationException("Unsupported driver; " + DRIVER);
        }
    }
    private String getSqlSubstrFunction(String column, String startNum)
    {
        switch (DRIVER) {
            case SQLITE:
                return "SUBSTR(" + column + "," + startNum + ")";
            default:
                throw new UnsupportedOperationException("Unsupported driver; " + DRIVER);
        }
    }
    private String getSqlSubstrFunction(String column, String startNum, String lengthNum)
    {
        switch (DRIVER) {
            case SQLITE:
                return "SUBSTR(" + column + "," + startNum + "," + lengthNum + ")";
            default:
                throw new UnsupportedOperationException("Unsupported driver; " + DRIVER);
        }
    }
    private String getSqlLeftFunction(String column, String charNum)
    {
        switch (DRIVER) {
            case SQLITE:
                return "SUBSTR(" + column + ",0," + charNum + ")";
            default:
                throw new UnsupportedOperationException("Unsupported driver; " + DRIVER);
        }
    }
    private String getSqlByteLengthFunction(String column)
    {
        switch (DRIVER) {
            case SQLITE:
                return "LENGTH(" + column + ")";
            default:
                throw new UnsupportedOperationException("Unsupported driver; " + DRIVER);
        }
    }
    private String getSqlInstrFunction(String column, String search)
    {
        switch (DRIVER) {
            case SQLITE:
                return "INSTR(" + column + "," + search + ")";
            default:
                throw new UnsupportedOperationException("Unsupported driver; " + DRIVER);
        }
    }
    private String getSqlGlobFunction(String column, String path)
    {
        switch (DRIVER) {
            case SQLITE:
                return "(" + column + " GLOB " + path + ")";
            default:
                throw new UnsupportedOperationException("Unsupported driver; " + DRIVER);
        }
    }
    private String pathToSql(Path f)
    {
        //Note that toUri() will always remove the trailing slash after a folder name (except for root),
        //so this should always returns paths without a trailing slash
        return f.toUri().getPath();
    }
    private void doSetBlob(PreparedStatement stmt, int parameterIndex, Blob blob) throws SQLException, IOException
    {
        if (this.isStreamingBlobSupported()) {
            stmt.setBlob(parameterIndex, blob);
        }
        else {
            //note that this is the equivalent of a file on disk and we ended up here because that file needs to exist,
            //so don't write null to the DB, but an empty data array instead.
            stmt.setBytes(parameterIndex, blob == null || blob.length() == 0 ? new byte[0] : blob.getBytes(1, (int) blob.length()));
        }
    }
    private int doInsert(Connection dbConnection, Path path, Blob content, FileType fileType, FsPermission permission) throws SQLException, IOException
    {
        //I hope it makes sense to store -1 to signal a default blocksize should be used and same for null als checksum options
        return this.doInsert(dbConnection, path, content, fileType, permission, -1, null);
    }
    private int doInsert(Connection dbConnection, Path path, Blob content, FileType fileType, FsPermission permission, long blockSize, Options.ChecksumOpt checksumOpt) throws SQLException, IOException
    {
        long now = System.currentTimeMillis();
        return this.doInsert(dbConnection, path, content, fileType, permission, DEFAULT_USERNAME, DEFAULT_GROUPNAME, now, now, blockSize, DEFAULT_REPLICATION, DEFAULT_VERIFY_CHECKSUM, checksumOpt);
    }
    private int doInsert(Connection dbConnection, Path path, Blob content, FileType fileType, FsPermission permission, String username, String groupname, long mtime, long atime, long blockSize,
                         short replication, boolean verifyChecksum, Options.ChecksumOpt checksumOpt) throws SQLException, IOException
    {
        int retVal;

        String sqlPath = pathToSql(path);

        try (PreparedStatement stmt = dbConnection.prepareStatement("INSERT INTO " + SQL_META_TABLE_NAME +
                                                                    " (" +
                                                                    SQL_COLUMN_PATH_NAME + "," +
                                                                    SQL_COLUMN_FILETYPE_NAME + "," +
                                                                    SQL_COLUMN_PERMISSION_NAME + "," +
                                                                    SQL_COLUMN_USERNAME_NAME + "," +
                                                                    SQL_COLUMN_GROUPNAME_NAME + "," +
                                                                    SQL_COLUMN_MTIME_NAME + "," +
                                                                    SQL_COLUMN_ATIME_NAME + "," +
                                                                    SQL_COLUMN_LENGTH_NAME + "," +
                                                                    SQL_COLUMN_REPLICATION_NAME + "," +
                                                                    SQL_COLUMN_BLOCKSIZE_NAME + "," +
                                                                    SQL_COLUMN_VERIFY_NAME + "," +
                                                                    SQL_COLUMN_CHECKSUM_ID_NAME + "," +
                                                                    SQL_COLUMN_CHECKSUM_SIZE_NAME +
                                                                    ")" +

                                                                    " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
            stmt.setString(1, pathToSql(path));
            stmt.setShort(2, fileType.getId());
            stmt.setShort(3, permission.toShort());
            stmt.setString(4, username);
            stmt.setString(5, groupname);
            stmt.setLong(6, mtime);
            stmt.setLong(7, atime);
            stmt.setLong(8, content == null ? 0 : content.length());
            stmt.setShort(9, replication);
            stmt.setLong(10, blockSize);
            stmt.setBoolean(11, verifyChecksum);
            stmt.setInt(12, checksumOpt == null ? -1 : checksumOpt.getChecksumType().id);
            stmt.setInt(13, checksumOpt == null ? -1 : checksumOpt.getBytesPerChecksum());

            retVal = stmt.executeUpdate();

            if (retVal == 1) {
                if (fileType.equals(FileType.FILE)) {
                    //let's first save the metadata and then the data so we can stop short in case there's no data (eg. folders)
                    try (PreparedStatement stmt2 = dbConnection.prepareStatement("INSERT INTO " + SQL_DATA_TABLE_NAME +
                                                                                 " (" +
                                                                                 SQL_COLUMN_PATH_NAME + "," +
                                                                                 SQL_COLUMN_BLOCK_NUMBER_NAME + "," +
                                                                                 SQL_COLUMN_CONTENT_NAME +
                                                                                 ")" +

                                                                                 " VALUES(?,?,?)")) {

                        stmt2.setString(1, sqlPath);
                        stmt2.setLong(2, 0);
                        this.doSetBlob(stmt2, 3, content);

                        retVal = stmt2.executeUpdate();
                    }
                }
            }
            else {
                throw new SQLException("Wrong return value (" + retVal + ") while saving new data for file '" + path + "'");
            }
        }

        return retVal;
    }
    private int doAppend(Connection dbConnection, Path path, Blob content) throws SQLException, IOException
    {
        int retVal;

        try (PreparedStatement stmt = dbConnection.prepareStatement("UPDATE " + SQL_DATA_TABLE_NAME +
                                                                    " SET " +
                                                                    //The || operator is "concatenate" - it joins together the two strings of its operands.
                                                                    SQL_COLUMN_CONTENT_NAME + " = " + SQL_COLUMN_CONTENT_NAME + " || ?" +
                                                                    " WHERE " + SQL_COLUMN_PATH_NAME + " = ?")) {
            this.doSetBlob(stmt, 1, content);
            stmt.setString(2, pathToSql(path));

            retVal = stmt.executeUpdate();

            //don't forget to transfer the new file size to the metadata table
            this.doSynchronizeFilesize(dbConnection, path);
        }

        return retVal;
    }
    private int doDelete(Connection dbConnection, Path path, boolean recursive) throws SQLException
    {
        int retVal = 0;

        String pathName = pathToSql(path);
        String pathNameRec = pathName.equals(Path.SEPARATOR) ? pathName : pathName + Path.SEPARATOR;
        String partTwoSubquery = " WHERE " + SQL_COLUMN_PATH_NAME + "=?";
        if (recursive) {
            //Note: 'LEFT' is much faster than 'LIKE'
            partTwoSubquery += " OR " + this.getSqlLeftFunction(SQL_COLUMN_PATH_NAME, "?") + " = ?";
        }

        String[] queries = {
                        "DELETE FROM " + SQL_DATA_TABLE_NAME + partTwoSubquery,
                        "DELETE FROM " + SQL_META_TABLE_NAME + partTwoSubquery,
                        };

        for (String query : queries) {
            try (PreparedStatement stmt = dbConnection.prepareStatement(query)) {
                stmt.setString(1, pathName);
                if (recursive) {
                    stmt.setInt(2, pathNameRec.length() + 1);
                    stmt.setString(3, pathNameRec);
                }
                //this will only return the result of the last query (the metadata),
                //hope that's ok
                retVal = stmt.executeUpdate();
            }
        }

        return retVal;
    }
    private void doSynchronizeFilesize(Connection dbConnection, Path path) throws SQLException
    {
        final String CONTENT_LENGTH_NAME = "length";

        String sqlPath = pathToSql(path);

        try (PreparedStatement stmt = dbConnection.prepareStatement("SELECT " +
                                                                    //this will report bytes
                                                                    this.getSqlByteLengthFunction(SQL_COLUMN_CONTENT_NAME) + " AS " + CONTENT_LENGTH_NAME +
                                                                    " FROM " + SQL_DATA_TABLE_NAME +
                                                                    " WHERE " + SQL_COLUMN_PATH_NAME + "=?")) {

            stmt.setString(1, sqlPath);

            ResultSet resultSet = stmt.executeQuery();
            if (resultSet.next()) {
                long length = resultSet.getLong(CONTENT_LENGTH_NAME);
                try (PreparedStatement stmt2 = dbConnection.prepareStatement("UPDATE " + SQL_META_TABLE_NAME +
                                                                             " SET " + SQL_COLUMN_LENGTH_NAME + "=?" +
                                                                             " WHERE " + SQL_COLUMN_PATH_NAME + "=?")) {
                    stmt2.setLong(1, length);
                    stmt2.setString(2, sqlPath);

                    int rowsUpdated = stmt2.executeUpdate();
                    if (rowsUpdated != 1) {
                        throw new SQLException("Couldn't find metadata entry (" + rowsUpdated + ") while updating the file size of '" + path + "'");
                    }
                }
            }
            //Note that because of the uniqueness of the path, we don't need to catch other alternatives (more than 1 row)
            else {
                throw new SQLException("Couldn't find data entry while updating the file size of '" + path + "'");
            }
        }
    }

    //-----INNER CLASSES-----
    private class SQLOutputStream extends OutputStream
    {
        private Connection dbConnection;
        private Path path;
        private boolean exists;
        private boolean append;
        private boolean overwrite;
        private FsPermission permission;
        private long blockSize;
        private Options.ChecksumOpt checksumOpt;
        private Blob blob;
        private OutputStream blobStream;

        private SQLOutputStream(Connection dbConnection, Path f, boolean exists, boolean append, boolean overwrite, FsPermission permission, long blockSize, Options.ChecksumOpt checksumOpt)
                        throws IOException
        {
            this.dbConnection = dbConnection;
            this.path = f;
            this.exists = exists;
            this.append = append;
            this.overwrite = overwrite;
            this.permission = permission;
            this.blockSize = blockSize;
            this.checksumOpt = checksumOpt;

            //see https://docs.oracle.com/javase/tutorial/jdbc/basics/blob.html
            boolean success = false;
            try {
                if (isStreamingBlobSupported()) {
                    //Note: we don't use the streaming blob API (this.blob.setBinaryStream()) to make the implementation below a bit easier,
                    // in regard to the 'fake' Blob implementation below, but if performance would be bad, we should look into this...
                    this.blob = this.dbConnection.createBlob();
                }
                else {
                    this.blob = new BlobImpl();
                }

                this.blobStream = this.blob.setBinaryStream(1);
                success = true;
            }
            catch (SQLException e) {
                throw new IOException("Error while creating blob output stream for " + this.path, e);
            }
            finally {
                if (!success && this.blobStream != null) {
                    IOUtils.closeQuietly(this.blobStream);
                    this.blobStream = null;
                }
            }
        }

        @Override
        public void close() throws IOException
        {
            //process the results
            try {
                this.blobStream.close();

                if (this.append) {
                    doAppend(this.dbConnection, this.path, this.blob);
                }
                else {
                    if (this.exists && this.overwrite) {
                        doDelete(this.dbConnection, this.path, false);
                    }
                    doInsert(this.dbConnection, this.path, this.blob, SqlFS.FileType.FILE, this.permission, this.blockSize, this.checksumOpt);
                }

                //if all went well, we need to adjust the size in the metadata table according to the real size in the data table
                doSynchronizeFilesize(this.dbConnection, this.path);
            }
            catch (SQLException e) {
                throw new IOException("Error while writing data for " + this.path, e);
            }
            finally {
                releaseConnection("SQLOutputStream", this.dbConnection);
            }
        }
        @Override
        public void flush() throws IOException
        {
            this.blobStream.flush();
        }
        @Override
        public void write(byte[] b, int off, int len) throws IOException
        {
            this.blobStream.write(b, off, len);
        }
        @Override
        public void write(int b) throws IOException
        {
            this.blobStream.write(b);
        }
    }

    private class SQLInputStream extends FSInputStream
    {
        private Connection dbConnection;
        private Path path;
        private Blob blob;
        private InputStream blobInputStream;
        private long position;

        public SQLInputStream(Connection dbConnection, Path f) throws IOException
        {
            this.dbConnection = dbConnection;
            this.path = f;

            //see https://docs.oracle.com/javase/tutorial/jdbc/basics/blob.html
            boolean success = false;
            try {
                try (PreparedStatement stmt = this.dbConnection.prepareStatement("SELECT " + SQL_COLUMN_CONTENT_NAME + " FROM " + SQL_DATA_TABLE_NAME +
                                                                                 " WHERE " + SQL_COLUMN_PATH_NAME + "=?")) {

                    stmt.setString(1, pathToSql(f));

                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        if (isStreamingBlobSupported()) {
                            this.blob = rs.getBlob(1);
                        }
                        else {
                            byte[] bytes = rs.getBytes(1);
                            this.blob = bytes == null ? new BlobImpl() : new BlobImpl(bytes);
                        }

                        this.blobInputStream = this.blob.getBinaryStream();
                    }
                }

                success = true;
            }
            catch (SQLException e) {
                throw new IOException("Error while creating blob input stream for " + this.path, e);
            }
            finally {
                if (!success && this.blobInputStream != null) {
                    IOUtils.closeQuietly(this.blobInputStream);
                }
            }
        }

        @Override
        public void seek(long pos) throws IOException
        {
            //implement this if needed
            throw new IOException("Seek not supported");

            //TODO
            //            if (pos < 0) {
            //                throw new EOFException(FSExceptionMessages.NEGATIVE_SEEK);
            //            }
            //
            //            //this.blobInputStream.skip(pos);
            //            //fis.getChannel().position(pos);
            //
            //            this.position = pos;
        }
        @Override
        public long getPos() throws IOException
        {
            return this.position;
        }
        @Override
        public boolean seekToNewSource(long targetPos) throws IOException
        {
            return false;
        }
        @Override
        public int available() throws IOException
        {
            return this.blobInputStream.available();
        }
        @Override
        public void close() throws IOException
        {
            try {
                this.blobInputStream.close();
            }
            finally {
                releaseConnection("SQLInputStream", this.dbConnection);
            }
        }
        @Override
        public boolean markSupported()
        {
            return false;
        }
        @Override
        public int read() throws IOException
        {
            int value = this.blobInputStream.read();
            if (value >= 0) {
                this.position++;
                statistics.incrementBytesRead(1);
            }

            return value;
        }
        @Override
        public int read(byte[] b, int off, int len) throws IOException
        {
            int value = this.blobInputStream.read(b, off, len);
            if (value > 0) {
                this.position += value;
                statistics.incrementBytesRead(value);
            }

            return value;
        }
        //TODO
        //        @Override
        //        public int read(long position, byte[] b, int off, int len) throws IOException
        //        {
        //            ByteBuffer bb = ByteBuffer.wrap(b, off, len);
        //
        //            int value = this.blobInputStream.getChannel().read(bb, position);
        //            if (value > 0) {
        //                statistics.incrementBytesRead(value);
        //            }
        //            return value;
        //        }
        @Override
        public long skip(long n) throws IOException
        {
            long value = this.blobInputStream.skip(n);
            if (value > 0) {
                this.position += value;
            }

            return value;
        }
        //        @Override
        //        public FileDescriptor getFileDescriptor() throws IOException
        //        {
        //            return fis.getFD();
        //        }
    }
}
