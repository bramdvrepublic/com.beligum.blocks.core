package com.beligum.blocks.fs.index.entries;

/**
 * Created by bram on 2/23/16.
 */
public interface PageIndexEntry extends IndexEntry
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    String getResource();
    String getTitle();
    String getLanguage();
    String getParent();

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
