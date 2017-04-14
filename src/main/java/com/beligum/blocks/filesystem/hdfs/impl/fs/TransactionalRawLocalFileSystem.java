package com.beligum.blocks.filesystem.hdfs.impl.fs;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.filesystem.hdfs.XADiskUtil;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.server.namenode.UnsupportedActionException;
import org.apache.hadoop.util.Progressable;
import org.xadisk.bridge.proxies.interfaces.XAFileInputStream;
import org.xadisk.bridge.proxies.interfaces.XAFileOutputStream;
import org.xadisk.bridge.proxies.interfaces.XASession;
import org.xadisk.filesystem.exceptions.InsufficientPermissionOnFileException;
import org.xadisk.filesystem.exceptions.LockingFailedException;
import org.xadisk.filesystem.exceptions.NoTransactionAssociatedException;

import java.io.*;
import java.net.URI;
import java.util.Arrays;

/**
 * This is more or less the same implementation as RawLocalFileSystem, but all file operations are made transactional using the XADisk library.
 * To make sure we override all needed methods, this is how I implemented it:
 * - make this class inherit the abstract org.apache.hadoop.fs.FileSystem and auto-generate all abstract method
 * - switch to RawLocalFileSystem, keeping the overloaded methods and check them one by one
 * - copy in the inner classes LocalFSFileOutputStream and LocalFSFileInputStream from RawLocalFileSystem (and change all FSError classes to Error)
 * -
 * <p/>
 * Created by bram on 2/1/16.
 */
public class TransactionalRawLocalFileSystem extends org.apache.hadoop.fs.FileSystem /*RawLocalFileSystem*/
{
    //-----CONSTANTS-----
    //analogous to RawLocalFileSystem
    public static final URI NAME = URI.create("fileXa:///");
    public static final String SCHEME = NAME.getScheme();

    // Temporary workaround for HADOOP-9652.
    // Note: changed to false because it pulled in too much clutter dependencies and the consequences are acceptable to us;
    // see https://issues.apache.org/jira/browse/HADOOP-9652
    // "RawLocalFs#getFileLinkStatus does not actually get the owner and mode of the symlink, but instead uses the owner and mode of the symlink target.
    //  If the target can't be found, it fills in bogus values (the empty string and FsPermission.getDefault) for these."
    private static boolean useDeprecatedFileStatus = false;

    //-----VARIABLES-----
    private Path workingDir;

    //-----CONSTRUCTORS-----
    public TransactionalRawLocalFileSystem()
    {
        this.workingDir = getInitialWorkingDirectory();
    }

    //-----PUBLIC METHODS-----
    @Override
    public void initialize(URI name, Configuration conf) throws IOException
    {
        super.initialize(name, conf);
        this.setConf(conf);
    }
    @Override
    public String getScheme()
    {
        return SCHEME;
    }

    //-----OBLIGATORY IMPLEMENTED METHODS-----
    @Override
    public URI getUri()
    {
        return NAME;
    }
    @Override
    public FSDataInputStream open(Path p, int bufferSize) throws IOException
    {
        XASession tx = this.getTransaction();
        File f = pathToFile(p);

        try {
            if (!txExists(tx, f)) {
                throw new FileNotFoundException(p.toString());
            }
        }
        catch (Exception e) {
            throw new IOException(e);
        }

        return new FSDataInputStream(new BufferedFSInputStream(new LocalFSFileInputStream(this.getTransaction(), p), bufferSize));
    }
    @Override
    public FSDataOutputStream create(Path f, FsPermission permission, boolean overwrite, int bufferSize, short replication, long blockSize, Progressable progress) throws IOException
    {
        return create(f, overwrite, true, bufferSize, replication, blockSize, progress, permission);
    }
    @Override
    public FSDataOutputStream append(Path p, int bufferSize, Progressable progress) throws IOException
    {
        XASession tx = this.getTransaction();
        File f = pathToFile(p);

        try {
            if (!txExists(tx, f)) {
                throw new FileNotFoundException("File " + p + " not found");
            }
        }
        catch (Exception e) {
            throw new IOException(e);
        }

        if (getFileStatus(p).isDirectory()) {
            throw new IOException("Cannot append to a directory (=" + p + " )");
        }

        return new FSDataOutputStream(new BufferedOutputStream(createOutputStreamWithMode(p, true, null), bufferSize), statistics);
    }
    @Override
    public boolean rename(Path src, Path dst) throws IOException
    {
        boolean retVal = false;

        XASession tx = this.getTransaction();

        // Attempt rename using Java API.
        File srcFile = pathToFile(src);
        File dstFile = pathToFile(dst);
        try {
            tx.moveFile(srcFile, dstFile);
            retVal = true;
        }
        catch (Exception e) {
            Logger.error("Exception caught while renaming " + src + " to " + dst, e);
        }

        return retVal;

        //        // Enforce POSIX rename behavior that a source directory replaces an existing
        //        // destination if the destination is an empty directory.  On most platforms,
        //        // this is already handled by the Java API call above.  Some platforms
        //        // (notably Windows) do not provide this behavior, so the Java API call above
        //        // fails.  Delete destination and attempt rename again.
        //        if (this.exists(dst)) {
        //            FileStatus sdst = this.getFileStatus(dst);
        //            if (sdst.isDirectory() && dstFile.list().length == 0) {
        //                if (LOG.isDebugEnabled()) {
        //                    LOG.debug("Deleting empty destination and renaming " + src + " to " + dst);
        //                }
        //                if (this.delete(dst, false) && srcFile.renameTo(dstFile)) {
        //                    return true;
        //                }
        //            }
        //        }
        //
        //        // The fallback behavior accomplishes the rename by a full copy.
        //        if (LOG.isDebugEnabled()) {
        //            LOG.debug("Falling through to a copy of " + src + " to " + dst);
        //        }
        //        return FileUtil.copy(this, src, this, dst, true, getConf());
    }
    @Override
    public boolean delete(Path p, boolean recursive) throws IOException
    {
        boolean retVal = false;

        XASession tx = this.getTransaction();
        File f = pathToFile(p);

        try {
            if (this.txExists(tx, f)) {
                if (!this.txExistsAndIsDirectory(tx, f)) {
                    tx.deleteFile(f);
                    retVal = true;
                }
                else {
                    if (!recursive && tx.listFiles(f).length != 0) {
                        throw new IOException("Directory " + f.toString() + " is not empty");
                    }
                    else {
                        retVal = XADiskUtil.fullyDelete(tx, f);
                    }
                }
            }
        }
        catch (Exception e) {
            throw new IOException(e);
        }

        return retVal;
    }
    @Override
    public FileStatus[] listStatus(Path p) throws FileNotFoundException, IOException
    {
        XASession tx = this.getTransaction();
        File f = pathToFile(p);
        FileStatus[] results;

        try {
            if (!this.txExists(tx, f)) {
                throw new FileNotFoundException("File " + p + " does not exist");
            }
        }
        catch (Exception e) {
            throw new IOException(e);
        }

        try {
            if (!this.txExistsAndIsDirectory(tx, f)) {
                if (!useDeprecatedFileStatus) {
                    return new FileStatus[] { getFileStatus(p) };
                }
                else {
                    throw new UnsupportedActionException(
                                    "Only non-deprecated file status implemented and by consequence, there are currently no OSs supported that don't support the UNIX stat(1) command (like Windows); " +
                                    f);
                    //return new FileStatus[] { new DeprecatedRawLocalFileStatus(f, getDefaultBlockSize(p), this) };
                }
            }
            else {
                String[] names = null;
                try {
                    names = tx.listFiles(f);
                    if (names == null) {
                        return null;
                    }
                }
                catch (Exception e) {
                    throw new IOException("Exception caught while listing the children of directory; " + f, e);
                }

                results = new FileStatus[names.length];
                int j = 0;
                for (int i = 0; i < names.length; i++) {
                    try {
                        // Assemble the path using the Path 3 arg constructor to make sure
                        // paths with colon are properly resolved on Linux
                        results[j] = getFileStatus(new Path(p, new Path(null, null, names[i])));
                        j++;
                    }
                    catch (FileNotFoundException e) {
                        // ignore the files not found since the dir list may have have changed
                        // since the names[] list was generated.
                    }
                }
                if (j == names.length) {
                    return results;
                }
                return Arrays.copyOf(results, j);
            }
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }
    @Override
    public void setWorkingDirectory(Path newDir)
    {
        workingDir = makeAbsolute(newDir);
        checkPath(workingDir);
    }
    @Override
    public Path getWorkingDirectory()
    {
        return workingDir;
    }
    @Override
    public boolean mkdirs(Path p, FsPermission permission) throws IOException
    {
        if (p == null) {
            throw new IllegalArgumentException("mkdirs path arg is null");
        }
        XASession tx = this.getTransaction();
        Path parent = p.getParent();
        File f = pathToFile(p);
        File parent2f = null;
        boolean parent2fExists = false;
        if (parent != null) {
            parent2f = pathToFile(parent);
            try {
                parent2fExists = parent2f != null && this.txExists(tx, parent2f);
                if (parent2fExists && !txExistsAndIsDirectory(tx, parent2f)) {
                    throw new ParentNotDirectoryException("Parent path is not a directory: " + parent);
                }
            }
            catch (Exception e) {
                throw new IOException(e);
            }
        }
        try {
            if (this.txExists(tx, f) && !txExistsAndIsDirectory(tx, f)) {
                throw new FileNotFoundException("Destination exists" +
                                                " and is not a directory: " + f.getCanonicalPath());
            }

            return (parent == null || parent2fExists || mkdirs(parent)) &&
                   (mkOneDirWithMode(tx, f, permission) || txExistsAndIsDirectory(tx, f));
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }
    @Override
    public FileStatus getFileStatus(Path f) throws IOException
    {
        return getFileLinkStatusInternal(this.getTransaction(), f, true);
    }

    //-----PROTECTED METHODS-----
    @Override
    protected Path getInitialWorkingDirectory()
    {
        return this.makeQualified(new Path(System.getProperty("user.dir")));
    }

    //-----PRIVATE METHODS-----
    /**
     * Convert a path to a File.
     */
    public File pathToFile(Path path)
    {
        checkPath(path);
        if (!path.isAbsolute()) {
            path = new Path(getWorkingDirectory(), path);
        }
        return new File(path.toUri().getPath());
    }
    private Path makeAbsolute(Path f)
    {
        if (f.isAbsolute()) {
            return f;
        }
        else {
            return new Path(workingDir, f);
        }
    }
    /**
     * Note: overwrite means: if a file with this name already exists, then if true, the file will be overwritten, and if false an setRollbackOnly will be thrown.
     */
    private FSDataOutputStream create(Path p, boolean overwrite, boolean createParent, int bufferSize, short replication, long blockSize, Progressable progress, FsPermission permission)
                    throws IOException
    {
        XASession tx = this.getTransaction();
        File f = pathToFile(p);

        try {
            if (!overwrite && txExists(tx, f)) {
                throw new FileAlreadyExistsException("File already exists: " + p);
            }
        }
        catch (Exception e) {
            throw new IOException(e);
        }

        if (createParent) {
            Path parent = p.getParent();
            if (parent != null && !mkdirs(parent)) {
                throw new IOException("Mkdirs failed to create " + parent.toString());
            }
        }

        return new FSDataOutputStream(new BufferedOutputStream(createOutputStreamWithMode(p, false, permission), bufferSize), statistics);
    }
    protected OutputStream createOutputStreamWithMode(Path f, boolean append, FsPermission permission) throws IOException
    {
        return new LocalFSFileOutputStream(this.getTransaction(), f, append, permission);
    }
    protected boolean mkOneDirWithMode(XASession tx, File f, FsPermission permission) throws IOException
    {
        boolean retVal = false;

        // little work around to never throw the exception below (because it's not supported by XADisk
        // see eg. https://groups.google.com/forum/#!topic/xadisk/0zbUGCIzNpI
        if (permission != null) {
            permission = null;
        }

        if (permission == null) {
            try {
                //XADisk crashes if the file already exists
                if (!this.txExists(tx, f)) {
                    tx.createFile(f, true);
                }
                retVal = true;
            }
            catch (Exception e) {
                throw new IOException("Caught exception while creating directory; " + f, e);
            }
        }
        else {
            throw new UnsupportedActionException("Creating directory with specific permissions is not supported by XADisk; " + f);

            //            if (Shell.WINDOWS && NativeIO.isAvailable()) {
            //                try {
            //                    NativeIO.Windows.createDirectoryWithMode(p2f, permission.toShort());
            //                    return true;
            //                }
            //                catch (IOException e) {
            //                    if (LOG.isDebugEnabled()) {
            //                        LOG.debug(String.format(
            //                                        "NativeIO.createDirectoryWithMode setRollbackOnly, path = %s, mode = %o",
            //                                        p2f, permission.toShort()), e);
            //                    }
            //                    return false;
            //                }
            //            }
            //            else {
            //                boolean b = p2f.mkdir();
            //                if (b) {
            //                    setPermission(p, permission);
            //                }
            //                return b;
            //            }
        }

        return retVal;
    }
    /**
     * Public {@link FileStatus} methods delegate to this function, which in turn
     * either call the new {@link Stat} based implementation or the deprecated
     * methods based on platform support.
     *
     * @param p           Path to stat
     * @param dereference whether to dereference the final path component if a
     *                    symlink
     * @return FileStatus of f
     * @throws IOException
     */
    private FileStatus getFileLinkStatusInternal(XASession tx, final Path p, boolean dereference) throws IOException
    {
        if (!useDeprecatedFileStatus) {
            // Not really sure about this code and it not using the TX anywhere...
            // uses the native the Unix stat(1) command, so for one, it won't work on Windows...
            checkPath(p);

            //we can't use this original code (from rawLocalFs) because XADisk doesn't support permissions, owner or group
            //            Stat stat = new Stat(f, getDefaultBlockSize(f), dereference, this);
            //            FileStatus status = stat.getFileStatus();
            //            return status;

            File f = pathToFile(p);

            try {
                if (!this.txExists(tx, f)) {
                    throw new FileNotFoundException("Can't get file status from an unexisting file/folder; "+f);
                }

                //taken from DeprecatedRawLocalFileStatus
                Path path = new Path(f.getPath()).makeQualified(this.getUri(), this.getWorkingDirectory());
                boolean isDir = this.txExistsAndIsDirectory(tx, f);
                // XADisk crashes on folder lengths (always checks file permission, even if directory)
                // but that's ok, since HDFS reports zero length for folders,
                // see https://hadoop.apache.org/docs/r1.0.4/webhdfs.html#GETFILESTATUS
                long length = isDir ? 0 : tx.getFileLength(f);
                int blockReplication = 1;
                long blockSize = this.getDefaultBlockSize(p);
                // same setRollbackOnly for dirs as folders, don't really know what to do
                long modificationTime = isDir ? 0 : tx.getFileLastModified(f);
                long accessTime = 0;
                FsPermission permission = null;
                String owner = "";
                String group = "";

                return new FileStatus(length, isDir, blockReplication, blockSize, modificationTime, accessTime, permission, owner, group, path);
            }
            //special case; eg. see FileContext.exists()
            catch (FileNotFoundException e) {
                throw e;
            }
            catch (Exception e) {
                throw new IOException(e);
            }
        }
        else {
            throw new UnsupportedActionException(
                            "Only non-deprecated file status implemented and by consequence, there are currently no OSs supported that don't support the UNIX stat(1) command (like Windows); " + p);
            //            if (dereference) {
            //                return deprecatedGetFileStatus(f);
            //            }
            //            else {
            //                return deprecatedGetFileLinkStatusInternal(f);
            //            }
        }
    }
    private XASession getTransaction() throws IOException
    {
        return StorageFactory.getCurrentXDiskTx();
    }
    /**
     * Special wrapper around XDisk Session.fileExists() method to solve issue XADISK-120
     * See https://java.net/jira/browse/XADISK-120
     * and also https://java.net/jira/browse/XADISK-157
     */
    private boolean txExists(XASession tx, File f) throws InterruptedException, LockingFailedException, NoTransactionAssociatedException
    {
        boolean retVal = false;

        try {
            retVal = tx.fileExists(f);
        }
        catch (InsufficientPermissionOnFileException e) {
            retVal = false;
        }

        return retVal;
    }
    /**
     * Same as this#txExists be for the fileExistsAndIsDirectory() method
     */
    private boolean txExistsAndIsDirectory(XASession tx, File f) throws InterruptedException, LockingFailedException, NoTransactionAssociatedException
    {
        boolean retVal = false;

        try {
            retVal = tx.fileExistsAndIsDirectory(f);
        }
        catch (InsufficientPermissionOnFileException e) {
            retVal = false;
        }

        return retVal;
    }

    //-----INNER CLASSES-----

    /*******************************************************
     * For open()'s FSInputStream.
     *******************************************************/
    class LocalFSFileInputStream extends FSInputStream /*implements HasFileDescriptor*/
    {
        private XAFileInputStream fis;

        public LocalFSFileInputStream(XASession tx, Path f) throws IOException
        {
            try {
                this.fis = tx.createXAFileInputStream(pathToFile(f));
            }
            catch (Exception e) {
                throw new IOException("Exception caught while opening transactional file input stream; " + f, e);
            }
        }

        @Override
        public void seek(long pos) throws IOException
        {
            if (pos < 0) {
                throw new EOFException(FSExceptionMessages.NEGATIVE_SEEK);
            }
            try {
                fis.position(pos);
            }
            catch (Exception e) {
                throw new IOException(e);
            }
        }

        @Override
        public long getPos() throws IOException
        {
            return this.fis.position();
        }

        @Override
        public boolean seekToNewSource(long targetPos) throws IOException
        {
            return false;
        }

        /*
         * Just forward to the fis
         */
        @Override
        public int available() throws IOException
        {
            try {
                return fis.available();
            }
            catch (Exception e) {
                throw new IOException(e);
            }
        }
        @Override
        public void close() throws IOException
        {
            try {
                fis.close();
            }
            catch (Exception e) {
                throw new IOException(e);
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
            try {
                int value = 0;
                try {
                    value = fis.read();
                }
                catch (Exception e) {
                    throw new IOException(e);
                }
                if (value >= 0) {
                    statistics.incrementBytesRead(1);
                }
                return value;
            }
            catch (IOException e) {                 // unexpected exception
                throw new Error(e);                   // assume native fs setRollbackOnly
            }
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException
        {
            try {
                int value = 0;
                try {
                    value = fis.read(b, off, len);
                }
                catch (Exception e) {
                    throw new IOException(e);
                }
                if (value >= 0) {
                    statistics.incrementBytesRead(value);
                }
                return value;
            }
            catch (IOException e) {                 // unexpected exception
                throw new Error(e);                   // assume native fs setRollbackOnly
            }
        }

        @Override
        public long skip(long n) throws IOException
        {
            long value = 0;
            try {
                value = fis.skip(n);
            }
            catch (Exception e) {
                throw new IOException(e);
            }

            return value;
        }

        //        @Override
        //        public FileDescriptor getFileDescriptor() throws IOException
        //        {
        //            return fis.getFD();
        //        }
    }

    /*********************************************************
     * For create()'s FSOutputStream.
     *********************************************************/
    class LocalFSFileOutputStream extends OutputStream
    {
        private XAFileOutputStream fos;

        private LocalFSFileOutputStream(XASession tx, Path f, boolean append, FsPermission permission) throws IOException
        {
            File file = pathToFile(f);

            // little work around to never throw the exception below (because it's not supported by XADisk
            // see eg. https://groups.google.com/forum/#!topic/xadisk/0zbUGCIzNpI
            if (permission != null) {
                permission = null;
            }

            if (permission == null) {
                try {
                    //Note: that createXAFileOutputStream() always appends to the file
                    if (!append && txExists(tx, file)) {
                        //Note that we encountered a bug under linux64 here, see https://java.net/jira/browse/XADISK-158
                        //Apparently, setting the position (eg. zero here) and then appending to a file, doesn't append to the position,
                        // but simply appends to the end of the file. So avoid using the truncate function altogether...
                        //tx.truncateFile(file, 0l);
                        tx.deleteFile(file);
                        //Note: file is created in the doIsValid below
                    }

                    //method below needs the file to exist before we can write to it
                    if (!txExists(tx, file)) {
                        //we assume we won't be writing to a directory
                        tx.createFile(file, false);
                    }

                    //Note: with the last flag set to true, I encountered buffer underrun exceptions!
                    this.fos = tx.createXAFileOutputStream(file, true);
                }
                catch (Exception e) {
                    throw new IOException("Exception caught while opening transactional file output stream; " + f, e);
                }
            }
            else {
                throw new UnsupportedActionException("Creation with specific permissions is not supported by XADisk; " + f);

                //                if (Shell.WINDOWS && NativeIO.isAvailable()) {
                //                    this.fos = NativeIO.Windows.createFileOutputStreamWithMode(file, append, permission.toShort());
                //                }
                //                else {
                //                    this.fos = new FileOutputStream(file, append);
                //                    boolean success = false;
                //                    try {
                //                        setPermission(f, permission);
                //                        success = true;
                //                    }
                //                    finally {
                //                        if (!success) {
                //                            IOUtils.cleanup(LOG, this.fos);
                //                        }
                //                    }
                //                }
            }
        }

        /*
         * Just forward to the fos
         */
        @Override
        public void close() throws IOException
        {
            try {
                fos.close();
            }
            catch (Exception e) {
                throw new IOException(e);
            }
        }
        @Override
        public void flush() throws IOException
        {
            try {
                fos.flush();
            }
            catch (Exception e) {
                throw new IOException(e);
            }
        }
        @Override
        public void write(byte[] b, int off, int len) throws IOException
        {
            try {
                try {
                    fos.write(b, off, len);
                }
                catch (Exception e) {
                    throw new IOException(e);
                }
            }
            catch (IOException e) {                // unexpected exception
                throw new Error(e);                  // assume native fs setRollbackOnly
            }
        }

        @Override
        public void write(int b) throws IOException
        {
            try {
                try {
                    fos.write(b);
                }
                catch (Exception e) {
                    throw new IOException(e);
                }
            }
            catch (IOException e) {              // unexpected exception
                throw new Error(e);                // assume native fs setRollbackOnly
            }
        }
    }
}
