package com.beligum.blocks.fs.index.entries;

import org.apache.lucene.index.Term;

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

    //-----STATIC METHODS-----
    public static Term toLuceneId(URI id)
    {
        return new Term(Field.id.name(), id.toString());
    }
    public static Term toLuceneId(IndexEntry indexEntry)
    {
        return new Term(Field.id.name(), indexEntry.getId().toString());
    }

    //-----PUBLIC METHODS-----
    public URI getId()
    {
        return id;
    }
    public void setId(URI id)
    {
        this.id = id;
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
