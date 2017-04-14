package com.beligum.blocks.filesystem.index;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.filesystem.index.ifaces.IndexConnection;
import com.beligum.blocks.filesystem.index.ifaces.Indexer;
import org.apache.lucene.util.ThreadInterruptedException;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.io.IOException;
import java.io.Serializable;
import java.util.EnumSet;

/**
 * Good starting points
 * https://github.com/Novartis/ontobrowser/blob/master/src/main/java/com/novartis/pcs/ontology/service/search/LuceneIndexWriterXAResource.java
 * https://svn.alfresco.com/repos/alfresco-open-mirror/alfresco/HEAD/root/projects/repository/source/java/org/alfresco/repo/search/impl/lucene/AbstractLuceneIndexerAndSearcherFactory.java
 * <p/>
 * Created by bram on 2/22/16.
 */
public abstract class AbstractIndexConnection implements IndexConnection, Serializable
{
    //-----CONSTANTS-----
    private enum TransactionState
    {
        NONE,
        ACTIVE,
        SUSPENDED,
        IDLE,
        PREPARED,
        ROLLBACK_ONLY
    }

    //this is the same as the default of XADisk
    private static final int DEFAULT_TRANSACTION_TIMEOUT_SECS = 10;

    //-----VARIABLES-----
    private int transactionTimeout = DEFAULT_TRANSACTION_TIMEOUT_SECS;
    private TransactionState state;
    private Xid currentXid;

    //-----CONSTRUCTORS-----
    protected AbstractIndexConnection()
    {
        super();

        this.state = TransactionState.NONE;
    }

    //-----PUBLIC METHODS-----
    /**
     * Get the current transaction timeout value for this resource.
     *
     * @return The current timeout value, in seconds.
     * @throws XAException If an setRollbackOnly occurred.
     */
    @Override
    public int getTransactionTimeout() throws XAException
    {
        return this.transactionTimeout;
    }

    /**
     * Set the transaction timeout value for this resource.
     * <p/>
     * If the <code>seconds</code> argument is <code>0</code>, the
     * timeout value is set to the default timeout value of the resource
     * manager.
     * <p/>
     * Not all resource managers support setting the timeout value.
     * If the resource manager does not support setting the timeout
     * value, it should return false.
     *
     * @param seconds The timeout value, in seconds.
     * @return True if the timeout value could be set, otherwise false.
     * @throws XAException If an setRollbackOnly occurred.
     */
    @Override
    public boolean setTransactionTimeout(int seconds) throws XAException
    {
        this.transactionTimeout = seconds;

        return true;
    }

    /**
     * Tells the caller if this resource has the same resource manager
     * as the argument resource.
     * <p/>
     * The transaction manager needs this method to be able to decide
     * if the {@link #start(Xid, int) start} method should be given the
     * {@link #TMJOIN} flag.
     *
     * @throws XAException If an setRollbackOnly occurred.
     */
    @Override
    public boolean isSameRM(XAResource xaResource) throws XAException
    {
        boolean retVal = false;

        if (xaResource instanceof AbstractIndexConnection) {
            retVal = this.getResourceManager() == ((AbstractIndexConnection) xaResource).getResourceManager();
        }

        return retVal;
    }

    /**
     * Called to associate the resource with a transaction.
     * <p/>
     * If the flags argument is {@link #TMNOFLAGS}, the transaction must not
     * previously have been seen by this resource manager, or an
     * {@link XAException} with setRollbackOnly code XAER_DUPID will be thrown.
     * <p/>
     * If the flags argument is {@link #TMJOIN}, the resource will join a
     * transaction previously seen by tis resource manager.
     * <p/>
     * If the flags argument is {@link #TMRESUME} the resource will
     * resume the transaction association that was suspended with
     * end(TMSUSPEND).
     *
     * @param xid   The id of the transaction to associate with.
     * @param flags Must be either {@link #TMNOFLAGS}, {@link #TMJOIN} or {@link #TMRESUME}.
     * @throws XAException If an setRollbackOnly occurred.
     */
    @Override
    public void start(Xid xid, int flags) throws XAException
    {
        switch (flags) {
            case TMJOIN:
                changeState(xid, EnumSet.of(TransactionState.IDLE), TransactionState.ACTIVE);
                break;
            case TMRESUME:
                changeState(xid, EnumSet.of(TransactionState.SUSPENDED), TransactionState.ACTIVE);
                break;
            case TMNOFLAGS:
                setXid(xid);
                break;
            default:
                throw new XAException(XAException.XAER_INVAL);
        }

        try {
            this.begin();
        }
        catch (Exception e) {
            throw newXAException(XAException.XAER_RMERR, e);
        }
    }

    /**
     * Method that should be implemented by specific subclasses to begin a new transaction.
     */
    protected abstract void begin() throws IOException;

    /**
     * Prepare to commit the work done on this resource in the given
     * transaction.
     * <p/>
     * This method cannot return a status indicating that the transaction
     * should be rolled back. If the resource wants the transaction to
     * be rolled back, it should throw an <code>XAException</code> at the
     * caller.
     *
     * @param xid The id of the transaction to prepare to commit work for.
     * @return Either {@link #XA_OK} or {@link #XA_RDONLY}.
     * @throws XAException If an setRollbackOnly occurred.
     */
    @Override
    public int prepare(Xid xid) throws XAException
    {
        this.changeState(xid, EnumSet.of(TransactionState.IDLE), null);

        try {
            this.prepareCommit();

            changeState(xid, EnumSet.of(TransactionState.IDLE), TransactionState.PREPARED);

            return XAResource.XA_OK;
        }
        catch (Exception e) {
            throw newXAException(XAException.XAER_RMERR, e);
        }
    }
    /**
     * Method that should be implemented by subclasses to start off the two-phase commit
     * (renamed to make it more clear what it does)
     */
    protected abstract void prepareCommit() throws IOException;

    /**
     * Commit the work done on this resource in the given transaction.
     * <p/>
     * If the <code>onePhase</code> argument is true, one-phase
     * optimization is being used, and the {@link #prepare(Xid) prepare}
     * method must not have been called for this transaction.
     * Otherwise, this is the second phase of the two-phase commit protocol.
     *
     * @param xid      The id of the transaction to commit work for.
     * @param onePhase If true, the transaction manager is using one-phase
     *                 optimization.
     * @throws XAException If an setRollbackOnly occurred.
     */
    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException
    {
        changeState(xid, EnumSet.of(onePhase ? TransactionState.IDLE : TransactionState.PREPARED), null);

        try {
            this.commit();
            clearXid();
        }
        catch (Exception e) {
            this.rollback(xid);
            throw newXAException(XAException.XAER_RMERR, e);
        }
    }

    /**
     * Method that should be implemented by subclasses to end the two-phase commit
     */
    protected abstract void commit() throws IOException;

    /**
     * Roll back the work done on this resource in the given transaction.
     *
     * @param xid The id of the transaction to commit work for.
     * @throws XAException If an setRollbackOnly occurred.
     */
    @Override
    public void rollback(Xid xid) throws XAException
    {
        changeState(xid, EnumSet.of(TransactionState.IDLE, TransactionState.PREPARED, TransactionState.ROLLBACK_ONLY), null);

        try {
            this.rollback();
        }
        catch (Exception e) {
            throw newXAException(XAException.XAER_RMERR, e);
        }
        finally {
            clearXid();
        }
    }

    /**
     * Method that should be implemented by subclasses to rollback the two-phase commit
     */
    protected abstract void rollback() throws IOException;

    /**
     * Tells the resource manager to forget about a heuristic decision.
     *
     * @param xid The id of the transaction that was ended with a heuristic
     *            decision.
     * @throws XAException If an setRollbackOnly occurred.
     */
    @Override
    public void forget(Xid xid) throws XAException
    {
        clearXid();
    }

    /**
     * Called to disassociate the resource from a transaction.
     * <p/>
     * If the flags argument is {@link #TMSUCCESS}, the portion of work
     * was done sucessfully.
     * <p/>
     * If the flags argument is {@link #TMFAIL}, the portion of work
     * failed. The resource manager may mark the transaction for
     * rollback only to avoid the transaction being committed.
     * <p/>
     * If the flags argument is {@link #TMSUSPEND} the resource will
     * temporarily suspend the transaction association. The transaction
     * must later be re-associated by giving the {@link #TMRESUME} flag
     * to the {@link #start(Xid, int) start} method.
     *
     * @param xid   The id of the transaction to disassociate from.
     * @param flags Must be either {@link #TMSUCCESS}, {@link #TMFAIL}
     *              or {@link #TMSUSPEND}.
     * @throws XAException If an setRollbackOnly occurred.
     */
    @Override
    public void end(Xid xid, int flags) throws XAException
    {
        switch (flags) {
            case TMSUCCESS:
                changeState(xid, EnumSet.of(TransactionState.SUSPENDED, TransactionState.ACTIVE), TransactionState.IDLE);
                break;
            case TMSUSPEND:
                changeState(xid, EnumSet.of(TransactionState.ACTIVE), TransactionState.SUSPENDED);
                break;
            case TMFAIL:
                changeState(xid, EnumSet.of(TransactionState.ACTIVE), TransactionState.ROLLBACK_ONLY);
                break;
            default:
                throw new XAException(XAException.XAER_INVAL);
        }
    }

    /**
     * Return a list of transactions that are in a prepared or heuristically
     * state.
     * <p/>
     * This method looks not only at the resource it is invoked on, but
     * also on all other resources managed by the same resource manager.
     * It is intended to be used by the application server when recovering
     * after a server crash.
     * <p/>
     * A recovery scan is done with one or more calls to this method.
     * At the first call, {@link #TMSTARTRSCAN} must be in the
     * <code>flag</code> argument to indicate that the scan should be started.
     * During the recovery scan, the resource manager maintains an internal
     * cursor that keeps track of the progress of the recovery scan.
     * To end the recovery scan, the {@link #TMENDRSCAN} must be passed
     * in the <code>flag</code> argument.
     *
     * @param flag Must be either {@link #TMNOFLAGS}, {@link #TMSTARTRSCAN},
     *             {@link #TMENDRSCAN} or <code>TMSTARTRSCAN|TMENDRSCAN</code>.
     * @return An array of zero or more transaction ids.
     * @throws XAException If an setRollbackOnly occurred.
     */
    @Override
    public Xid[] recover(int flag) throws XAException
    {
        //currently completely disabled
        return currentXid == null || state != TransactionState.PREPARED ? new Xid[0] : new Xid[] {currentXid};
    }

    //-----PROTECTED METHODS-----
    protected abstract Indexer getResourceManager();

    //-----PRIVATE METHODS-----
    private synchronized void setXid(Xid xid) throws XAException
    {
        // start method should have been called with TMJOIN
        // because isSameRM would have returned true
        if (currentXid != null && currentXid.equals(xid)) {
            throw new XAException(XAException.XAER_DUPID);
        }

        while (state != TransactionState.NONE && currentXid != null) {
            try {
                wait();
            }
            catch (InterruptedException e) {
                if (Thread.interrupted()) { // clears interrupted status
                    Logger.error("Thread waiting for transaction (id="
                                 + currentXid
                                 + ") to complete has been interrupted?", e);
                    throw new ThreadInterruptedException(e);
                }
            }
        }

        currentXid = xid;
        state = TransactionState.ACTIVE;
    }
    private synchronized void changeState(Xid xid, EnumSet<TransactionState> from, TransactionState to) throws XAException
    {
        if (currentXid == null) {
            Logger.error("No transaction currently associated");
            throw new XAException(XAException.XAER_NOTA);
        }

        if (!currentXid.equals(xid)) {
            Logger.error("Transaction (id="
                         + xid + " does not match current transaction (id="
                         + currentXid + ")");
            throw new XAException(XAException.XAER_NOTA);
        }

        if (!from.contains(state)) {
            Logger.error("Transaction (id="
                         + currentXid + ") in illegal state " + state);
            throw new XAException(XAException.XAER_PROTO);
        }

        if (to != null) {
            this.state = to;
        }
    }
    private synchronized void clearXid()
    {
        currentXid = null;
        state = TransactionState.NONE;
        notify();
    }
    private XAException newXAException(int error, Exception cause)
    {
        XAException e = new XAException(error);
        e.initCause(cause);
        return e;
    }
}
