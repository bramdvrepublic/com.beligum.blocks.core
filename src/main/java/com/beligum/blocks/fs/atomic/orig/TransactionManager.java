package com.beligum.blocks.fs.atomic.orig;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * This does atomicity, but not isolation: it will generally be wrong
 * if two overlapping transactions work on the same file.
 * <p/>
 * It is a grievous error to have two different transaction mgrs
 * using the same directory.
 */
public class TransactionManager
{

    ////////////////////////////////

    private File journalDir;
    private int nextTxnNumber;
    private List transactions;
    private String fileExtension;
    private boolean renameCanDelete;  // Does this filesystem allow rename
    // to replace existing files?

    /**
     * The journalDir directory is where journals and other crucial
     * rollback/recovery information is stored.  This directory must
     * be unique to this TM.
     * It should not be a temp directory.
     */
    public TransactionManager(File journalDir)
                    throws IOException, InconsistentStateException
    {
        this.journalDir = journalDir.getAbsoluteFile();
        journalDir.mkdirs();
        checkRename();
        nextTxnNumber = 0;
        transactions = new ArrayList();
        String ext = System.getProperty("com.beligum.blocks.fs.atomic.extension");
        setFileExtension(ext);

        recover();
    }

    /**
     * See TransactionManager(File)
     */
    public TransactionManager(String journalDir)
                    throws IOException, InconsistentStateException
    {
        this(new File(journalDir));
    }

    private void checkRename() throws IOException
    {
        File t1 = File.createTempFile("checkRename", "", journalDir);
        File t2 = File.createTempFile("checkRename", "", journalDir);
        renameCanDelete = t1.renameTo(t2);
        t1.delete();
        t2.delete();
    }

    ////////////////////////////////////////////////////////////////
    /// Accessors.

    public String getFileExtension()
    {
        return fileExtension;
    }

    public void setFileExtension(String ext)
    {
        if (ext == null)
            fileExtension = ".abk";
        else {
            fileExtension = ext;
            if (!fileExtension.startsWith("."))
                fileExtension = "." + fileExtension;
        }
    }

    File getJournalDir()
    {
        return journalDir;
    }

    boolean renameCanDelete()
    {
        return renameCanDelete;
    }

    ////////////////////////////////////////////////////////////////
    /// Transactions.

    public synchronized Transaction beginTransaction()
                    throws TransactionException
    {
        Transaction t = new Transaction(this, nextTxnNumber);
        nextTxnNumber++;
        transactions.add(t);
        return t;
    }

    /**
     * Rollback any active transaction.
     */
    public synchronized void close() throws InconsistentStateException
    {
        Iterator i = transactions.iterator();
        while (i.hasNext()) {
            Transaction t = (Transaction) i.next();
            if (t.isActive())
                t.rollback();
        }
    }

    private void recover() throws InconsistentStateException
    {
        File[] files = journalDir.listFiles();
        for (int i = 0; i < files.length; i++)
            if (files[i].getName().startsWith("journal-"))
                Journal.recover(files[i]);
    }

    protected void finalize()
    {
        try {
            close();
        }
        catch (InconsistentStateException e) {
            System.err.println("INCONSISTENT STATE IN FINALIZE");
        }
    }
}
