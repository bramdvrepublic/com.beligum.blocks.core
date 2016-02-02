package com.beligum.blocks.fs.hdfs;

import org.xadisk.bridge.proxies.interfaces.Session;

/**
 * Created by bram on 2/1/16.
 */
public class XADiskRequestCacheEntry
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    public TransactionalRawLocalFileSystem hdfsFileSystem;
    public Session xaSession;

    //-----CONSTRUCTORS-----
    public XADiskRequestCacheEntry(TransactionalRawLocalFileSystem hdfsFileSystem, Session xaSession)
    {
        this.hdfsFileSystem = hdfsFileSystem;
        this.xaSession = xaSession;
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
