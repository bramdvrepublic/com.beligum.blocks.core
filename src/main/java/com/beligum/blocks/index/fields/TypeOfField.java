package com.beligum.blocks.index.fields;

import com.beligum.base.utils.toolkit.StringFunctions;
import com.beligum.blocks.index.entries.AbstractIndexEntry;
import com.beligum.blocks.index.ifaces.ResourceIndexEntry;
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
    public String getValue(ResourceIndexEntry indexEntry)
    {
        return this.serialize(indexEntry.getTypeOf());
    }
    @Override
    public boolean hasValue(ResourceIndexEntry indexEntry)
    {
        return ((AbstractIndexEntry)indexEntry).hasTypeOf();
    }
    @Override
    public void setValue(ResourceIndexEntry indexEntry, String value)
    {
        ((AbstractIndexEntry)indexEntry).setTypeOf(this.deserialize(value));
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
