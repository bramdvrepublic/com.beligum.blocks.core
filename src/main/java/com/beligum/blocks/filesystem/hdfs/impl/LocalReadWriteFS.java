package com.beligum.blocks.filesystem.hdfs.impl;

import com.beligum.blocks.filesystem.hdfs.impl.fs.ReadWriteRawLocalFileSystem;
import org.apache.hadoop.conf.Configuration;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by bram on 1/14/17.
 */
public class LocalReadWriteFS extends AbstractLocalFS
{
    //-----CONSTANTS-----
    public static final String SCHEME = ReadWriteRawLocalFileSystem.SCHEME;

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    protected LocalReadWriteFS(URI uri, Configuration conf) throws IOException, URISyntaxException
    {
        super(uri, conf, new ReadWriteRawLocalFileSystem());
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
