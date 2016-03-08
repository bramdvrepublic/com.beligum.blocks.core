package com.beligum.blocks.fs.index.entries;

import java.io.Serializable;
import java.net.URI;

/**
 * Created by bram on 2/14/16.
 */
public interface IndexEntry extends Serializable
{
    //-----CONSTANTS-----
    interface IndexEntryField
    {
        //since all implementations are enums, this will be implemented by the default enum name() method
        String name();
    }
    enum Field implements IndexEntryField
    {
        id,
        tokenisedId
    }

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    URI getId();

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
