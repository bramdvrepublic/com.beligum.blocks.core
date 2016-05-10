package com.beligum.blocks.fs.index.entries.pages;

import com.beligum.blocks.fs.index.entries.IndexEntry;
import org.apache.lucene.index.Term;

import java.net.URI;

/**
 * Created by bram on 2/14/16.
 */
public abstract class AbstractPageIndexEntry implements IndexEntry
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    protected URI id;
    protected String title;
    protected String description;
    protected URI image;

    //-----CONSTRUCTORS-----
    protected AbstractPageIndexEntry(URI id)
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
    @Override
    public URI getId()
    {
        return id;
    }
    @Override
    public String getTitle()
    {
        return title;
    }
    @Override
    public String getDescription()
    {
        return description;
    }
    @Override
    public URI getImage()
    {
        return image;
    }

    //-----PROTECTED METHODS-----
    protected void setId(URI id)
    {
        this.id = id;
    }
    protected void setTitle(String title)
    {
        this.title = title;
    }
    protected void setDescription(String description)
    {
        this.description = description;
    }
    protected void setImage(String image)
    {
        this.image = image == null ? null : URI.create(image);
    }

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----
    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (!(o instanceof AbstractPageIndexEntry))
            return false;

        AbstractPageIndexEntry that = (AbstractPageIndexEntry) o;

        return getId() != null ? getId().equals(that.getId()) : that.getId() == null;

    }
    @Override
    public int hashCode()
    {
        return getId() != null ? getId().hashCode() : 0;
    }
}
