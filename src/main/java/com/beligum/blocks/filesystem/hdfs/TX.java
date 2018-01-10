/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.beligum.blocks.filesystem.hdfs;

import bitronix.tm.BitronixTransaction;
import bitronix.tm.internal.TransactionStatusChangeListener;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.ReleaseFilter;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.filesystem.hdfs.bitronix.CustomBitronixResourceProducer;
import com.beligum.blocks.filesystem.index.ifaces.XAClosableResource;
import org.xadisk.bridge.proxies.interfaces.XASession;

import javax.transaction.*;
import javax.transaction.xa.XAResource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
public class TX implements AutoCloseable
{
    //-----CONSTANTS-----
    public interface Listener
    {
        void transactionTimedOut(TX transaction);

        void transactionStatusChanged(TX transaction, int oldStatus, int newStatus);
    }

    //-----VARIABLES-----
    private TransactionManager transactionManager;
    private Transaction jtaTransaction;
    private Map<String, XAResource> registeredResources;
    private XASession xdiskSession;

    //-----CONSTRUCTORS-----
    public TX(TransactionManager transactionManager) throws Exception
    {
        this(transactionManager, null, 0);
    }
    public TX(TransactionManager transactionManager, Listener listener, long timeoutMillis) throws Exception
    {
        try {
            //keep a reference to the manager
            this.transactionManager = transactionManager;

            //Modify the timeout value that is associated with transactions started by the current thread with the begin method.
            this.transactionManager.setTransactionTimeout((int) (timeoutMillis / 1000.0));

            //instance a new transaction and associate it with the current thread.
            //Note that this will throw an setRollbackOnly if there's already a transaction attached to the current thread (no nesting supported)
            this.transactionManager.begin();

            //get the transaction object that represents the transaction context of the calling thread.
            this.jtaTransaction = transactionManager.getTransaction();
            if (this.jtaTransaction == null) {
                throw new IOException("I seem to have gotten a null-valued transaction from the transaction manager; this shouldn't happen...");
            }

            if (this.jtaTransaction instanceof BitronixTransaction) {
                BitronixTransaction btxTx = (BitronixTransaction) this.jtaTransaction;
                btxTx.addTransactionStatusChangeListener(new TransactionStatusChangeListener()
                {
                    @Override
                    public void statusChanged(int oldStatus, int newStatus)
                    {
                        //Logger.debug("Transaction " + jtaTransaction.hashCode() + " changed status from " + Decoder.decodeStatus(oldStatus)+" to "+Decoder.decodeStatus(newStatus));

                        if (listener != null) {
                            listener.transactionStatusChanged(TX.this, oldStatus, newStatus);

                            //catch this specific case internally to make the callback API a little easier to work with
                            if (oldStatus == Status.STATUS_ACTIVE && btxTx.timedOut()) {
                                listener.transactionTimedOut(TX.this);
                            }
                        }
                    }
                });
            }
            else {
                throw new IllegalStateException("Encountered an unimplemented transaction object; this shouldn't happen; " + this.jtaTransaction);
            }

            //start out with an empty set of sub-transactions
            this.registeredResources = new HashMap<>();
        }
        catch (Exception e) {
            throw new IOException("Error while starting a new transaction", e);
        }
    }

    //-----PUBLIC METHODS-----
    public synchronized boolean isActive() throws IOException
    {
        try {
            return this.getStatus() == Status.STATUS_ACTIVE;
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }
    /**
     * Attach an XAResource to the current TX session.
     * Note that the name should be a id, identifying the resource, so we can check if it has been registered before or not
     */
    public synchronized void registerResource(String resourceName, XAResource xaResource) throws IOException
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
                //note: this is the callback for the very end of the transaction (when it gets closed),
                // regardless of the TX timeout value
                @Override
                public void afterCompletion(int status)
                {
                    bitronixProducer.unregisterResource(xaResource);
                }
            });

            //From the PDF mentioned above:
            // enlistResource: this method adds work to the transaction. The required argument is of type XAResource, which
            // is an interface for resources that understand two-phase commit. By enlisting an XAResource, the work that it
            // represents will undergo the same outcome as the transaction. If different resources are enlisted, then their outcome
            // will be consistent with the transaction's outcome, meaning that either all will commit or all with rollback.
            this.jtaTransaction.enlistResource(xaResource);

            if (resourceName == null) {
                throw new NullPointerException("Can't register an XAResource without a name");
            }
            else if (this.registeredResources.containsKey(resourceName)) {
                //this probably means there's a synchronization problem, don't continue
                throw new IOException("XAResource with name '" + resourceName + "' already registered in this transaction, not registering it again!");
            }
            else {
                this.registeredResources.put(resourceName, xaResource);
            }
        }
        catch (Exception e) {
            throw new IOException("Error occurred while registering sub-transaction", e);
        }
    }
    public synchronized XAResource getRegisteredResource(String resourceName)
    {
        return this.registeredResources.get(resourceName);
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
        //Note: it shouldn't be possible to register an xdisk session twice
        this.registerResource("xdisk", xdiskSession.getXAResource());
    }
    /**
     * Modify this transaction such that the only possible outcome of the transaction is to roll back the transaction.
     */
    public synchronized void setRollbackOnly() throws Exception
    {
        if (this.jtaTransaction == null) {
            throw new IOException("Can't mark this transaction as rollback-only because it seems like it has been closed already...");
        }
        else {
            this.jtaTransaction.setRollbackOnly();
        }
    }
    /**
     * This is the main and single deconstructor, wrapping and handling all internal administration
     */
    public synchronized void close(boolean forceRollback) throws Exception
    {
        try {
            if (forceRollback || this.getStatus() != Status.STATUS_ACTIVE) {
                this.doRollback();
            }
            else {
                //this is the general case: try to commit and (try to) rollback on setRollbackOnly
                try {
                    this.doCommit();
                }
                catch (Throwable e) {
                    try {
                        Logger.error("Caught exception while committing transaction, trying to rollback instead", e);
                        this.doRollback();
                    }
                    catch (Throwable e1) {
                        Logger.error("Caught exception while rolling back a transaction after a failed commit; this is bad", e);
                    }
                }
            }
        }
        finally {
            try {
                this.doClose();
            }
            catch (Throwable e) {
                throw new IOException("Exception caught while closing a transaction; this is bad", e);
            }
        }
    }
    @Override
    public void close() throws Exception
    {
        this.close(false);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private int getStatus() throws SystemException
    {
        return this.jtaTransaction == null ? Status.STATUS_UNKNOWN : this.jtaTransaction.getStatus();
    }
    /**
     * Atomically commit all sub-transactions registered in this request TX
     *
     * @see ReleaseFilter
     */
    private void doCommit() throws IOException
    {
        //From the PDF mentioned above:
        // commit: same as TransactionManager.commit(). This method should not be called randomly: first, every XAResource
        // that was enlisted should also be properly delisted. Otherwise, XA-level protocol errors can occur.
        try {
            //Note: it's important *all* commits/rollback are serialized, because we ran into this problem illustrated here:
            //https://issues.apache.org/jira/browse/JENA-1302
            //By requiring a lock over the entire transaction manager, we force all of them to be atomic.
            synchronized (this.transactionManager) {
                //see rollback() for why this is commented out
                //this.delistAllResources(TMSUCCESS);
                this.jtaTransaction.commit();
            }
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
    private void doRollback() throws IOException
    {
        //From the PDF mentioned above:
        // rollback: same as TransactionManager.rollback(). As with commit, this method should not be called randomly:
        // first, every resource that was enlisted should also be delisted. Otherwise, XA-level protocol errors can occur.
        try {
            synchronized (this.transactionManager) {
                //Note: we don't do this anymore because of errors thrown (see BitronixTransaction.delistResource(); it first checks isWorking() and that is most probably true)
                //Found some docs that this shouldn't be called manually in most cases, see http://stackoverflow.com/questions/7168605/when-should-transaction-delistresource-be-called
                //this.delistAllResources(TMFAIL);
                this.jtaTransaction.rollback();
            }
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
    private void doClose() throws IOException
    {
        IOException lastError = null;

        for (XAResource r : this.registeredResources.values()) {
            if (r instanceof XAClosableResource) {
                try {
                    ((XAClosableResource) r).close();
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
            for (XAResource r : this.registeredResources.values()) {
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
