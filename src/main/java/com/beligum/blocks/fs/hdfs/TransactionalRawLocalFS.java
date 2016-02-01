package com.beligum.blocks.fs.hdfs;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.fs.local.LocalConfigKeys;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by bram on 2/2/16.
 */
public class TransactionalRawLocalFS extends DelegateToFileSystem
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    /**
     * This constructor has the signature needed by
     * {@link AbstractFileSystem#createFileSystem(URI, Configuration)}.
     *
     * @param uri  which must be that of localFs
     * @param conf
     * @throws IOException
     * @throws URISyntaxException
     */
    protected TransactionalRawLocalFS(final URI uri, final Configuration conf) throws IOException, URISyntaxException
    {
        super(uri, new TransactionalRawLocalFileSystem(), conf, TransactionalRawLocalFileSystem.SCHEME, false);
    }

    //-----PUBLIC METHODS-----
    @Override
    public int getUriDefaultPort()
    {
        return -1; // No default port for file:///
    }

    @Override
    public FsServerDefaults getServerDefaults() throws IOException
    {
        return LocalConfigKeys.getServerDefaults();
    }

    @Override
    public boolean isValidName(String src)
    {
        // Different local file systems have different validation rules. Skip
        // validation here and just let the OS handle it. This is consistent with
        // RawLocalFileSystem.
        return true;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
