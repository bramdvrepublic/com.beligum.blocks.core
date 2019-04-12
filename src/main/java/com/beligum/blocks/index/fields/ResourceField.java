package com.beligum.blocks.index.fields;

import com.beligum.base.utils.toolkit.StringFunctions;
import com.beligum.blocks.index.entries.AbstractIndexEntry;
import com.beligum.blocks.index.ifaces.ResourceIndexEntry;

import java.net.URI;

/**
 * Created by bram on Apr 12, 2019
 */
public class ResourceField extends JsonField
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public ResourceField()
    {
        super("resource");
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getValue(ResourceIndexEntry indexEntry)
    {
        return indexEntry.getResource();
    }
    @Override
    public boolean hasValue(ResourceIndexEntry indexEntry)
    {
        return ((AbstractIndexEntry)indexEntry).hasResource();
    }
    @Override
    public void setValue(ResourceIndexEntry indexEntry, String value)
    {
        ((AbstractIndexEntry)indexEntry).setResource(value);
    }

    public String create(URI rootResourceUri)
    {
        return this.serializeUri(rootResourceUri);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
