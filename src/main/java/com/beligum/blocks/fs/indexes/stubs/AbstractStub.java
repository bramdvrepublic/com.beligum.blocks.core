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
    //Note that eg. infinispan doesn't use this annotation (but does use the method as the key in the cache)
    @DocumentId
    public URI getId()
    {
        return id;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----
    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (!(o instanceof AbstractStub))
            return false;

        AbstractStub that = (AbstractStub) o;

        return getId() != null ? getId().equals(that.getId()) : that.getId() == null;

    }
    @Override
    public int hashCode()
    {
        return getId() != null ? getId().hashCode() : 0;
    }
}
