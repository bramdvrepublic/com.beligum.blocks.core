package com.beligum.blocks.index.fields;

import com.beligum.blocks.index.entries.AbstractIndexEntry;
import com.beligum.blocks.index.ifaces.ResourceIndexEntry;
import com.beligum.blocks.index.ifaces.ResourceProxy;

/**
 * Created by bram on Apr 12, 2019
 */
public class DescriptionField extends JsonField
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public DescriptionField()
    {
        super("description");
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getValue(ResourceProxy resourceProxy)
    {
        return resourceProxy.getDescription();
    }
    @Override
    public boolean hasValue(ResourceProxy resourceProxy)
    {
        return resourceProxy.getDescription() != null;
    }
    @Override
    public void setValue(ResourceProxy resourceProxy, String value)
    {
        if (resourceProxy instanceof AbstractIndexEntry) {
            ((AbstractIndexEntry) resourceProxy).setDescription(value);
        }
    }
    @Override
    public boolean isInternal()
    {
        return true;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
