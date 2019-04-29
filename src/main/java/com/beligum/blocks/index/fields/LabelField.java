package com.beligum.blocks.index.fields;

import com.beligum.blocks.index.entries.AbstractIndexEntry;
import com.beligum.blocks.index.ifaces.ResourceIndexEntry;
import com.beligum.blocks.index.ifaces.ResourceProxy;

import java.net.URI;

/**
 * Created by bram on Apr 12, 2019
 */
public class LabelField extends InternalField
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
    public String getValue(ResourceProxy resourceProxy)
    {
        return resourceProxy.getLabel();
    }
    @Override
    public boolean hasValue(ResourceProxy resourceProxy)
    {
        return resourceProxy.getLabel() != null;
    }
    @Override
    public void setValue(ResourceProxy resourceProxy, String value)
    {
        if (resourceProxy instanceof AbstractIndexEntry) {
            ((AbstractIndexEntry) resourceProxy).setLabel(value);
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
