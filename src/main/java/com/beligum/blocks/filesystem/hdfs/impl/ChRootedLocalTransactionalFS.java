package com.beligum.blocks.filesystem.hdfs.impl;

import com.beligum.blocks.filesystem.hdfs.impl.orig.v2_7_1.ChRootedFs;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by bram on 1/14/17.
 */
public class ChRootedLocalTransactionalFS extends ChRootedFs
{
    //-----CONSTANTS-----
    public static final String SCHEME = LocalTransactionalFS.SCHEME;

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    protected ChRootedLocalTransactionalFS(URI uri, Configuration conf) throws IOException, URISyntaxException
    {
        super(new LocalTransactionalFS(uri, conf), new Path(uri.getPath()));
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
