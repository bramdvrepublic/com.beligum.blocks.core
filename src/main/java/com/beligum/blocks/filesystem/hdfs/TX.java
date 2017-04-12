package com.beligum.blocks.filesystem.hdfs;

import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.ReleaseFilter;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.filesystem.hdfs.bitronix.CustomBitronixResourceProducer;
import com.beligum.blocks.filesystem.index.ifaces.IndexConnection;
import org.openrdf.query.algebra.If;
import org.xadisk.bridge.proxies.interfaces.XASession;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static javax.transaction.xa.XAResource.TMFAIL;
import static javax.transaction.xa.XAResource.TMSUCCESS;

/**
 * This is actually a wrapper around a central JTA transaction, but with some additional functionality
 * to make our life easier.
 * <p>
 * Found some interesting information in this PDF:
 * (via https://wiki.kuali.org/pages/viewpage.action?pageId=18121702)
 * https://wiki.kuali.org/download/attachments/18121702/AtomikosTransactionsGuide.pdf?api=v2
 * <p>
 * Created by bram on 2/1/16.
 */
public class TX
{
    //-----CONSTANTS-----
    public interface Listener
    {
        void transactionEnded(int status);
    }

    //-----VARIABLES-----
    private TransactionManager transactionManager;
    private Transaction jtaTransaction;
    private Set<XAResource> registeredResources;
    private XASession xdiskSession;

    //-----CONSTRUCTORS-----
    public TX(TransactionManager transactionManager) throws Exception
    {
        this(transactionManager, 0);
    }
    public TX(TransactionManager transactionManager, long timeoutMillis) throws Exception
    {
        try {
            //keep a reference to the manager
            this.transactionManager = transactionManager;

            //Modify the timeout value that is associated with transactions started by the current thread with the begin method.
            this.transactionManager.setTransactionTimeout((int) (timeoutMillis / 1000.0));

            //create a new transaction and associate it with the current thread.
            //Note that this will throw an error if there's already a transaction attached to the current thread (no nesting supported)
            this.transactionManager.begin();

            //get the transaction object that represents the transaction context of the calling thread.
            this.jtaTransaction = transactionManager.getTransaction();

            //start out with an empty set of sub-transactions
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
    public synchronized void registerResource(XAResource xaResource, Listener listener) throws IOException
    {
        try {
            final CustomBitronixResourceProducer bitronixProducer = StorageFactory.getBitronixResourceProducer();
            bitronixProducer.registerResource(xaResource);
            this.jtaTransaction.registerSynchronization(new Synchronization()
            {
                @Override
                public void beforeCompletion()
                {
                }
                @Override
                public void afterCompletion(int status)
                {
                    bitronixProducer.unregisterResource(xaResource);

                    If we reach this point, the transaction is closed, regardless of it's timeout, we should find a callback for that

                    if (listener != null) {
                        listener.transactionEnded(status);
                    }
                }
            });

            //From the PDF mentioned above:
            // enlistResource: this method adds work to the transaction. The required argument is of type XAResource, which
            // is an interface for resources that understand two-phase commit. By enlisting an XAResource, the work that it
            // represents will undergo the same outcome as the transaction. If different resources are enlisted, then their outcome
            // will be consistent with the transaction's outcome, meaning that either all will commit or all with rollback.
            this.jtaTransaction.enlistResource(xaResource);

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
    public synchronized void setAndRegisterXdiskSession(XASession xdiskSession) throws Exception
    {
        this.xdiskSession = xdiskSession;
        this.registerResource(xdiskSession.getXAResource(), null);
    }
    /**
     * This is the main and single deconstructor, wrapping and handling all internal administration
     */
    public synchronized void close(boolean forceRollback) throws Exception
    {
        try {
            if (forceRollback || this.getStatus() != Status.STATUS_ACTIVE) {
                this.rollback();
            }
            else {
                //this is the general case: try to commit and (try to) rollback on error
                try {
                    this.commit();
                }
                catch (Throwable e) {
                    try {
                        Logger.warn("Caught exception while committing transaction, trying to rollback instead", e);
                        this.rollback();
                    }
                    catch (Throwable e1) {
                        Logger.warn("Caught exception while rolling back a transaction after a failed commit; this is bad", e);
                    }
                }
            }
        }
        finally {
            try {
                this.close();
            }
            catch (Throwable e) {
                throw new IOException("Exception caught while closing a transaction; this is bad", e);
            }
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private int getStatus() throws Exception
    {
        return this.jtaTransaction == null ? null : this.jtaTransaction.getStatus();
    }
    /**
     * Atomically commit all sub-transactions registered in this request TX
     *
     * @see ReleaseFilter
     */
    private void commit() throws Exception
    {
        //From the PDF mentioned above:
        // commit: same as TransactionManager.commit(). This method should not be called randomly: first, every XAResource
        // that was enlisted should also be properly delisted. Otherwise, XA-level protocol errors can occur.
        try {
            this.delistAllResources(TMSUCCESS);
            this.jtaTransaction.commit();
        }
        catch (Exception e) {
            throw new IOException("Error occurred while committing main transaction", e);
        }
    }
    /**
     * Atomically rollback all sub-transactions registered in this request TX
     *
     * @see ReleaseFilter
     */
    private void rollback() throws Exception
    {
        //From the PDF mentioned above:
        // rollback: same as TransactionManager.rollback(). As with commit, this method should not be called randomly:
        // first, every resource that was enlisted should also be delisted. Otherwise, XA-level protocol errors can occur
        try {
            this.delistAllResources(TMFAIL);
            this.jtaTransaction.rollback();
        }
        catch (Exception e) {
            throw new IOException("Error occurred while rolling back main transaction", e);
        }
    }
    /**
     * This is called at the very end of each request,
     * after commit/rollback has been performed.
     *
     * @see ReleaseFilter
     */
    private void close() throws Exception
    {
        IOException lastError = null;

        for (XAResource r : this.registeredResources) {
            if (r instanceof IndexConnection) {
                try {
                    ((IndexConnection) r).close();
                }
                catch (IOException e) {
                    Logger.error("Error while closing an index connection in request transaction close; " + R.requestContext().getJaxRsRequest().getUriInfo().getRequestUri(), e);
                    lastError = e;
                }
            }
        }

        //really close it down
        this.registeredResources.clear();
        this.registeredResources = null;
        this.jtaTransaction = null;

        if (lastError != null) {
            throw lastError;
        }
    }
    private void delistAllResources(int flag)
    {
        //From the PDF mentioned above:
        // delistResource: this method indicates that the application stops using the XAResource for this transaction. The
        // XAResource is essentially a connection to the underlying data source, and this method notifies the transaction
        // manager that the connection becomes available for two-phase commit processing. There are two special cases: if
        // a flag value of TMSUSPEND is given as a parameter, then the method call merely indicates that the application
        // is temporarily done and intends to come back to this work. This merely serves for internal optimizations inside
        // the data source. You should call this method if the transaction is being suspended. Coming back to such a suspended
        // work's context is done by calling enlistResource again, with the same XAResource. The second special
        // case is when TMFAIL is supplied as argument. This can be done to indicate that a failure has happened and that
        // the application is uncertain about the work that was done. In this case, commit should not be allowed, because
        // there is uncertainty about the contents of the transaction. For instance, if a SQLException occurs during a SQL
        // update, then the application can not know if the update was done or not. In that case, it should delist the resource
        // with the TMFAIL flag, because committing the transaction would lead to unknown effects on the data; this
        // could lead to corrupt data
        if (this.registeredResources != null && this.jtaTransaction != null) {
            for (XAResource r : this.registeredResources) {
                try {
                    this.jtaTransaction.delistResource(r, flag);
                }
                catch (Exception e) {
                    //let's swallow but log this as I think it's not that bad?
                    Logger.error("Error while delisting a transaction participant", e);
                }
            }
        }
    }
}
