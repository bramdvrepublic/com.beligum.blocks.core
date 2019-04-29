package com.beligum.blocks.index.fields;

import com.beligum.base.server.R;
import com.beligum.blocks.index.entries.AbstractIndexEntry;
import com.beligum.blocks.index.ifaces.ResourceIndexEntry;
import com.beligum.blocks.index.ifaces.ResourceProxy;

import java.net.URI;
import java.util.Locale;

/**
 * Created by bram on Apr 12, 2019
 */
public class ParentUriField extends InternalField
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
    public String getValue(ResourceProxy resourceProxy)
    {
        return this.serialize(resourceProxy.getParentUri());
    }
    @Override
    public boolean hasValue(ResourceProxy resourceProxy)
    {
        return resourceProxy.getParentUri() != null;
    }
    @Override
    public void setValue(ResourceProxy resourceProxy, String value)
    {
        if (resourceProxy instanceof AbstractIndexEntry) {
            ((AbstractIndexEntry) resourceProxy).setParentUri(this.deserialize(value));
        }
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
