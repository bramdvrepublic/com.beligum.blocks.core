package com.beligum.blocks.fs.hdfs;

import com.atomikos.icatch.jta.UserTransactionManager;
import org.xadisk.bridge.proxies.interfaces.XASession;

import javax.transaction.SystemException;
import javax.transaction.xa.XAResource;
import java.io.IOException;

/**
 * Created by bram on 2/1/16.
 */
public class RequestTX
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private final UserTransactionManager transactionManager;
    private XASession xdiskSession;

    //-----CONSTRUCTORS-----
    public RequestTX(UserTransactionManager transactionManager) throws IOException
    {
        try {
            this.transactionManager = transactionManager;
            this.transactionManager.init();
            //this.transactionManager.setTransactionTimeout(60);
            this.transactionManager.begin();
        }
        catch (Exception e) {
            throw new IOException("Error while starting transaction manager", e);
        }
    }

    //-----PUBLIC METHODS-----
    public void registerResource(XAResource xaResource) throws IOException
    {
        try {
            this.transactionManager.getTransaction().enlistResource(xaResource);
        }
        catch (Exception e) {
            throw new IOException("Error occurred while registering sub-transaction", e);
        }
    }
    public void commit() throws IOException
    {
        try {
            this.transactionManager.commit();
        }
        catch (Exception e) {
            throw new IOException("Error occurred while committing main transaction", e);
        }
    }
    public void rollback() throws IOException
    {
        try {
            this.transactionManager.rollback();
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
    public void close()
    {
        this.transactionManager.close();
//        for (XAResource r : this.resources) {
//            try {
//                this.transaction.delistResource(r, XAResource.TMSUCCESS);
//            }
//            catch (SystemException e) {
//                Logger.error("Exception caught while delisting resource; this is bad; "+r, e);
//            }
//        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
