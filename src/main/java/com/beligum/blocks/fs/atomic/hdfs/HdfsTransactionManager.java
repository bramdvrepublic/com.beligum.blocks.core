package com.beligum.blocks.fs.atomic.hdfs;

import com.beligum.blocks.fs.atomic.hdfs.exceptions.TransactionException;
import com.beligum.blocks.fs.atomic.hdfs.ifaces.TransactionManager;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.hdfs.qjournal.server.Journal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This is a very simple implementation of a HDFS transaction manager to provide atomic actions.
 * Started out with this code:
 *   http://archive.oreilly.com/pub/a/onjava/2001/11/07/atomic.html?page=2
 *   (also found here: https://github.com/khaliqgaffar/pci_dss_ext_usb/tree/master/cryptoService/src/main/java/com/astrel/io/atomic)
 * and changed it to work with our HDFS implementation, re-using as much Hadoop NameNode code as possible.
 *
 * Don't know how good it it, but it seems to do the job reasonably well...
 *
 * Created by bram on 2/1/16.
 */
public class HdfsTransactionManager implements TransactionManager
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private int nextTxNumber;
    private List<Transaction> transactions;
    private java.nio.file.Path journalDir;

    //-----CONSTRUCTORS-----
    public HdfsTransactionManager() throws IOException
    {
        this.nextTxNumber = 0;
        this.transactions = new ArrayList<>();
        this.journalDir = Paths.get("/home/bram/test/");

        this.initJournalDir(this.journalDir);
        this.recover();
    }

    //-----PUBLIC METHODS-----
    @Override
    public synchronized Transaction beginTransaction() throws IOException
    {
        Transaction retVal = new Transaction(this, nextTxNumber++);
        transactions.add(retVal);

        return retVal;
    }
    @Override
    public synchronized void rollbackAllActiveTransactions()
    {
        Iterator i = transactions.iterator();
        while (i.hasNext()) {
            Transaction t = (Transaction) i.next();
            if (t.isActive())
                t.rollback();
        }
    }
    @Override
    public java.nio.file.Path getJournalDir()
    {
        return this.journalDir;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private void recover()
    {
        RemoteIterator<LocatedFileStatus> files = this.fileContext.util().listFiles(this.journalDir, false);
        while (files.hasNext()) {
            Path file = files.next().getPath();
            if (file.getName().startsWith("journal-")) {
                Journal.recover(this.fileContext, file);
            }
        }
    }
    /**
     * Check if we can create and rename a file on the file system
     * @throws IOException
     * @param journalDir
     */
    private void initJournalDir(java.nio.file.Path journalDir) throws IOException
    {
        java.nio.file.Path t1 = null;
        java.nio.file.Path t2 = null;

        try {
            Files.createDirectories(journalDir);

            t1 = Files.createTempFile(journalDir, "checkRename", ".tmp");
            t2 = Files.createTempFile(journalDir, "checkRename", ".tmp");

            Files.move(t1, t2);
        }
        finally {
            Exception caughtEx = null;
            try {
                if (t1!=null) {
                    Files.deleteIfExists(t1);
                }
            }
            catch (Exception e) {
                caughtEx = e;
            }
            try {
                if (t2!=null) {
                    Files.deleteIfExists(t2);
                }
            }
            catch (Exception e) {
                caughtEx = e;
            }

            if (caughtEx!=null) {
                throw new IOException("Error while deleting the test-rename files in the journal dir; this shouldn't happen", caughtEx);
            }
        }
    }
}
