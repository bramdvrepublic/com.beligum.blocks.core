package com.beligum.blocks.filesystem.hdfs.impl;

import com.beligum.blocks.filesystem.hdfs.impl.fs.TransactionalRawLocalFileSystem;
import org.apache.hadoop.conf.Configuration;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by bram on 1/14/17.
 */
public class LocalTransactionalFS extends AbstractLocalFS
{
    //-----CONSTANTS-----
    public static final String SCHEME = TransactionalRawLocalFileSystem.SCHEME;

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    protected LocalTransactionalFS(URI uri, Configuration conf) throws IOException, URISyntaxException
    {
        super(uri, conf, new TransactionalRawLocalFileSystem());
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
