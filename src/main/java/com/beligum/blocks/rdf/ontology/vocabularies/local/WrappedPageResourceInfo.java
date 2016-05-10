package com.beligum.blocks.rdf.ontology.vocabularies.local;

import com.beligum.blocks.endpoints.ifaces.ResourceInfo;
import com.beligum.blocks.fs.index.entries.pages.PageIndexEntry;

import java.net.URI;
import java.util.Locale;

/**
 * Created by bram on 3/12/16.
 */
public class WrappedPageResourceInfo implements ResourceInfo
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private PageIndexEntry indexEntry;

    //-----CONSTRUCTORS-----
    //For json deserialization
    private WrappedPageResourceInfo()
    {
        this(null);
    }
    public WrappedPageResourceInfo(PageIndexEntry indexEntry)
    {
        this.indexEntry = indexEntry;
    }

    //-----PUBLIC METHODS-----
    @Override
    public URI getResourceUri()
    {
        return this.indexEntry == null ? null : this.indexEntry.getResource();
    }
    @Override
    public URI getResourceType()
    {
        if (this.indexEntry != null && this.indexEntry.getTypeOf() != null) {
            return this.indexEntry.getTypeOf().getCurieName();
        }
        else {
            return null;
        }
    }
    @Override
    public String getLabel()
    {
        return this.indexEntry == null ? null : this.indexEntry.getTitle();
    }
    @Override
    public URI getLink()
    {
        //note: the ID of a page is the public URL
        return this.indexEntry == null ? null : this.indexEntry.getId();
    }
    @Override
    public boolean isExternalLink()
    {
        //local resources aren't external
        return false;
    }
    @Override
    public URI getImage()
    {
        return this.indexEntry == null ? null : this.indexEntry.getImage();
    }
    @Override
    public String getName()
    {
        return this.indexEntry == null ? null : this.indexEntry.getTitle();
    }
    @Override
    public Locale getLanguage()
    {
        return this.indexEntry == null ? null : this.indexEntry.getLanguage();
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
