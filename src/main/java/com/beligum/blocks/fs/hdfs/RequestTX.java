package com.beligum.blocks.fs.hdfs;

import org.xadisk.bridge.proxies.interfaces.XASession;

import javax.transaction.*;
import javax.transaction.xa.XAResource;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by bram on 2/1/16.
 */
public class RequestTX
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private final Transaction transaction;
    private XASession xdiskSession;
    private Set<XAResource> resources;

    //-----CONSTRUCTORS-----
    public RequestTX(Transaction transaction)
    {


        this.transaction = transaction;
        this.resources = new HashSet<>();
    }

    //-----PUBLIC METHODS-----
    public void registerResource(XAResource xaResource) throws IOException
    {
        try {
            this.resources.add(xaResource);
            this.transaction.enlistResource(xaResource);
        }
        catch (Exception e) {
            throw new IOException("Error occurred while registering sub-transaction", e);
        }
    }
    public boolean hasResource(XAResource xaResource)
    {
        return this.resources.contains(xaResource);
    }
    public void commit() throws IOException
    {
        try {
            this.transaction.commit();
        }
        catch (Exception e) {
            throw new IOException("Error occurred while committing main transaction", e);
        }
    }
    public void rollback() throws IOException
    {
        try {
            this.transaction.rollback();
        }
        catch (SystemException e) {
            throw new IOException("Error occurred while rolling back main transaction", e);
        }
    }
    public synchronized XASession getXdiskSession()
    {
        return xdiskSession;
    }
    public synchronized void setXdiskSession(XASession xdiskSession)
    {
        this.xdiskSession = xdiskSession;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
