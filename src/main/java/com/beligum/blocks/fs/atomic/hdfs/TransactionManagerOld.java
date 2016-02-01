package com.beligum.blocks.fs.atomic.hdfs;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.protocol.LayoutVersion;
import org.apache.hadoop.hdfs.server.common.Storage;
import org.apache.hadoop.hdfs.server.common.StorageErrorReporter;
import org.apache.hadoop.hdfs.server.namenode.*;
import org.apache.hadoop.hdfs.server.protocol.NamenodeCommand;

import java.io.File;
import java.io.IOException;

/**
 * A very simple journaled transaction manager, based on Hadoop edit logs
 *
 * @see org.apache.hadoop.hdfs.server.namenode.FSNamesystem
 * @see org.apache.hadoop.hdfs.server.namenode.FSImage
 * @see org.apache.hadoop.hdfs.server.namenode.FSEditLog
 * <p/>
 * Created by bram on 1/31/16.
 */
public class TransactionManagerOld
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private JournalManager journalManager;

    //-----CONSTRUCTORS-----
    public TransactionManagerOld() throws IOException
    {
        this.journalManager = new FileJournalManager(new Configuration(), new Storage.StorageDirectory(new File("/home/bram/test/journal")), new ErrorReporter());

        this.journalManager.recoverUnfinalizedSegments();
    }

    //-----PUBLIC METHODS-----
    public synchronized Transaction beginTransaction() throws IOException
    {
        final int LAYOUT_VERSION = LayoutVersion.Feature.RESERVED_REL2_4_0.getInfo().getLayoutVersion();

        long txId = 0;

        EditLogOutputStream editLog = this.journalManager.startLogSegment(txId, LAYOUT_VERSION);
        editLog.create(LAYOUT_VERSION);

        this.journalManager.doPreUpgrade();

        this.journalManager.doRollback();

        NameNode nameNode = new NameNode(new Configuration());

        NamenodeCommand blah = nameNode.getRpcServer().startCheckpoint(null);

        EditLogOutputStream editLogStream = new EditLogFileOutputStream(new Configuration(), new File("/home/bram/test/transactionId.ext"));

        return null;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----INNER CLASSES-----
    private class ErrorReporter implements StorageErrorReporter
    {
        @Override
        public void reportErrorOnFile(File f)
        {
            throw new RuntimeException("Very serious error: encountered an error in the journal storage; can't proceed, exiting; " + f);
        }
    }
}
