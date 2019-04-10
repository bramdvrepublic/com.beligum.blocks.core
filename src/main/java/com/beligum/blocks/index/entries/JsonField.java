package com.beligum.blocks.index.entries;

import com.beligum.blocks.index.ifaces.ResourceIndexEntry;
import com.beligum.blocks.index.ifaces.IndexEntryField;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import org.eclipse.rdf4j.model.Value;

import java.io.IOException;
import java.util.Locale;
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
    public String getValue(ResourceIndexEntry indexEntry)
    {
        return null;
    }
    @Override
    public boolean hasValue(ResourceIndexEntry indexEntry)
    {
        return false;
    }
    @Override
    public void setValue(ResourceIndexEntry indexEntry, String value)
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
    public String serialize(Value rdfValue, RdfProperty predicate, Locale language) throws IOException
    {
        //this is a very default and generic implementation, subclass for more flexibility
        return rdfValue == null ? null : rdfValue.stringValue();
    }

    //-----PRIVATE METHODS-----

    //-----MGMT METHODS-----
    @Override
    public String toString()
    {
        return this.getName();
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
