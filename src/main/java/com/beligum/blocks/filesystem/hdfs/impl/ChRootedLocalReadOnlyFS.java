package com.beligum.blocks.filesystem.hdfs.impl;

import com.beligum.blocks.filesystem.hdfs.impl.orig.v2_7_1.ChRootedFs;
import org.apache.hadoop.conf.Configuration;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by bram on 1/14/17.
 */
public class ChRootedLocalReadOnlyFS extends AbstractChRootedFS
{
    //-----CONSTANTS-----
    public static final String SCHEME = LocalReadOnlyFS.SCHEME;

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    protected ChRootedLocalReadOnlyFS(URI uri, Configuration conf) throws IOException, URISyntaxException
    {
        super(uri, conf, new LocalReadOnlyFS(uri, conf));
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
