package com.beligum.blocks.fs.indexes.entries;

import org.hibernate.search.annotations.DocumentId;

import java.net.URI;

/**
 * Created by bram on 2/14/16.
 */
public abstract class AbstractIndexEntry implements IndexEntry
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    protected URI id;

    //-----CONSTRUCTORS-----
    protected AbstractIndexEntry(URI id)
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
        if (!(o instanceof AbstractIndexEntry))
            return false;

        AbstractIndexEntry that = (AbstractIndexEntry) o;

        return getId() != null ? getId().equals(that.getId()) : that.getId() == null;

    }
    @Override
    public int hashCode()
    {
        return getId() != null ? getId().hashCode() : 0;
    }
}
