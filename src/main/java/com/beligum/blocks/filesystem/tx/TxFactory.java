package com.beligum.blocks.filesystem.tx;

import com.beligum.blocks.config.StorageFactory;

/**
 * This class is basically an abstraction wrapper around StorageFactory.getCurrentRequestTx()
 * but it acts as a unified contract to create new transactions from inside the indexers.
 *
 * Created by bram on Jun 18, 2019
 */
public class TxFactory
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public TxFactory()
    {
    }

    //-----PUBLIC METHODS-----
    /**
     * Returns the current scoped transaction. Creates one if none is present, or returns null if no TX could be created.
     */
    public TX getCurrentTx()
    {
        return StorageFactory.getCurrentRequestTx();
    }
    /**
     * Returns true if the current scope has a transaction active.
     */
    public boolean hasCurrentTx()
    {
        return StorageFactory.hasCurrentRequestTx();
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
