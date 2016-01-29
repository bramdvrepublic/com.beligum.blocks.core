package com.beligum.blocks.fs.atomic.v2;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/* Assumptions:
     delete is atomic
	 rename is atomic
	 create is atomic
	 open for writing is atomic
	 close is idempotent
*/

/* TODO:

*/

public class Transaction
{
    private TransactionManager tm;
    private Journal journal;
    private int number;     // unique txn number
    private boolean active; // neither committed nor rolled back
    private boolean committed; // if false and !active, then rolled back
    private boolean cleanupSuccessful; // if cleanup didn't give errors at end
    private volatile boolean shouldCommit;
    private ArrayList rollbackCloseExceptions;

    Transaction(TransactionManager tm, int num) throws TransactionException
    {
        try {
            this.tm = tm;
            File journalFile = new File(tm.getJournalDir(), "journal-" + num);
            number = num;
            journal = new Journal(journalFile, number);
            active = true;
            shouldCommit = false;
        }
        catch (IOException e) {
            throw new TransactionException(e);
        }
    }

    public TransactionManager getTransactionManager()
    {
        return tm;
    }

    public boolean isActive()
    {
        return active;
    }

    /**
     * Return the unique transaction number assigned by the Transaction
     * Manager.
     */
    public int getNumber()
    {
        return number;
    }

    /**
     * Open a file for reading.  This has no effect on the transaction.
     * It is provided for symmetry only.
     * The only difference between using this method and calling the
     * FileInputStream constructor directly is that when the transaction
     * ends, the file will be closed.
     */

    public synchronized Object run(Action a)
                    throws IllegalStateException, IOException,
                           TransactionException, InconsistentStateException
    {
        checkActive();
        a.setTransaction(this);
        try {
            a.prepare();
            crashPoint("post-preserve");
            journal.writeAction(a);
            crashPoint("post-write");
            a.createBackup();
            crashPoint("post-backup");
            // We want to catch IOExceptions and unexpected exceptions
            // (RuntimeException's), but not TransactionException
            // or InconsistentStateException.
        }
        catch (RuntimeException e) {
            rollback();
            throw new TransactionException(e);
        }
        catch (IOException e) {
            crashPoint("preservation-ioexception");
            rollback();
            throw new TransactionException(e);
        }

        try {
            Object result = a.execute();
            crashPoint("post-run");
            return result;
        }
        catch (RuntimeException e) {
            try {
                journal.actionFailed();
                crashPoint("post-action-failed");
            }
            catch (Exception e2) {
                crashPoint("action-failed-exception");
                rollback();
                throw new TransactionException(e2, e);
            }
            throw e;
        }
    }

    ////////////////////////////////////////////////////////////////
    /// Specific actions.

    public FileInputStream openInputStream(File f)
                    throws FileNotFoundException
    {
        checkActive();
        FileInputStream in = new FileInputStream(f);
        // Add an action so that the file will be automatically closed.
        journal.addAction(new OpenFileInputAction(in));
        return in;
    }

    private static class OpenFileInputAction extends Action
    {
        // Exists just to close the file.
        private FileInputStream in;

        OpenFileInputAction(FileInputStream in)
        {
            this.in = in;
        }

        protected void prepare()
        {
        }

        protected void close() throws IOException
        {
            in.close();
        }

        protected Object execute()
        {
            return null;
        }

        protected void undo()
        {
        }
    }

    public FileInputStream openInputStream(String s)
                    throws FileNotFoundException
    {
        return openInputStream(new File(s));
    }

    ////////////////////////////////////////////////////////////////
    // Open for writing.

    public FileOutputStream openOutputStream(File f, boolean append)
                    throws IOException, TransactionException, InconsistentStateException
    {
        return (FileOutputStream) run(new WriteAction(f, append));
    }

    /**
     * Convenience method for openOutputStream(File, boolean)
     */
    public FileOutputStream openOutputStream(File f)
                    throws IOException, TransactionException, InconsistentStateException
    {
        return openOutputStream(f, false);
    }

    /**
     * Convenience method for openOutputStream(File, boolean)
     */
    public FileOutputStream openOutputStream(String s, boolean append)
                    throws IOException, TransactionException, InconsistentStateException
    {
        return openOutputStream(new File(s), append);
    }

    /**
     * Convenience method for openOutputStream(File, boolean)
     */
    public FileOutputStream openOutputStream(String s)
                    throws IOException, TransactionException, InconsistentStateException
    {
        return openOutputStream(new File(s), false);
    }

    public boolean mkdir(File f)
                    throws IOException, TransactionException, InconsistentStateException
    {
        return ((Boolean) run(new MakeDirectoryAction(f))).booleanValue();
    }

    public boolean mkdir(String s)
                    throws IOException, TransactionException, InconsistentStateException
    {
        return mkdir(new File(s));
    }

    ////////////////////////////////////////////////////////////////
    // Open a RandomAccessFile.

    public RandomAccessFile openRandomAccess(File f, String mode)
                    throws IOException, TransactionException,
                           InconsistentStateException
    {
        checkActive();
        if (!mode.equals("rw")) {
            RandomAccessFile in = new RandomAccessFile(f, mode);
            // Add an action so that the file will be automatically closed.
            journal.addAction(new OpenRandomReadOnlyAction(in));
            return in;
        }
        else
            return (RandomAccessFile) run(new OpenRandomAccessAction(f));
    }

    public RandomAccessFile openRandomAccess(String s, String mode)
                    throws IOException, TransactionException,
                           InconsistentStateException
    {
        return openRandomAccess(new File(s), mode);
    }

    private static class OpenRandomReadOnlyAction extends Action
    {
        // Exists just to close the file.
        private RandomAccessFile in;

        OpenRandomReadOnlyAction(RandomAccessFile in)
        {
            this.in = in;
        }

        protected void close() throws IOException
        {
            in.close();
        }

        protected void prepare()
        {
        }

        protected Object execute()
        {
            return null;
        }

        protected void undo()
        {
        }
    }

    ////////////////////////////////////////////////////////////////
    // Deletion.

    /**
     * Delete the given file.
     *
     * @throw IllegalStateException if this transaction has already been
     * committed or rolled back
     * @throw FileNotFoundException if the file does not exist
     * @throw DeleteException if the deletion fails
     * @throw TransactionException if there is a problem maintaining
     * transaction information
     * @throw InconsistentStateException if this transaction cannot be restored
     * to a consistent state (either no effect or all effects); failure
     * of atomicity
     */

    public void delete(File f)
                    throws IllegalStateException, IOException, TransactionException,
                           InconsistentStateException
    {
        run(new DeleteAction(f));
    }

    /**
     * Convenience method for delete(File)
     */
    public void delete(String s)
                    throws IllegalStateException, IOException, TransactionException,
                           InconsistentStateException
    {
        delete(new File(s));
    }

    ////////////////////////////////////////////////////////////////
    // Renaming.

    //	public void rename(File f1, File f2)
    //				throws IllegalStateException, IOException, TransactionException,
    //							 InconsistentStateException {
    //		run(new RenameAction(f1, f2));
    //	}

    /**
     * Convenience method for rename(File, File)
     */
    //	public void rename(String s1, String s2)
    //				throws IllegalStateException, IOException, TransactionException,
    //							 InconsistentStateException {
    //		rename(new File(s1), new File(s2));
    //	}

    ////////////////////////////////////////////////////////////////
    private void checkActive() throws IllegalStateException
    {
        if (!active)
            throw new IllegalStateException("transaction not active");
    }

    ////////////////////////////////////////////////////////////////
    /// Finishing.

    //if a rollback happens internally, then a call to end with should
    // commit false should not be an error, it should do nothing

    public void shouldCommit(boolean b) throws IllegalStateException
    {
        checkActive();
        shouldCommit = b;
    }

    public boolean shouldCommit()
    {
        return shouldCommit;
    }

    public boolean committed()
    {
        return committed;
    }

    public synchronized void end()
                    throws TransactionException, InconsistentStateException
    {
        if (!active) // OK; std idiom with end in a finally will result in a call
            return;         // even after a forced rollback
        if (shouldCommit)
            commit();
        else
            rollback();
    }

    public List getRollbackCloseExceptions()
    {
        return Collections.unmodifiableList(rollbackCloseExceptions);
    }

    private void commit()
                    throws TransactionException, InconsistentStateException
    {
        try {
            closeAll();
        }
        catch (Exception e) {
            rollback();
            throw new TransactionException(e);
        }
        journal.commit();
        active = false;
        committed = true;
    }

    // package access because called from TM
    void rollback() throws InconsistentStateException
    {
        rollbackCloseExceptions = closeAllCatchingExceptions();
        journal.rollback();
        active = false;
        committed = false;
    }

    private void closeAll() throws IOException
    {
        Iterator i = journal.getActions().iterator();
        while (i.hasNext()) {
            Action a = (Action) i.next();
            a.close();
        }
    }

    private ArrayList closeAllCatchingExceptions()
    {
        Iterator i = journal.getActions().iterator();
        ArrayList result = new ArrayList();
        while (i.hasNext()) {
            Action a = (Action) i.next();
            try {
                a.close();
            }
            catch (IOException e) {
                result.add(e);
            }
        }
        return result;
    }

    ////////////////////////////////////////////////////////////////
    /// Testing.

    private String crashHere = null;

    public void setCrashPoint(String s)
    {
        crashHere = s.intern();
    }

    private void crashPoint(String s)
    {
        //		System.out.println("crashPoint: s = " + s + ", crashHere = " + crashHere);
        if (s == crashHere) { // OK because of intern
            System.out.println("CRASH *** " + crashHere + " ***");
            Runtime.getRuntime().halt(-1);
        }
    }

}
