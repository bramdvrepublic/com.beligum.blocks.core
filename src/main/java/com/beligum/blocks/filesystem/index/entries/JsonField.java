package com.beligum.blocks.filesystem.index.entries;

import com.beligum.blocks.filesystem.index.ifaces.IndexEntry;
import com.beligum.blocks.filesystem.index.ifaces.IndexEntryField;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import org.eclipse.rdf4j.model.Value;

import java.io.IOException;
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
    @Override
    public void setValue(IndexEntry indexEntry, String value)
    {
        //NOOP, override
    }

    //-----PROTECTED METHODS-----
    /**
     * Translate the property name to the json field name.
     */
    protected String toFieldName(RdfProperty property)
    {
        return property.getCurie().toString();
    }
    @Override
    public String serialize(RdfProperty predicate, Value rdfValue) throws IOException
    {
        //this is a very default and generic implementation, subclass for more flexibility
        return rdfValue == null ? null : rdfValue.stringValue();
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
