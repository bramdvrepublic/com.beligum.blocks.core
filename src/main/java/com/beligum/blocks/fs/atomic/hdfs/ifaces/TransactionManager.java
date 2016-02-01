package com.beligum.blocks.fs.atomic.hdfs.ifaces;

import com.beligum.blocks.fs.atomic.hdfs.Transaction;
import com.beligum.blocks.fs.atomic.hdfs.exceptions.TransactionException;

import java.io.IOException;

/**
 * Created by bram on 2/1/16.
 */
public interface TransactionManager
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    Transaction beginTransaction() throws IOException;

    void rollbackAllActiveTransactions();

    java.nio.file.Path getJournalDir();

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
