package com.beligum.blocks.fs;

import com.beligum.blocks.fs.ifaces.ResourcePath;
import org.apache.hadoop.fs.Path;

import java.io.IOException;

/**
 * Created by bram on 1/19/16.
 */
public class LockFile implements AutoCloseable
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private ResourcePath resourcePath;
    private Path lockFile;

    //-----CONSTRUCTORS-----
    public LockFile(ResourcePath resourcePath, Path lockFile)
    {
        this.resourcePath = resourcePath;
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
        this.resourcePath.releaseLockFile(this);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
