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
import com.beligum.blocks.filesystem.hdfs.xattr.XAttrResolver;
import com.beligum.blocks.filesystem.ifaces.XAttrFS;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.fs.local.LocalConfigKeys;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.security.AccessControlException;
import org.apache.hadoop.util.Progressable;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class SqlFS extends AbstractFileSystem implements Closeable, XAttrFS
{
    //-----CONSTANTS-----
    public static final URI NAME = URI.create("sql:///");
    public static final String SCHEME = NAME.getScheme();
    public static final String DEFAULT_FILENAME_EXT = "db";

    private static final int DEFAULT_PORT = -1;
    private static final String DEFAULT_DATABASE_FILENAME = "data." + DEFAULT_FILENAME_EXT;
    private static final String JDBC_SCHEME = "jdbc:sqlite";
    private static final String SQL_TABLE_NAME = "filesystem";

    private static final String SQL_COLUMN_PATH_NAME = "path";
    //Note: "data" and "type" are probably reserved, let's avoid them
    private static final String SQL_COLUMN_CONTENT_NAME = "content";
    private static final String SQL_COLUMN_FILETYPE_NAME = "filetype";
    private static final String SQL_COLUMN_PERMISSION_NAME = "permission";
    private static final String SQL_COLUMN_USERNAME_NAME = "username";
    private static final String SQL_COLUMN_GROUPNAME_NAME = "groupname";
    private static final String SQL_COLUMN_MTIME_NAME = "mtime";
    private static final String SQL_COLUMN_ATIME_NAME = "atime";
    private static final String SQL_COLUMN_REPLICATION_NAME = "replication";
    private static final String SQL_COLUMN_BLOCKSIZE_NAME = "blocksize";
    private static final String SQL_COLUMN_VERIFY_NAME = "verify";
    private static final String SQL_COLUMN_CHECKSUM_ID_NAME = "checksum_type";
    private static final String SQL_COLUMN_CHECKSUM_SIZE_NAME = "checksum_size";

    private static final String DEFAULT_USERNAME = null;
    private static final String DEFAULT_GROUPNAME = null;
    private static final short DEFAULT_REPLICATION = 1;
    private static final boolean DEFAULT_VERIFY_CHECKSUM = false;

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
    private java.nio.file.Path dbFile;
    private boolean resumed;
    private Connection dbConnection;
    protected XAttrResolver xAttrResolver;

    //-----CONSTRUCTORS-----
    /**
     * Based on https://github.com/apache/ignite/blob/master/modules/hadoop/src/main/java/org/apache/ignite/hadoop/fs/v2/IgniteHadoopFileSystem.java
     */
    public SqlFS(final URI uri, final Configuration conf) throws IOException, URISyntaxException
    {
        super(uri, SCHEME, false, DEFAULT_PORT);

        this.uri = uri;

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
    public FSDataOutputStream createInternal(Path f, EnumSet<CreateFlag> flag, FsPermission absolutePermission, int bufferSize, short replication, long blockSize, Progressable progress,
                                             Options.ChecksumOpt checksumOpt, boolean createParent)
                    throws AccessControlException, FileAlreadyExistsException, FileNotFoundException, ParentNotDirectoryException, UnsupportedFileSystemException, UnresolvedLinkException, IOException
    {
        this.enterBusy();

        this.checkAccess(f);

        FSDataOutputStream retVal = null;
        boolean success = false;
        try {
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

            boolean exists = this.exists(f);
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
                retVal =
                                new FSDataOutputStream(new BufferedOutputStream(new SQLOutputStream(this.dbConnection, f, exists, true, overwrite, null, blockSize, checksumOpt), bufferSize),
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
                if (parent != null && !this.exists(parent)) {
                    this.mkdir(parent, absolutePermission, true);
                }

                retVal =
                                new FSDataOutputStream(
                                                new BufferedOutputStream(new SQLOutputStream(this.dbConnection, f, exists, false, overwrite, absolutePermission, blockSize, checksumOpt), bufferSize),
                                                this.statistics);
                success = true;
            }
        }
        catch (SQLException e) {
            throw new IOException("SQL exception while creating file " + f, e);
        }
        finally {
            // Close if failed during stream creation.
            if (!success && retVal != null) {
                IOUtils.closeQuietly(retVal);
            }

            leaveBusy();
        }

        return retVal;
    }
    @Override
    public void mkdir(Path dir, FsPermission permission, boolean createParent) throws AccessControlException, FileAlreadyExistsException, FileNotFoundException, UnresolvedLinkException, IOException
    {
        this.enterBusy();

        this.checkAccess(dir);

        try {
            Path parentDir = dir.getParent();
            boolean parentExists = false;
            if (parentDir != null) {
                parentExists = this.exists(parentDir);
                if (parentExists && !this.getFileStatus(parentDir).isDirectory()) {
                    throw new ParentNotDirectoryException("Parent path is not a directory: " + parentDir);
                }
            }

            boolean exists = this.exists(dir);
            if (exists && !this.getFileStatus(dir).isDirectory()) {
                throw new FileNotFoundException("Destination exists and is not a directory: " + dir);
            }

            if (createParent && !parentExists && parentDir != null) {
                this.mkdir(parentDir, permission, createParent);
            }

            if (!exists) {
                this.doInsert(this.dbConnection, dir, null, FileType.DIRECTORY, permission);
            }
        }
        catch (SQLException e) {
            throw new IOException("Error while executing mkdir for " + dir, e);
        }
        finally {
            this.leaveBusy();
        }

    }
    @Override
    public boolean delete(Path f, boolean recursive) throws AccessControlException, FileNotFoundException, UnresolvedLinkException, IOException
    {
        this.enterBusy();

        this.checkAccess(f);

        boolean retVal;

        try {
            boolean exists = this.exists(f);
            if (!exists) {
                throw new FileNotFoundException("File " + f + " not found");
            }

            retVal = this.doDelete(this.dbConnection, f, recursive) > 0;
        }
        catch (SQLException e) {
            throw new IOException("Error while deleting " + f, e);
        }
        finally {
            this.leaveBusy();
        }

        return retVal;
    }
    @Override
    public FSDataInputStream open(Path f, int bufferSize) throws AccessControlException, FileNotFoundException, UnresolvedLinkException, IOException
    {
        this.enterBusy();

        this.checkAccess(f);

        FSDataInputStream retVal = null;
        boolean success = false;
        try {
            boolean exists = this.exists(f);
            if (!exists) {
                throw new FileNotFoundException("File " + f + " not found");
            }

            retVal = new FSDataInputStream(new BufferedFSInputStream(new SQLInputStream(this.dbConnection, f), bufferSize));
            success = true;
        }
        catch (SQLException e) {
            throw new IOException("Error while opening " + f, e);
        }
        finally {
            // Close if failed during stream creation.
            if (!success && retVal != null) {
                IOUtils.closeQuietly(retVal);
            }

            this.leaveBusy();
        }

        return retVal;
    }
    @Override
    public boolean setReplication(Path f, short replication) throws AccessControlException, FileNotFoundException, UnresolvedLinkException, IOException
    {
        this.enterBusy();

        this.checkAccess(f);

        boolean retVal = false;
        try {
            boolean exists = this.exists(f);
            if (!exists) {
                throw new FileNotFoundException("File " + f + " not found");
            }

            try (PreparedStatement stmt = this.dbConnection.prepareStatement("UPDATE " + SQL_TABLE_NAME +
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
            this.leaveBusy();
        }

        return retVal;
    }
    @Override
    public void renameInternal(Path src, Path dst) throws AccessControlException, FileAlreadyExistsException, FileNotFoundException, ParentNotDirectoryException, UnresolvedLinkException, IOException
    {
        this.enterBusy();

        this.checkAccess(src);

        int retVal;
        try {
            boolean exists = this.exists(src);
            if (!exists) {
                throw new FileNotFoundException("File " + src + " not found");
            }

            if (this.getFileStatus(src).isDirectory()) {

                String pathName = pathToSql(src);
                String pathNameRec = pathName + Path.SEPARATOR;

                try (PreparedStatement stmt = this.dbConnection.prepareStatement("UPDATE " + SQL_TABLE_NAME +
                                                                                 " SET " + SQL_COLUMN_PATH_NAME + "=?||" + this.getSqlSubstrFunction(SQL_COLUMN_PATH_NAME, "?") +
                                                                                 " WHERE " + SQL_COLUMN_PATH_NAME + "=? OR " + this.getSqlLeftFunction(SQL_COLUMN_PATH_NAME, "?") + " = ?")) {
                    stmt.setString(1, pathToSql(dst));
                    stmt.setInt(2, pathNameRec.length() + 1);
                    stmt.setString(3, pathName);
                    stmt.setInt(4, pathNameRec.length() + 1);
                    stmt.setString(5, pathNameRec);

                    retVal = stmt.executeUpdate();
                }
            }
            else {
                try (PreparedStatement stmt = this.dbConnection.prepareStatement("UPDATE " + SQL_TABLE_NAME +
                                                                                 " SET " + SQL_COLUMN_PATH_NAME + "=?" +
                                                                                 " WHERE " + SQL_COLUMN_PATH_NAME + "=?")) {
                    stmt.setString(1, pathToSql(dst));
                    stmt.setString(2, pathToSql(src));

                    retVal = stmt.executeUpdate();
                }
            }
        }
        catch (SQLException e) {
            throw new IOException("Error while renaming " + src + " to " + dst, e);
        }
        finally {
            this.leaveBusy();
        }
    }
    @Override
    public void setPermission(Path f, FsPermission permission) throws AccessControlException, FileNotFoundException, UnresolvedLinkException, IOException
    {
        this.enterBusy();

        this.checkAccess(f);

        try {
            boolean exists = this.exists(f);
            if (!exists) {
                throw new FileNotFoundException("File " + f + " not found");
            }

            try (PreparedStatement stmt = this.dbConnection.prepareStatement("UPDATE " + SQL_TABLE_NAME +
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
            this.leaveBusy();
        }
    }
    @Override
    public void setOwner(Path f, String username, String groupname) throws AccessControlException, FileNotFoundException, UnresolvedLinkException, IOException
    {
        this.enterBusy();

        this.checkAccess(f);

        try {
            boolean exists = this.exists(f);
            if (!exists) {
                throw new FileNotFoundException("File " + f + " not found");
            }

            try (PreparedStatement stmt = this.dbConnection.prepareStatement("UPDATE " + SQL_TABLE_NAME +
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
            this.leaveBusy();
        }
    }
    @Override
    public void setTimes(Path f, long mtime, long atime) throws AccessControlException, FileNotFoundException, UnresolvedLinkException, IOException
    {
        this.enterBusy();

        this.checkAccess(f);

        try {
            boolean exists = this.exists(f);
            if (!exists) {
                throw new FileNotFoundException("File " + f + " not found");
            }

            try (PreparedStatement stmt = this.dbConnection.prepareStatement("UPDATE " + SQL_TABLE_NAME +
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
            this.leaveBusy();
        }
    }
    @Override
    public FileChecksum getFileChecksum(Path f) throws AccessControlException, FileNotFoundException, UnresolvedLinkException, IOException
    {
        //TODO
        return null;
    }
    @Override
    public FileStatus getFileStatus(Path f) throws AccessControlException, FileNotFoundException, UnresolvedLinkException, IOException
    {
        this.enterBusy();

        this.checkAccess(f);

        FileStatus retVal = null;
        try {
            boolean exists = this.exists(f);
            if (!exists) {
                throw new FileNotFoundException("File " + f + " not found");
            }

            final String CONTENT_LENGTH_NAME = "length";
            try (PreparedStatement stmt = this.dbConnection.prepareStatement("SELECT " +
                                                                             SQL_COLUMN_PATH_NAME + "," +
                                                                             SQL_COLUMN_FILETYPE_NAME + "," +
                                                                             SQL_COLUMN_REPLICATION_NAME + "," +
                                                                             SQL_COLUMN_BLOCKSIZE_NAME + "," +
                                                                             SQL_COLUMN_MTIME_NAME + "," +
                                                                             SQL_COLUMN_ATIME_NAME + "," +
                                                                             SQL_COLUMN_PERMISSION_NAME + "," +
                                                                             SQL_COLUMN_USERNAME_NAME + "," +
                                                                             SQL_COLUMN_GROUPNAME_NAME + "," +
                                                                             //this will report bytes
                                                                             this.getSqlByteLengthFunction() + "(" + SQL_COLUMN_CONTENT_NAME + ") AS " + CONTENT_LENGTH_NAME +
                                                                             " FROM " + SQL_TABLE_NAME +
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
                    long length = resultSet.getLong(CONTENT_LENGTH_NAME);

                    retVal = new FileStatus(length, fileType.equals(FileType.DIRECTORY), replication, blockSize, mtime, atime, permission, username, groupname, path);
                }
            }
        }
        catch (SQLException e) {
            throw new IOException("Error while getting file status for " + f, e);
        }
        finally {
            this.leaveBusy();
        }

        return retVal;
    }
    @Override
    public BlockLocation[] getFileBlockLocations(Path f, long start, long len) throws AccessControlException, FileNotFoundException, UnresolvedLinkException, IOException
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
    public FsStatus getFsStatus() throws AccessControlException, FileNotFoundException, IOException
    {
        //TODO this is a bit weird because I don't know what to reply...
        long capacity = Long.MAX_VALUE;
        long used = 0;

        return new FsStatus(capacity, used, capacity - used);
    }
    @Override
    public FileStatus[] listStatus(Path f) throws AccessControlException, FileNotFoundException, UnresolvedLinkException, IOException
    {
        this.enterBusy();

        this.checkAccess(f);

        List<FileStatus> retVal = new ArrayList<>();
        try {
            boolean exists = this.exists(f);
            if (!exists) {
                throw new FileNotFoundException("File " + f + " not found");
            }

            //From the docs:
            // List the statuses of the files/directories in the given path
            // if the path is a directory.
            boolean isDir = this.getFileStatus(f).isDirectory();
            if (isDir) {
                String pathName = pathToSql(f);
                String pathNameRec = pathName + Path.SEPARATOR;

                //we search for all paths that start with "path/" and don't have a "/" in the part after that
                String whereSql = this.getSqlLeftFunction(SQL_COLUMN_PATH_NAME, "?") + " = ? " +
                                  "  AND " + this.getSqlInstrFunction("SUBSTR(" + SQL_COLUMN_PATH_NAME + ", ?)", "'" + Path.SEPARATOR + "'") + "=0";

                final String CONTENT_LENGTH_NAME = "length";
                try (PreparedStatement stmt = this.dbConnection.prepareStatement("SELECT " +
                                                                                 SQL_COLUMN_PATH_NAME + "," +
                                                                                 SQL_COLUMN_FILETYPE_NAME + "," +
                                                                                 SQL_COLUMN_REPLICATION_NAME + "," +
                                                                                 SQL_COLUMN_BLOCKSIZE_NAME + "," +
                                                                                 SQL_COLUMN_MTIME_NAME + "," +
                                                                                 SQL_COLUMN_ATIME_NAME + "," +
                                                                                 SQL_COLUMN_PERMISSION_NAME + "," +
                                                                                 SQL_COLUMN_USERNAME_NAME + "," +
                                                                                 SQL_COLUMN_GROUPNAME_NAME + "," +
                                                                                 //this will report bytes
                                                                                 this.getSqlByteLengthFunction() + "(" + SQL_COLUMN_CONTENT_NAME + ") AS " + CONTENT_LENGTH_NAME +
                                                                                 " FROM " + SQL_TABLE_NAME +
                                                                                 " WHERE " + whereSql)) {

                    stmt.setInt(1, pathNameRec.length() + 1);
                    stmt.setString(2, pathNameRec);
                    stmt.setInt(3, pathNameRec.length() + 1);

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
                        long length = resultSet.getLong(CONTENT_LENGTH_NAME);

                        retVal.add(new FileStatus(length, fileType.equals(FileType.DIRECTORY), replication, blockSize, mtime, atime, permission, username, groupname, path));
                    }
                }
            }
        }
        catch (SQLException e) {
            throw new IOException("Error while listing status for " + f, e);
        }
        finally {
            this.leaveBusy();
        }

        return retVal.toArray(new FileStatus[retVal.size()]);
    }
    @Override
    public void setVerifyChecksum(boolean verifyChecksum) throws AccessControlException, IOException
    {
        //NOOP
    }
    @Override
    public void close() throws IOException
    {
        if (this.closeGuard.compareAndSet(false, true)) {
            if (this.dbConnection != null) {
                try {
                    this.dbConnection.close();
                }
                catch (SQLException e) {
                    Logger.error("Error while closing the SQL connection while shutting down the HDFS SQL filesystem; " + this.getUri(), e);
                }
                finally {
                    this.dbConnection = null;
                }
            }
        }
    }
    @Override
    public byte[] getXAttr(Path path, String name) throws IOException
    {
        if (this.xAttrResolver != null) {
            return this.xAttrResolver.getXAttr(path, name);
        }
        else {
            return super.getXAttr(path, name);
        }
    }
    @Override
    public Map<String, byte[]> getXAttrs(Path path) throws IOException
    {
        if (this.xAttrResolver != null) {
            return this.xAttrResolver.getXAttrs(path);
        }
        else {
            return super.getXAttrs(path);
        }
    }
    @Override
    public Map<String, byte[]> getXAttrs(Path path, List<String> names) throws IOException
    {
        if (this.xAttrResolver != null) {
            return this.xAttrResolver.getXAttrs(path, names);
        }
        else {
            return super.getXAttrs(path, names);
        }
    }
    @Override
    public void register(XAttrResolver xAttrResolver)
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
        // No-op.
    }
    private TX getTransaction() throws IOException
    {
        return StorageFactory.getCurrentRequestTx();
    }
    private void initialize(URI name, Configuration cfg) throws IOException
    {
        enterBusy();

        boolean booted = false;
        try {
            //The public scheme name is "sql", but internally, we'll replace it by "jdbc:xxx", where "xxx" is the specific SQL dialect
            //that's implemented by this class. In the future, we might consider changing it to "sql:xxx" with the same dialect, but for now,
            //we'll only support one: SQLite

            //start off with the path only, since we'll create a SQLite file URI
            this.dbFile = Paths.get(this.getUri().getPath());
            //an existing file means we're resuming an old session
            this.resumed = Files.exists(this.dbFile);
            //Note that we're probably always passed a folder, but let's support both
            if (this.resumed) {
                if (Files.isDirectory(this.dbFile)) {
                    this.dbFile = this.dbFile.resolve(DEFAULT_DATABASE_FILENAME);
                }
            }
            else {
                //here, we need to choose between using the supplied name as a file or a dir,
                //let us use the extension to decide between both: if it ends with .db, it's a file, otherwise,
                //it's a folder where we'll create a new file
                if (!this.dbFile.endsWith("." + DEFAULT_FILENAME_EXT)) {
                    this.dbFile = this.dbFile.resolve(DEFAULT_DATABASE_FILENAME);
                }
            }

            //let's double-check
            if (Files.exists(this.dbFile) && !Files.isRegularFile(this.dbFile)) {
                throw new IOException("The SQL connection URL path should point to a regular file; " + this.dbFile);
            }

            //make sure the parent exists
            Files.createDirectories(this.dbFile.getParent());

            this.dbConnection = DriverManager.getConnection(JDBC_SCHEME + ":" + this.dbFile);

            //create the main table if it doesn't exist
            ResultSet rs = this.dbConnection.getMetaData().getTables(null, null, SQL_TABLE_NAME, null);
            if (!rs.next()) {
                try (Statement stmt = this.dbConnection.createStatement()) {
                    stmt.executeUpdate("CREATE TABLE " + SQL_TABLE_NAME + " " +
                                       "(" + SQL_COLUMN_PATH_NAME + " TEXT PRIMARY KEY NOT NULL," +
                                       " " + SQL_COLUMN_CONTENT_NAME + " " + (isBlobSupported() ? "BLOB" : "BLOB") + "," +
                                       " " + SQL_COLUMN_FILETYPE_NAME + " SMALLINT NOT NULL," +
                                       " " + SQL_COLUMN_PERMISSION_NAME + " INTEGER NOT NULL," +
                                       " " + SQL_COLUMN_USERNAME_NAME + " TEXT," +
                                       " " + SQL_COLUMN_GROUPNAME_NAME + " TEXT," +
                                       //SQLite: The value is a signed integer, stored in 1, 2, 3, 4, 6, or 8 bytes depending on the magnitude of the value,
                                       //so it's safe to use INTEGER for LONG
                                       " " + SQL_COLUMN_MTIME_NAME + " BIGINT NOT NULL," +
                                       " " + SQL_COLUMN_ATIME_NAME + " BIGINT NOT NULL," +
                                       " " + SQL_COLUMN_REPLICATION_NAME + " SMALLINT NOT NULL," +
                                       " " + SQL_COLUMN_BLOCKSIZE_NAME + " BIGINT NOT NULL," +
                                       " " + SQL_COLUMN_VERIFY_NAME + " BOOLEAN NOT NULL," +
                                       " " + SQL_COLUMN_CHECKSUM_ID_NAME + " INTEGER," +
                                       " " + SQL_COLUMN_CHECKSUM_SIZE_NAME + " INTEGER" +
                                       ");");
                }
            }

            //            RequestContext requestContext = R.requestContext();
            //            if (requestContext != null) {
            //                requestContext.registerClosable(this);
            //            }

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
    private void checkAccess(Path f) throws AccessControlException
    {
        //TODO
    }
    private boolean exists(Path f) throws SQLException
    {
        boolean retVal = false;

        try (PreparedStatement stmt = this.dbConnection.prepareStatement("SELECT COUNT(*) FROM " + SQL_TABLE_NAME +
                                                                         " WHERE " + SQL_COLUMN_PATH_NAME + "=?")) {
            stmt.setString(1, pathToSql(f));
            retVal = stmt.executeQuery().getLong(1) > 0;
        }

        return retVal;
    }
    private boolean isBlobSupported()
    {
        //JDBC driver has no support
        return false;
    }
    private String getSqlSubstrFunction(String column, String startNum)
    {
        //For SQLite it's "SUBSTR", for MySQL and SQL Server, it's "SUBSTRING"
        return "SUBSTR(" + column + "," + startNum + ")";
    }
    private String getSqlSubstrFunction(String column, String startNum, String lengthNum)
    {
        //For SQLite it's "SUBSTR", for MySQL and SQL Server, it's "SUBSTRING"
        return "SUBSTR(" + column + "," + startNum + "," + lengthNum + ")";
    }
    private String getSqlLeftFunction(String column, String charNum)
    {
        //For SQLite it's "SUBSTR", for MySQL and SQL Server, it's "LEFT"
        return "SUBSTR(" + column + ",0," + charNum + ")";
    }
    private String getSqlByteLengthFunction()
    {
        //For SQLite and MySQL, it's "LENGTH", for SQL Server, it's "DATALENGTH"
        return "LENGTH";
    }
    private String getSqlInstrFunction(String column, String search)
    {
        //For SQLite and MySQL, it's "INSTR", for SQL Server, it's "CHARINDEX"
        return "INSTR(" + column + "," + search + ")";
    }
    private String pathToSql(Path f)
    {
        return f.toUri().getPath();
    }
    private void doSetBlob(PreparedStatement stmt, int parameterIndex, Blob blob) throws SQLException, IOException
    {
        if (this.isBlobSupported()) {
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
                         short replication,
                         boolean verifyChecksum, Options.ChecksumOpt checksumOpt) throws SQLException, IOException
    {
        int retVal;

        try (PreparedStatement stmt = dbConnection.prepareStatement("INSERT INTO " + SQL_TABLE_NAME +
                                                                    " (" +
                                                                    SQL_COLUMN_PATH_NAME + "," +
                                                                    SQL_COLUMN_CONTENT_NAME + "," +
                                                                    SQL_COLUMN_FILETYPE_NAME + "," +
                                                                    SQL_COLUMN_PERMISSION_NAME + "," +
                                                                    SQL_COLUMN_USERNAME_NAME + "," +
                                                                    SQL_COLUMN_GROUPNAME_NAME + "," +
                                                                    SQL_COLUMN_MTIME_NAME + "," +
                                                                    SQL_COLUMN_ATIME_NAME + "," +
                                                                    SQL_COLUMN_REPLICATION_NAME + "," +
                                                                    SQL_COLUMN_BLOCKSIZE_NAME + "," +
                                                                    SQL_COLUMN_VERIFY_NAME + "," +
                                                                    SQL_COLUMN_CHECKSUM_ID_NAME + "," +
                                                                    SQL_COLUMN_CHECKSUM_SIZE_NAME +
                                                                    ")" +

                                                                    " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
            stmt.setString(1, pathToSql(path));
            this.doSetBlob(stmt, 2, content);
            stmt.setShort(3, fileType.getId());
            stmt.setShort(4, permission.toShort());
            stmt.setString(5, username);
            stmt.setString(6, groupname);
            stmt.setLong(7, mtime);
            stmt.setLong(8, atime);
            stmt.setShort(9, replication);
            stmt.setLong(10, blockSize);
            stmt.setBoolean(11, verifyChecksum);
            stmt.setInt(12, checksumOpt == null ? -1 : checksumOpt.getChecksumType().id);
            stmt.setInt(13, checksumOpt == null ? -1 : checksumOpt.getBytesPerChecksum());

            retVal = stmt.executeUpdate();
        }

        return retVal;
    }
    private int doAppend(Connection dbConnection, Path path, Blob content) throws SQLException, IOException
    {
        int retVal;

        try (PreparedStatement stmt = dbConnection.prepareStatement("UPDATE " + SQL_TABLE_NAME +
                                                                    " SET " +
                                                                    SQL_COLUMN_CONTENT_NAME + " = " + SQL_COLUMN_CONTENT_NAME + " || ?" +
                                                                    " WHERE " + SQL_COLUMN_PATH_NAME + " = ?")) {
            this.doSetBlob(stmt, 1, content);
            stmt.setString(2, pathToSql(path));

            retVal = stmt.executeUpdate();
        }

        return retVal;
    }
    private int doDelete(Connection dbConnection, Path path, boolean recursive) throws SQLException
    {
        int retVal;

        String pathName = pathToSql(path);
        String pathNameRec = pathName + Path.SEPARATOR;
        String sql = "DELETE FROM " + SQL_TABLE_NAME +
                     " WHERE " + SQL_COLUMN_PATH_NAME + "=?";
        if (recursive) {
            //Note: 'LEFT' is much faster than 'LIKE'
            sql += " OR " + this.getSqlLeftFunction(SQL_COLUMN_PATH_NAME, "?") + " = ?";
        }

        try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
            stmt.setString(1, pathName);
            if (recursive) {
                stmt.setInt(2, pathNameRec.length() + 1);
                stmt.setString(3, pathNameRec);
            }
            retVal = stmt.executeUpdate();
        }

        return retVal;
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
                if (isBlobSupported()) {
                    //Note: we don't use the streaming blob API (this.blob.setBinaryStream()) to make the implementation below a bit easier,
                    // in regard to the 'fake' Blob implementation below, but if performance would be bad, we should look into this...
                    this.blob = dbConnection.createBlob();
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
            this.blobStream.close();

            //process the results
            try {
                if (this.append) {
                    doAppend(this.dbConnection, this.path, this.blob);
                }
                else {
                    if (this.exists && this.overwrite) {
                        doDelete(this.dbConnection, this.path, false);
                    }
                    doInsert(this.dbConnection, this.path, this.blob, SqlFS.FileType.FILE, this.permission, this.blockSize, this.checksumOpt);
                }
            }
            catch (SQLException e) {
                throw new IOException("Error while writing data for " + this.path, e);
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
                try (PreparedStatement stmt = this.dbConnection.prepareStatement("SELECT " + SQL_COLUMN_CONTENT_NAME + " FROM " + SQL_TABLE_NAME +
                                                                                 " WHERE " + SQL_COLUMN_PATH_NAME + "=?")) {

                    stmt.setString(1, pathToSql(f));

                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        if (isBlobSupported()) {
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
            this.blobInputStream.close();
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
