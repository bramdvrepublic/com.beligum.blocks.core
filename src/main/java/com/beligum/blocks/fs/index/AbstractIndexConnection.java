package com.beligum.blocks.fs.index;

import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.fs.index.ifaces.IndexConnection;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.io.IOException;

/**
 * Created by bram on 2/22/16.
 */
public abstract class AbstractIndexConnection implements IndexConnection
{
    //-----CONSTANTS-----
    //In XADisk, default value is 60 seconds.
    private static final int DEFAULT_TX_TIMEOUT_SECS = 60;

    //-----VARIABLES-----
    private int txTimeout;

    //-----CONSTRUCTORS-----
    protected AbstractIndexConnection()
    {
        this.txTimeout = DEFAULT_TX_TIMEOUT_SECS;
    }

    //-----PUBLIC METHODS-----
    /**
     * Tells the caller if this resource has the same resource manager
     * as the argument resource.
     * <p/>
     * The transaction manager needs this method to be able to decide
     * if the {@link #start(Xid, int) start} method should be given the
     * {@link #TMJOIN} flag.
     *
     * @throws XAException If an error occurred.
     */
    @Override
    public boolean isSameRM(XAResource xaResource) throws XAException
    {
        try {
            return StorageFactory.getCurrentRequestTx().hasResource(xaResource);
        }
        catch (IOException e) {
            throw new XAException(e == null ? null : e.getMessage());
        }
    }

    /**
     * Get the current transaction timeout value for this resource.
     *
     * @return The current timeout value, in seconds.
     * @throws XAException If an error occurred.
     */
    @Override
    public int getTransactionTimeout() throws XAException
    {
        return this.txTimeout;
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
     * @throws XAException If an error occurred.
     */
    @Override
    public boolean setTransactionTimeout(int seconds) throws XAException
    {
        return false;
    }

    /**
     * Called to associate the resource with a transaction.
     * <p/>
     * If the flags argument is {@link #TMNOFLAGS}, the transaction must not
     * previously have been seen by this resource manager, or an
     * {@link XAException} with error code XAER_DUPID will be thrown.
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
     * @throws XAException If an error occurred.
     */
    @Override
    public void start(Xid xid, int flags) throws XAException
    {
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
     * @throws XAException If an error occurred.
     */
    @Override
    public Xid[] recover(int flag) throws XAException
    {
        throw new XAException("Not implemented yet");
        //return new Xid[0];
    }

    /**
     * Roll back the work done on this resource in the given transaction.
     *
     * @param xid The id of the transaction to commit work for.
     * @throws XAException If an error occurred.
     */
    @Override
    public abstract void rollback(Xid xid) throws XAException;

    /**
     * Tells the resource manager to forget about a heuristic decision.
     *
     * @param xid The id of the transaction that was ended with a heuristic
     *            decision.
     * @throws XAException If an error occurred.
     */
    @Override
    public void forget(Xid xid) throws XAException
    {
        throw new XAException("Not implemented yet");
    }

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
     * @throws XAException If an error occurred.
     */
    @Override
    public int prepare(Xid xid) throws XAException
    {
        this.prepareCommit(xid);

        return XA_OK;
    }
    //renamed to make it more clear what it does
    public abstract void prepareCommit(Xid xid) throws XAException;

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
     * @throws XAException If an error occurred.
     */
    @Override
    public abstract void commit(Xid xid, boolean onePhase) throws XAException;

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
     * @throws XAException If an error occurred.
     */
    @Override
    public void end(Xid xid, int flags) throws XAException
    {
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
