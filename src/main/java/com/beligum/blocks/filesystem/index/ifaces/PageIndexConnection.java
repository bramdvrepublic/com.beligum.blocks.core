package com.beligum.blocks.filesystem.index.ifaces;

import com.beligum.blocks.filesystem.index.entries.pages.PageIndexEntry;

import java.io.IOException;
import java.net.URI;

/**
 * Created by bram on 12/04/17.
 */
public interface PageIndexConnection extends IndexConnection
{
    /**
     * This is the overridden general get() method from the super interface to be able to be more specific in it's return type.
     */
    @Override
    PageIndexEntry get(URI key) throws IOException;
}
