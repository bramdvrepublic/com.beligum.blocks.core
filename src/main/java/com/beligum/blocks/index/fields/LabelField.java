package com.beligum.blocks.index.fields;

import com.beligum.blocks.index.entries.AbstractIndexEntry;
import com.beligum.blocks.index.ifaces.ResourceIndexEntry;

import java.net.URI;

/**
 * Created by bram on Apr 12, 2019
 */
public class LabelField extends JsonField
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public LabelField()
    {
        super("label");
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getValue(ResourceIndexEntry indexEntry)
    {
        return indexEntry.getLabel();
    }
    @Override
    public boolean hasValue(ResourceIndexEntry indexEntry)
    {
        return ((AbstractIndexEntry)indexEntry).hasLabel();
    }
    @Override
    public void setValue(ResourceIndexEntry indexEntry, String value)
    {
        ((AbstractIndexEntry)indexEntry).setLabel(value);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
