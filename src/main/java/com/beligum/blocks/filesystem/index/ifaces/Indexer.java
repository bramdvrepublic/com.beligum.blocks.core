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
     * Shorthand for the same method below, but attached to the default request-scoped transaction
     */
    IndexConnection connect() throws IOException;

    /**
     * This method starts up a new, transactional session, connected to the supplied transaction
     */
    IndexConnection connect(TX tx) throws IOException;

    /**
     * Permanently shutdown this indexer (on application shutdown)
     */
    void shutdown() throws IOException;

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
