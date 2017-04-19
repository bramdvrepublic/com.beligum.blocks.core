package com.beligum.blocks.filesystem.index.ifaces;

import com.beligum.blocks.filesystem.hdfs.TX;

import java.io.IOException;

/**
 * Created by bram on 1/26/16.
 */
public interface Indexer
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----

    /**
     * This method starts up a new, transactional session, connected to the supplied transaction.
     * Note: it's ok to pass null as the transaction object to explicitly indicate the session shouldn't be transactional,
     * throwing an exception if any method that requires a transaction would be accessed during the session.
     */
    IndexConnection connect(TX tx) throws IOException;

    /**
     * Sometimes (eg. after a serious setRollbackOnly) it may help to reboot the indexer (and have the transaction log do it's work).
     * This should basically do a shutdown() and re-initialize().
     */
    void reboot() throws IOException;

    /**
     * Permanently shutdown this indexer (on application shutdown)
     */
    void shutdown() throws IOException;

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
