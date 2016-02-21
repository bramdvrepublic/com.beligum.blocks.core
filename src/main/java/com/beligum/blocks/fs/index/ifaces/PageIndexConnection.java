package com.beligum.blocks.fs.index.ifaces;

import com.beligum.blocks.fs.index.entries.PageIndexEntry;
import com.beligum.blocks.fs.pages.ifaces.Page;

import java.io.IOException;
import java.net.URI;

/**
 * Created by bram on 2/21/16.
 */
public interface PageIndexConnection extends IndexConnection
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    PageIndexEntry get(URI key) throws IOException;
    void delete(Page page) throws IOException;
    void indexPage(Page page) throws IOException;

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
