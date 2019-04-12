package com.beligum.blocks.index.fields;

import com.beligum.base.server.R;
import com.beligum.blocks.index.entries.AbstractIndexEntry;
import com.beligum.blocks.index.ifaces.ResourceIndexEntry;

import java.net.URI;
import java.util.Locale;

/**
 * Created by bram on Apr 12, 2019
 */
public class ParentUriField extends JsonField
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public ParentUriField()
    {
        super("parentUri");
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getValue(ResourceIndexEntry indexEntry)
    {
        return this.serialize(indexEntry.getParentUri());
    }
    @Override
    public boolean hasValue(ResourceIndexEntry indexEntry)
    {
        return ((AbstractIndexEntry)indexEntry).hasParentUri();
    }
    @Override
    public void setValue(ResourceIndexEntry indexEntry, String value)
    {
        ((AbstractIndexEntry)indexEntry).setParentUri(this.deserialize(value));
    }

    public URI create(ResourceIndexEntry parent)
    {
        return parent == null ? null : parent.getUri();
    }
    public String serialize(URI value)
    {
        return this.serializeUri(value);
    }
    public URI deserialize(String value)
    {
        return this.deserializeUri(value);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
