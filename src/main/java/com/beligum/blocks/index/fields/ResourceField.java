package com.beligum.blocks.index.fields;

import com.beligum.base.utils.toolkit.StringFunctions;
import com.beligum.blocks.index.entries.AbstractIndexEntry;
import com.beligum.blocks.index.ifaces.ResourceIndexEntry;
import com.beligum.blocks.index.ifaces.ResourceProxy;

import java.net.URI;

/**
 * Created by bram on Apr 12, 2019
 */
public class ResourceField extends InternalField
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
    public String getValue(ResourceProxy resourceProxy)
    {
        return resourceProxy.getResource();
    }
    @Override
    public boolean hasValue(ResourceProxy resourceProxy)
    {
        return resourceProxy.getResource() != null;
    }
    @Override
    public void setValue(ResourceProxy resourceProxy, String value)
    {
        if (resourceProxy instanceof AbstractIndexEntry) {
            ((AbstractIndexEntry) resourceProxy).setResource(value);
        }
    }

    public String create(URI rootResourceUri)
    {
        return this.serializeUri(rootResourceUri);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
