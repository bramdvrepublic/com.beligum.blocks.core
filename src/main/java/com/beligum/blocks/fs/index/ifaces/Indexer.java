package com.beligum.blocks.fs.index.ifaces;

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
     * This method starts up a new, transactional session
     * @return
     * @throws IOException
     */
    IndexConnection connect() throws IOException;

    void shutdown() throws IOException;

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
