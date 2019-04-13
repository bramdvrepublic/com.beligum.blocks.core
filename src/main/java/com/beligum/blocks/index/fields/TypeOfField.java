package com.beligum.blocks.index.fields;

import com.beligum.base.utils.toolkit.StringFunctions;
import com.beligum.blocks.index.entries.AbstractIndexEntry;
import com.beligum.blocks.index.ifaces.ResourceIndexEntry;
import com.beligum.blocks.index.ifaces.ResourceProxy;
import com.beligum.blocks.rdf.RdfFactory;
import com.beligum.blocks.rdf.ifaces.RdfClass;

import java.net.URI;

/**
 * Created by bram on Apr 12, 2019
 */
public class TypeOfField extends JsonField
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public TypeOfField()
    {
        super("typeOf");
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getValue(ResourceProxy resourceProxy)
    {
        return this.serialize(resourceProxy.getTypeOf());
    }
    @Override
    public boolean hasValue(ResourceProxy resourceProxy)
    {
        return resourceProxy.getTypeOf() != null;
    }
    @Override
    public void setValue(ResourceProxy resourceProxy, String value)
    {
        if (resourceProxy instanceof AbstractIndexEntry) {
            ((AbstractIndexEntry) resourceProxy).setTypeOf(this.deserialize(value));
        }
    }

    public String serialize(RdfClass value)
    {
        return value == null ? null : value.toString();
    }
    public RdfClass deserialize(String value)
    {
        return value == null ? null : RdfFactory.getClass(value);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
