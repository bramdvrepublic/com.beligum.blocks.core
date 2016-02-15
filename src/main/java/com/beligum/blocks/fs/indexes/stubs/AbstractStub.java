package com.beligum.blocks.fs.indexes.stubs;

import org.hibernate.search.annotations.DocumentId;

import java.net.URI;

/**
 * Created by bram on 2/14/16.
 */
public abstract class AbstractStub implements Stub
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private URI id;

    //-----CONSTRUCTORS-----
    protected AbstractStub(URI id)
    {
        this.id = id;
    }

    //-----PUBLIC METHODS-----
    @DocumentId
    public URI getId()
    {
        return id;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
