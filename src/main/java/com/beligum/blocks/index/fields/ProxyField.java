package com.beligum.blocks.index.fields;

import com.beligum.blocks.index.entries.AbstractIndexEntry;
import com.beligum.blocks.index.ifaces.ResourceProxy;
import com.beligum.blocks.rdf.RdfFactory;
import com.beligum.blocks.rdf.ifaces.RdfClass;

/**
 * Created by bram on Apr 12, 2019
 */
public class ProxyField extends InternalField
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public ProxyField()
    {
        super("proxy");
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getValue(ResourceProxy resourceProxy)
    {
        //return this.serialize(resourceProxy.getTypeOf());
        return null;
    }
    @Override
    public boolean hasValue(ResourceProxy resourceProxy)
    {
        //return resourceProxy.getTypeOf() != null;
        return false;
    }
    @Override
    public void setValue(ResourceProxy resourceProxy, String value)
    {
        //        if (resourceProxy instanceof AbstractIndexEntry) {
        //            ((AbstractIndexEntry) resourceProxy).setTypeOf(this.deserialize(value));
        //        }
    }
    @Override
    public boolean isVirtual()
    {
        // this is not exactly a virtual field, but we want to control it's creation manually,
        // instead of with the automated using the getters/setters above.
        return true;
    }

//    public String serialize(RdfClass value)
//    {
//        return value == null ? null : value.toString();
//    }
//    public RdfClass deserialize(String value)
//    {
//        return value == null ? null : RdfFactory.getClass(value);
//    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
