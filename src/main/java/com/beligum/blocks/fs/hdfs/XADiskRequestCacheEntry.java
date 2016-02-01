package com.beligum.blocks.fs.hdfs;

import org.xadisk.bridge.proxies.interfaces.Session;
import org.xadisk.bridge.proxies.interfaces.XAFileSystem;

/**
 * Created by bram on 2/1/16.
 */
public class XADiskRequestCacheEntry
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    public TransactionalRawLocalFileSystem hdfsFileSystem;
    public XAFileSystem xaFileSystem;
    public Session xaSession;

    //-----CONSTRUCTORS-----
    public XADiskRequestCacheEntry(TransactionalRawLocalFileSystem hdfsFileSystem, XAFileSystem xaFileSystem, Session xaSession)
    {
        this.hdfsFileSystem = hdfsFileSystem;
        this.xaFileSystem = xaFileSystem;
        this.xaSession = xaSession;
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
