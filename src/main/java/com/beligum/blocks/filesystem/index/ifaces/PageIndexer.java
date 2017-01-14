package com.beligum.blocks.filesystem.index.ifaces;

import java.io.IOException;

/**
 * Generic superclass for the page indexer
 *
 * Created by bram on 1/26/16.
 */
public interface PageIndexer extends Indexer
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    PageIndexConnection connect() throws IOException;

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
