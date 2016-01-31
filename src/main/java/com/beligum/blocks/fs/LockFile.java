package com.beligum.blocks.fs;

import com.beligum.blocks.fs.ifaces.PathInfo;
import org.apache.hadoop.fs.Path;

import java.io.IOException;

/**
 * Created by bram on 1/19/16.
 */
public class LockFile implements AutoCloseable
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private PathInfo pathInfo;
    private Path lockFile;

    //-----CONSTRUCTORS-----
    public LockFile(PathInfo pathInfo, Path lockFile)
    {
        this.pathInfo = pathInfo;
        this.lockFile = lockFile;
    }

    //-----PUBLIC METHODS-----
    public Path getLockFile()
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
