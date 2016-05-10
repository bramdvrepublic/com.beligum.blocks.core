package com.beligum.blocks.fs.index.entries.pages;

import com.beligum.blocks.fs.index.entries.IndexEntry;
import com.beligum.blocks.rdf.ifaces.RdfClass;

import java.net.URI;
import java.util.Locale;

/**
 * Created by bram on 2/23/16.
 */
public interface PageIndexEntry extends IndexEntry
{
    //-----CONSTANTS-----
    //note: sync these with the getter names below (and the setters of the implementations)
    enum Field implements IndexEntry.IndexEntryField
    {
        resource,
        typeOf,
        language,
        canonicalAddress,
    }

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    URI getResource();
    RdfClass getTypeOf();
    Locale getLanguage();
    URI getCanonicalAddress();

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
