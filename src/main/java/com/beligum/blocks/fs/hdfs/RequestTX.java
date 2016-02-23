package com.beligum.blocks.fs.hdfs;

import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.fs.index.ifaces.IndexConnection;
import org.xadisk.bridge.proxies.interfaces.XASession;

import javax.transaction.SystemException;
import javax.transaction.Transaction;
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

    private Set<XAResource> registeredResources;
    private XASession xdiskSession;

    //-----CONSTRUCTORS-----
    public RequestTX(Transaction transaction) throws IOException
    {
        try {
            this.transaction = transaction;
            this.registeredResources = new HashSet<>();
        }
        catch (Exception e) {
            throw new IOException("Error while starting transaction manager", e);
        }
    }

    //-----PUBLIC METHODS-----
    /**
     * Attach an XAResource to the current TX session
     */
    public void registerResource(XAResource xaResource) throws IOException
    {
        try {
            this.transaction.enlistResource(xaResource);
            this.registeredResources.add(xaResource);
        }
        catch (Exception e) {
            throw new IOException("Error occurred while registering sub-transaction", e);
        }
    }
    /**
     * Return the registered XADisk instance for this request or null if none was attached (yet)
     */
    public synchronized XASession getXdiskSession()
    {
        return xdiskSession;
    }
    /**
     * Allows us to attach an XADisk instance after this RequestTX was already created (and being used for other TX uses)
     */
    public synchronized void setXdiskSession(XASession xdiskSession)
    {
        this.xdiskSession = xdiskSession;
    }
    /**
     * Atomically commit all sub-transactions registered in this request TX
     * @see com.beligum.blocks.fs.hdfs.RequestTransactionFilter
     */
    public void commit() throws IOException
    {
        try {
            this.transaction.commit();
        }
        catch (Exception e) {
            throw new IOException("Error occurred while committing main transaction", e);
        }
    }
    /**
     * Atomically rollback all sub-transactions registered in this request TX
     * @see com.beligum.blocks.fs.hdfs.RequestTransactionFilter
     */
    public void rollback() throws IOException
    {
        try {
            this.transaction.rollback();
        }
        catch (SystemException e) {
            throw new IOException("Error occurred while rolling back main transaction", e);
        }
    }
    /**
     * This is called at the very end of each request,
     * after commit/rollback has been performed.
     * @see com.beligum.blocks.fs.hdfs.RequestTransactionFilter
     */
    public void close() throws IOException
    {
        IOException lastError = null;

        for (XAResource r : this.registeredResources) {
            if (r instanceof IndexConnection) {
                try {
                    ((IndexConnection)r).close();
                }
                catch (IOException e) {
                    Logger.error("Error while closing an index connection in request transaction close; " + R.requestContext().getJaxRsRequest().getUriInfo().getRequestUri(), e);
                    lastError = e;
                }
            }
        }

        if (lastError!=null) {
            throw lastError;
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
