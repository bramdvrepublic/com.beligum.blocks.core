package com.beligum.blocks.fs.atomic.hdfs;

import org.apache.hadoop.hdfs.server.namenode.FSEditLogOp;
import org.apache.hadoop.hdfs.server.namenode.FSEditLogOpCodes;

import java.io.IOException;

/**
 * Created by bram on 2/1/16.
 */
public abstract class FSAction extends FSEditLogOp
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private FSEditLogOp editLogOp;

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    public void setTransactionId(long txId)
    {
        this.editLogOp.setTransactionId(txId);

        FSEditLogOp.OpInstanceCache opCache = new FSEditLogOp.OpInstanceCache();
        FSEditLogOp testDelete = (DeleteOp)opCache.get(FSEditLogOpCodes.OP_DELETE);



        DeleteOp op = DeleteOp.getInstance(cache.get())
                              .setPath(src)
                              .setTimestamp(timestamp);
        logRpcIds(op, toLogRpcIds);
        logEdit(op);
    }

    //-----PROTECTED METHODS-----
    /**
     * Prepare for the action by storing all information required
     * for undo in non-transient instance variables.
     * <i>This method should not change the state of the filesystem.</i>
     * After this call, the Action object will be serialized into the journal.
     * Exceptions are considered transaction errors, and will result in a
     * rollback.
     */
    protected abstract void prepare() throws IOException;

    /**
     * Create a backup file, if necessary.
     * Exceptions cause rollback.
     */
    protected void createBackup() throws IOException
    {
    }

    /**
     * Do the action.  Exceptions are reported to the user as is.
     */
    protected abstract Object execute() throws IOException;

    /**
     * Called at commit or rollback, only if action was successful.
     */
    protected void close() throws IOException
    {
    }

    /**
     * Undo the effects of the action, if those effects are present.
     * This must be <i>locally idempotent</i>; it must be able to be
     * invoked multiple times with the same results, assuming no other
     * changes to the relevant files are made between invocations.
     * <p/>
     * Exceptions thrown from this method are considered
     * to leave the system in an inconsistent state.
     */
    protected abstract void undo() throws IOException;

    /**
     * Delete backup and perform other needed cleanup.
     * Called after commit and rollback.
     * Exceptions thrown during cleanup do not prevent commit or rollback
     * from succeeding.  They are not thrown out of Transaction methods;
     * instead, you can get them by calling Transaction.getCleanupExceptions.
     */
    protected void cleanup() throws IOException
    {
    }

    protected FSEditLogOp getEditLogOp()
    {
        return this.editLogOp;
    }

    //-----PRIVATE METHODS-----

}
