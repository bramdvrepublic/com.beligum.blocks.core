package com.beligum.blocks.filesystem.index.solr;

import com.beligum.blocks.filesystem.index.entries.pages.JsonPageIndexEntry;
import com.beligum.blocks.filesystem.pages.ifaces.Page;

import java.io.IOException;

public class SolrPageIndexEntry extends JsonPageIndexEntry
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    /**
     * Package-private constructor: only for serialization and to call getInternalFields() during initialization
     */
    SolrPageIndexEntry()
    {
        super();
    }
    public SolrPageIndexEntry(Page page) throws IOException
    {
        super(page);
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
