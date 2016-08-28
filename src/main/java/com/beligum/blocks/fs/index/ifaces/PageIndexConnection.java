package com.beligum.blocks.fs.index.ifaces;

import com.beligum.blocks.fs.index.entries.pages.PageIndexEntry;
import com.beligum.blocks.fs.pages.ifaces.Page;

import java.io.IOException;
import java.net.URI;

/**
 * Created by bram on 2/21/16.
 */
public interface PageIndexConnection<T extends PageIndexEntry> extends IndexConnection<T>
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    T get(URI key) throws IOException;
    void delete(Page page) throws IOException;
    void update(Page page) throws IOException;
    void deleteAll() throws IOException;

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
