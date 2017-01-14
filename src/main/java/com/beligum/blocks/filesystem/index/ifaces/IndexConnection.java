package com.beligum.blocks.filesystem.index.ifaces;

import com.beligum.blocks.filesystem.index.entries.IndexEntry;

import javax.transaction.xa.XAResource;
import java.io.IOException;

/**
 * Created by bram on 2/21/16.
 */
public interface IndexConnection<T extends IndexEntry> extends XAResource
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----

    /**
     * Free all resources held by this connection. Called at the very end of each request transaction.
     * Note that we explicitly don't implement AutoClosable because we don't want to hint users to close
     * the connections themself.
     */
    void close() throws IOException;

//    B getNewQueryBuilder() throws IOException;
//    R executeQuery(Q query) throws IOException;

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
