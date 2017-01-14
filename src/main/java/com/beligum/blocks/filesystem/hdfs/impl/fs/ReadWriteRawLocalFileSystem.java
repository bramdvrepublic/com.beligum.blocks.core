package com.beligum.blocks.filesystem.hdfs.impl.fs;

import org.apache.hadoop.fs.FsConstants;
import org.apache.hadoop.fs.RawLocalFileSystem;

/**
 * Created by bram on 1/14/17.
 */
public class ReadWriteRawLocalFileSystem extends RawLocalFileSystem
{
    //-----CONSTANTS-----
    public static final String SCHEME = FsConstants.LOCAL_FS_URI.getScheme();

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public ReadWriteRawLocalFileSystem()
    {
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getScheme()
    {
        return SCHEME;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
