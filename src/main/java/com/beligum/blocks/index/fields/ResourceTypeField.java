package com.beligum.blocks.index.fields;

import com.beligum.blocks.index.entries.AbstractIndexEntry;
import com.beligum.blocks.index.ifaces.ResourceIndexEntry;
import com.beligum.blocks.index.ifaces.ResourceProxy;
import com.beligum.blocks.rdf.RdfFactory;
import com.beligum.blocks.rdf.ifaces.RdfClass;

/**
 * Created by bram on Apr 12, 2019
 */
public class ResourceTypeField extends InternalField
{
    //-----CONSTANTS-----
    public final String DEFAULT_VALUE;
    public final String SUB_VALUE;
    public final String PROXY_VALUE;

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public ResourceTypeField()
    {
        super("resourceType");

        DEFAULT_VALUE = this.serialize(ResourceIndexEntry.Type.DEFAULT);
        SUB_VALUE = this.serialize(ResourceIndexEntry.Type.SUB);
        PROXY_VALUE = this.serialize(ResourceIndexEntry.Type.PROXY);
    }

    //-----PUBLIC METHODS-----
//    @Override
//    public String getValue(ResourceProxy resourceProxy)
//    {
//        return this.serialize(resourceProxy.getTypeOf());
//    }
//    @Override
//    public boolean hasValue(ResourceProxy resourceProxy)
//    {
//        return resourceProxy.getTypeOf() != null;
//    }
//    @Override
//    public boolean isVirtual()
//    {
//        return true;
//    }
    @Override
    public void setValue(ResourceProxy resourceProxy, String value)
    {
        if (resourceProxy instanceof AbstractIndexEntry) {
            ((AbstractIndexEntry) resourceProxy).setResourceType(this.deserialize(value));
        }
    }

    public String serialize(ResourceIndexEntry.Type value)
    {
        return value == null ? null : value.toString();
    }
    public ResourceIndexEntry.Type deserialize(String value)
    {
        return value == null ? null : ResourceIndexEntry.Type.valueOf(value);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
