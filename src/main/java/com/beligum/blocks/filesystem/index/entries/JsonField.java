package com.beligum.blocks.filesystem.index.entries;

import com.beligum.blocks.filesystem.index.ifaces.IndexEntry;
import com.beligum.blocks.filesystem.index.ifaces.IndexEntryField;
import com.beligum.blocks.rdf.ifaces.RdfProperty;

import java.util.Objects;

public class JsonField implements IndexEntryField
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    protected String name;

    //-----CONSTRUCTORS-----
    public JsonField(String name)
    {
        this.name = name;
    }
    public JsonField(RdfProperty rdfProperty)
    {
        this.name = this.toFieldName(rdfProperty);
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getName()
    {
        return name;
    }
    @Override
    public String getValue(IndexEntry indexEntry)
    {
        return null;
    }
    @Override
    public boolean hasValue(IndexEntry indexEntry)
    {
        return false;
    }

    //-----PROTECTED METHODS-----
    /**
     * Translate the property name to the json field name.
     */
    protected String toFieldName(RdfProperty property)
    {
        return property.getCurie().toString();
    }

    //-----PRIVATE METHODS-----

    //-----MGMT METHODS-----
    @Override
    public String toString()
    {
        return name;
    }
    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof JsonField)) return false;
        JsonField that = (JsonField) o;
        return Objects.equals(getName(), that.getName());
    }
    @Override
    public int hashCode()
    {
        return Objects.hash(getName());
    }
}
