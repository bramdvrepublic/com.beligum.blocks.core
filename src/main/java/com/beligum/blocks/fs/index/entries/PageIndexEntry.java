package com.beligum.blocks.fs.index.entries;

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
        title,
        typeOf,
        language,
        parent
    }

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    String getResource();
    String getTypeOf();
    String getTitle();
    String getLanguage();
    String getParent();

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
