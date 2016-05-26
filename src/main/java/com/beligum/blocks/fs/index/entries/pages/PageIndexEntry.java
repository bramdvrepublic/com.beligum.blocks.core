package com.beligum.blocks.fs.index.entries.pages;

import com.beligum.blocks.fs.index.entries.IndexEntry;

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
        object,
    }

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    String getResource();
    String getTypeOf();
    String getLanguage();
    String getCanonicalAddress();

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
