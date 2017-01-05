package com.beligum.blocks.fs;

import com.beligum.blocks.fs.ifaces.BlocksResource;
import org.apache.hadoop.fs.Path;

import java.io.IOException;

/**
 * Created by bram on 1/19/16.
 */
public class LockFile implements AutoCloseable
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private BlocksResource blocksResource;
    private Path lockFile;

    //-----CONSTRUCTORS-----
    public LockFile(BlocksResource blocksResource, Path lockFile)
    {
        this.blocksResource = blocksResource;
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
        this.blocksResource.releaseLockFile(this);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
