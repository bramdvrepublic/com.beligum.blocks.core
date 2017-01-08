package com.beligum.blocks.fs.hdfs;

import org.apache.hadoop.fs.*;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.util.Progressable;

import java.io.IOException;
import java.net.URI;
import java.util.EnumSet;

/**
 * Read-only version of the Hadoop RawLocalFileSystem that overloads all write functions to throw exceptions
 * just to make sure we don't use it to edit files.
 * <p>
 * Created by bram on 2/1/16.
 */
public class ReadOnlyRawLocalFileSystem extends RawLocalFileSystem
{
    //-----CONSTANTS-----
    public static final URI NAME = FsConstants.LOCAL_FS_URI;
    public static final String SCHEME = NAME.getScheme();

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public ReadOnlyRawLocalFileSystem()
    {
        super();
    }

    //-----PUBLIC METHODS-----
    @Override
    public FSDataOutputStream append(Path f, int bufferSize, Progressable progress) throws IOException
    {
        return this.throwReadOnly("append");
    }
    @Override
    public FSDataOutputStream create(Path f, boolean overwrite, int bufferSize, short replication, long blockSize, Progressable progress) throws IOException
    {
        return this.throwReadOnly("create");
    }
    @Override
    @Deprecated
    public FSDataOutputStream createNonRecursive(Path f, FsPermission permission, EnumSet<CreateFlag> flags, int bufferSize, short replication, long blockSize, Progressable progress) throws IOException
    {
        return this.throwReadOnly("createNonRecursive");
    }
    @Override
    public FSDataOutputStream create(Path f, FsPermission permission, boolean overwrite, int bufferSize, short replication, long blockSize, Progressable progress) throws IOException
    {
        return this.throwReadOnly("create");
    }
    @Override
    public FSDataOutputStream createNonRecursive(Path f, FsPermission permission, boolean overwrite, int bufferSize, short replication, long blockSize, Progressable progress) throws IOException
    {
        return this.throwReadOnly("createNonRecursive");
    }
    @Override
    public boolean rename(Path src, Path dst) throws IOException
    {
        return this.throwReadOnly("rename");
    }
    @Override
    public boolean truncate(Path f, final long newLength) throws IOException
    {
        return this.throwReadOnly("truncate");
    }
    @Override
    public boolean delete(Path p, boolean recursive) throws IOException
    {
        return this.throwReadOnly("delete");
    }
    @Override
    public boolean mkdirs(Path f) throws IOException
    {
        return this.throwReadOnly("mkdirs");
    }
    @Override
    public boolean mkdirs(Path f, FsPermission permission) throws IOException
    {
        return this.throwReadOnly("mkdirs");
    }
    @Override
    public void moveFromLocalFile(Path src, Path dst) throws IOException
    {
        this.throwReadOnly("moveFromLocalFile");
    }
    @Override
    public Path startLocalOutput(Path fsOutputFile, Path tmpLocalFile) throws IOException
    {
        return this.throwReadOnly("startLocalOutput");
    }
    @Override
    public void completeLocalOutput(Path fsWorkingFile, Path tmpLocalFile) throws IOException
    {
        this.throwReadOnly("completeLocalOutput");
    }
    @Override
    public void setOwner(Path p, String username, String groupname) throws IOException
    {
        this.throwReadOnly("setOwner");
    }
    @Override
    public void setPermission(Path p, FsPermission permission) throws IOException
    {
        this.throwReadOnly("setPermission");
    }
    @Override
    public void setTimes(Path p, long mtime, long atime) throws IOException
    {
        this.throwReadOnly("setTimes");
    }
    @Override
    public void createSymlink(Path target, Path link, boolean createParent) throws IOException
    {
        this.throwReadOnly("createSymlink");
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private <T> T throwReadOnly(String methodName) throws IOException
    {
        throw new IOException("Unsupported method " + methodName + "() because this is a read-only file system");
    }
}
