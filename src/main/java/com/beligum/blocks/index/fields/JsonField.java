package com.beligum.blocks.index.fields;

import com.beligum.blocks.index.ifaces.IndexEntryField;
import com.beligum.blocks.index.ifaces.ResourceProxy;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.utils.RdfTools;
import org.eclipse.rdf4j.model.Value;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Objects;

public class JsonField implements IndexEntryField
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    protected String name;
    private RdfProperty rdfProperty;

    //-----CONSTRUCTORS-----
    public JsonField(String name)
    {
        this.name = name;
        this.rdfProperty = null;
    }
    public JsonField(RdfProperty rdfProperty)
    {
        this.name = this.toFieldName(rdfProperty);
        this.rdfProperty = rdfProperty;
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getName()
    {
        return name;
    }
    @Override
    public String getValue(ResourceProxy resourceProxy)
    {
        return null;
    }
    @Override
    public boolean hasValue(ResourceProxy resourceProxy)
    {
        return false;
    }
    @Override
    public void setValue(ResourceProxy resourceProxy, String value)
    {
        //NOOP, override
    }
    @Override
    public boolean isInternal()
    {
        return this.rdfProperty == null;
    }
    @Override
    public boolean isVirtual()
    {
        return false;
    }
    @Override
    public String serialize(Value rdfValue, Locale language) throws IOException
    {
        //this is a very default and generic implementation, subclass for more flexibility
        return rdfValue == null ? null : rdfValue.stringValue();
    }
    public RdfProperty getRdfProperty()
    {
        return rdfProperty;
    }
    public JsonProxyField getProxyField()
    {
        return new JsonProxyField(this);
    }

    //-----PROTECTED METHODS-----
    /**
     * Translate the property name to the json field name.
     */
    protected String toFieldName(RdfProperty property)
    {
        return property.getCurie().toString();
    }
    protected String serializeUri(URI value)
    {
        if (value != null) {
            //TODO check this: it used to be implemented with StringFunctions.getRightOfDomain(id)
            return RdfTools.relativizeToLocalDomain(value).toString();
        }
        else {
            return null;
        }
    }
    protected URI deserializeUri(String value)
    {
        return value == null ? null : URI.create(value);
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
