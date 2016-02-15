package com.beligum.blocks.fs.indexes.ifaces;

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
    void beginTransaction() throws IOException;
    void commitTransaction() throws IOException;
    void rollbackTransaction() throws IOException;

    void shutdown();

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
