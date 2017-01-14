package com.beligum.blocks.filesystem.hdfs.impl;

import com.beligum.blocks.filesystem.hdfs.impl.fs.ReadOnlyRawLocalFileSystem;
import org.apache.hadoop.conf.Configuration;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by bram on 1/14/17.
 */
public class LocalReadOnlyFS extends AbstractLocalFS
{
    //-----CONSTANTS-----
    public static final String SCHEME = ReadOnlyRawLocalFileSystem.SCHEME;

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    protected LocalReadOnlyFS(URI uri, Configuration conf) throws IOException, URISyntaxException
    {
        super(uri, conf, new ReadOnlyRawLocalFileSystem());
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
