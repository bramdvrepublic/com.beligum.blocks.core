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
        tokenisedId,
        title,
        description,
        image,
    }

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    URI getId();

    /**
     * The title of this resource, to be used directly in the HTML that is returned to the client.
     * So, in the right language and format. Mainly used to build eg. search result lists.
     * Try not to return null or "".
     */
    String getTitle();

    /**
     * The description of this resource, to be used directly in the HTML that is returned to the client.
     * So, in the right language and format. Mainly used to build eg. search result lists.
     * Might be empty or null.
     */
    String getDescription();

    /**
     * A link to the main image that describes this resource, mainly used to build eg. search result lists.
     * Might be null.
     */
    URI getImage();

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
