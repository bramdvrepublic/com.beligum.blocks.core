package com.beligum.blocks.filesystem.hdfs.impl;

import org.apache.hadoop.conf.Configuration;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by bram on 1/14/17.
 */
public class ChRootedLocalReadWriteFS extends AbstractChRootedFS
{
    //-----CONSTANTS-----
    public static final String SCHEME = LocalReadWriteFS.SCHEME;

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    protected ChRootedLocalReadWriteFS(URI uri, Configuration conf) throws IOException, URISyntaxException
    {
        super(uri, conf, new LocalReadWriteFS(uri, conf));
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
