package com.beligum.blocks.fs.atomic.hdfs;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.fs.atomic.hdfs.exceptions.TransactionException;
import com.beligum.blocks.fs.atomic.hdfs.ifaces.TransactionManager;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.protocol.LayoutVersion;
import org.apache.hadoop.hdfs.server.namenode.EditLogFileOutputStream;
import org.apache.hadoop.hdfs.server.namenode.EditLogOutputStream;
import org.apache.hadoop.hdfs.server.namenode.FSEditLogOp;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by bram on 1/31/16.
 */
public class Transaction implements AutoCloseable
{
    //-----CONSTANTS-----
    private static final String JOURNAL_FILE_PREFIX = "journal-";
    private static final String JOURNAL_FILE_EXT = ".editlog";
    private static final int OUTPUT_BUFFER_CAPACITY = 512*1024;
    private static final int LAYOUT_VERSION = LayoutVersion.Feature.RESERVED_REL2_4_0.getInfo().getLayoutVersion();

    //-----VARIABLES-----
    private TransactionManager transactionManager;
    private long txId;
    private EditLogOutputStream editLogStream = null;
    private boolean active; // neither committed nor rolled back
    private boolean committed; // if false and !active, then rolled back
    private volatile boolean shouldCommit;
    private String debugCrashHere = null;
    private ArrayList rollbackCloseExceptions;

    //-----CONSTRUCTORS-----
    protected Transaction(TransactionManager transactionManager, long txId) throws IOException
    {
        this.transactionManager = transactionManager;
        this.txId = txId;

        Path journalFile = transactionManager.getJournalDir().resolve(JOURNAL_FILE_PREFIX + txId + JOURNAL_FILE_EXT);
        this.editLogStream = new EditLogFileOutputStream(new Configuration(), journalFile.toFile(), OUTPUT_BUFFER_CAPACITY);
        this.editLogStream.create(LAYOUT_VERSION);

        this.active = true;
        this.committed = false;
        this.shouldCommit = false;
    }

    //-----PUBLIC METHODS-----
    /**
     * Open a file for reading.  This has no effect on the transaction.
     * It is provided for symmetry only.
     * The only difference between using this method and calling the
     * FileInputStream constructor directly is that when the transaction
     * ends, the file will be closed.
     */

    public synchronized Object run(FSAction op) throws IOException, TransactionException
    {
        Object retVal = null;

        checkActive();

//        op.setTransaction(this);
        op.setTransactionId(this.txId);

        try {
            op.prepare();
            testCrashPoint("post-preserve");
//            journal.writeAction(op);
            this.editLogStream.write(op.getEditLogOp());
            testCrashPoint("post-write");
            op.createBackup();
            testCrashPoint("post-backup");
            // We want to catch IOExceptions and unexpected exceptions
            // (RuntimeException's), but not TransactionException
            // or InconsistentStateException.
        }
        catch (RuntimeException e) {
            rollback();
            throw new TransactionException(e);
        }
        catch (IOException e) {
            testCrashPoint("preservation-ioexception");
            rollback();
            throw new TransactionException(e);
        }

        try {
            retVal = op.execute();
            testCrashPoint("post-run");
        }
        catch (RuntimeException e) {
            try {
//                journal.actionFailed();
                this.editLogStream.abort();
                testCrashPoint("post-action-failed");
            }
            catch (Exception e2) {
                testCrashPoint("action-failed-exception");
                rollback();
                throw new TransactionException(e2, e);
            }
            throw e;
        }

        return retVal;
    }
    @Override
    public void close() throws Exception
    {

    }
    public boolean isActive()
    {
        return this.active;
    }
    public List getRollbackCloseExceptions()
    {
        return Collections.unmodifiableList(rollbackCloseExceptions);
    }
    public void setDebugCrashPoint(String s)
    {
        debugCrashHere = s.intern();
    }

    //-----PROTECTED METHODS-----
    // package access because called from TM
    protected void rollback()
    {
        rollbackCloseExceptions = closeAllCatchingExceptions();
        this.editLogStream.write();
        journal.rollback();
        active = false;
        committed = false;
    }

    //-----PRIVATE METHODS-----
    private void checkActive() throws IllegalStateException
    {
        if (!this.active) {
            throw new IllegalStateException("Can't write execute an action; transaction is not active (anymore)");
        }
    }
    private ArrayList closeAllCatchingExceptions()
    {
        Iterator<FSEditLogOp> i = journal.getActions().iterator();
        ArrayList result = new ArrayList();
        while (i.hasNext()) {
            FSEditLogOp a = i.next();
            try {
                a.close();
            }
            catch (IOException e) {
                result.add(e);
            }
        }
        return result;
    }
    private void testCrashPoint(String s)
    {
        Logger.info("crashPoint: s = " + s + ", crashHere = " + debugCrashHere);

        if (s == debugCrashHere) { // OK because of String.intern()
            Logger.info("CRASH *** " + debugCrashHere + " ***");
            Runtime.getRuntime().halt(-1);
        }
    }
}
