package com.beligum.blocks.fs.atomic.manual;

import com.beligum.blocks.fs.HdfsUtils;
import com.beligum.blocks.fs.atomic.manual.exceptions.InconsistentStateException;
import com.beligum.blocks.fs.atomic.manual.exceptions.TransactionException;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.fs.permission.FsPermission;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This is a very simple implementation of a HDFS transaction manager to provide atomic actions.
 * Started out with this code:
 *   http://archive.oreilly.com/pub/a/onjava/2001/11/07/atomic.html?page=2
 *   (also found here: https://github.com/khaliqgaffar/pci_dss_ext_usb/tree/master/cryptoService/src/main/java/com/astrel/io/atomic)
 * and changed it to work with our HDFS implementation.
 *
 * Don't know how good it it, but it seems to do the job reasonably well...
 *
 * -----------------------------------------------------------------------------------
 * This does atomicity, but not isolation: it will generally be wrong
 * if two overlapping transactions work on the same file.
 *
 * It is a grievous error to have two different transaction mgrs
 * using the same directory.
 */
public class TransactionManager
{
    ////////////////////////////////
    private FileContext fileContext;
    private Path journalDir;
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
    public TransactionManager(FileContext fileContext, Path journalDir) throws IOException, InconsistentStateException
    {
        this.fileContext = fileContext;
        this.journalDir = journalDir;

        this.fileContext.mkdir(this.journalDir, FsPermission.getDirDefault(), true);
        
        checkCanRenameFile();
        nextTxnNumber = 0;
        transactions = new ArrayList();
        String ext = System.getProperty("com.beligum.blocks.fs.atomic.extension");
        setFileExtension(ext);

        recover();
    }

    /**
     * Check if we can create and rename a file on the file system
     * @throws IOException
     */
    private void checkCanRenameFile() throws IOException
    {
        Path t1 = null;
        Path t2 = null;

        try {
            t1 = HdfsUtils.createTempFile(this.fileContext, "checkRename", "", journalDir);
            t2 = HdfsUtils.createTempFile(this.fileContext, "checkRename", "", journalDir);
            this.fileContext.rename(t1, t2);
        }
        finally {
            try {
                if (t1!=null) {
                    this.fileContext.delete(t1, false);
                }
            }
            catch (Exception e){}
            try {
                if (t2!=null) {
                    this.fileContext.delete(t2, false);
                }
            }
            catch (Exception e){}
        }
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

    public Path getJournalDir()
    {
        return journalDir;
    }

    public boolean renameCanDelete()
    {
        return renameCanDelete;
    }
    public FileContext getFileContext()
    {
        return fileContext;
    }

    ////////////////////////////////////////////////////////////////
    /// Transactions.

    public synchronized Transaction beginTransaction() throws TransactionException
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

    private void recover() throws InconsistentStateException, IOException
    {
        RemoteIterator<LocatedFileStatus> files = this.fileContext.util().listFiles(this.journalDir, false);
        while (files.hasNext()) {
            Path file = files.next().getPath();
            if (file.getName().startsWith("journal-")) {
                Journal.recover(this.fileContext, file);
            }
        }
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
