package com.beligum.blocks.fs.index.ifaces;

import java.io.IOException;

/**
 * Created by bram on 2/21/16.
 */
public interface IndexConnection extends AutoCloseable
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    void commit() throws IOException;
    void rollback() throws IOException;

//    B getNewQueryBuilder() throws IOException;
//    R executeQuery(Q query) throws IOException;

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
