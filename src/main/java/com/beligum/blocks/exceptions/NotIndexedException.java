package com.beligum.blocks.exceptions;

import java.io.IOException;
import java.net.URI;

/**
 * Because some index values depend on others to be indexed first,
 * we need a way to signal (eg. the bulk re-indexer) that the error happened
 * is because it needs to index another resource first.
 *
 * Created by bram on 8/29/16.
 */
public class NotIndexedException extends IOException
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private URI resourceBeingIndexed;
    private URI resourceNeedingIndexation;

    //-----CONSTRUCTORS-----
    public NotIndexedException(URI resourceBeingIndexed, URI resourceNeedingIndexation, String message)
    {
        super(message);

        this.resourceBeingIndexed = resourceBeingIndexed;
        this.resourceNeedingIndexation = resourceNeedingIndexation;
    }

    //-----PUBLIC METHODS-----
    public URI getResourceBeingIndexed()
    {
        return resourceBeingIndexed;
    }
    public URI getResourceNeedingIndexation()
    {
        return resourceNeedingIndexation;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
