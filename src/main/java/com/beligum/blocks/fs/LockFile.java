package com.beligum.blocks.fs;

import com.beligum.blocks.fs.ifaces.PathInfo;

import java.io.IOException;

/**
 * Created by bram on 1/19/16.
 */
public class LockFile<T> implements AutoCloseable
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private PathInfo<T> pathInfo;
    private T lockFile;

    //-----CONSTRUCTORS-----
    public LockFile(PathInfo<T> pathInfo, T lockFile)
    {
        this.pathInfo = pathInfo;
        this.lockFile = lockFile;
    }

    //-----PUBLIC METHODS-----
    public T getLockFile()
    {
        return lockFile;
    }
    @Override
    public void close() throws IOException
    {
        this.pathInfo.releaseLockFile(this);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
