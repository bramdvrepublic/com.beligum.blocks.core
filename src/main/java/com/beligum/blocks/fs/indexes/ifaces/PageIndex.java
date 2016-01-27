package com.beligum.blocks.fs.indexes.ifaces;

import com.beligum.blocks.fs.pages.ifaces.Page;

import java.io.IOException;

/**
 * Created by bram on 1/26/16.
 */
public interface PageIndex extends Index
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    void indexPage(Page page) throws IOException;

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
