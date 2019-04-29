package com.beligum.blocks.index.fields;

import com.beligum.blocks.index.ifaces.IndexEntryField;
import com.beligum.blocks.index.ifaces.ResourceProxy;
import org.eclipse.rdf4j.model.Value;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

/**
 * Created by bram on Apr 13, 2019
 */
public class JsonProxyField implements IndexEntryField
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private final String name;
    private final JsonField coreField;

    //-----CONSTRUCTORS-----
    public JsonProxyField(JsonField coreField)
    {
        this.name = coreField.getName() + IndexEntryField.PROXY_FIELD_SUFFIX;
        this.coreField = coreField;
    }
    @Override
    public String getName()
    {
        return name;
    }
    @Override
    public String getValue(ResourceProxy resourceProxy)
    {
        //this is not really true, but we don't seem to use it
        return this.coreField.getValue(resourceProxy);
    }
    @Override
    public boolean hasValue(ResourceProxy resourceProxy)
    {
        //this is not really true, but we don't seem to use it
        return this.coreField.hasValue(resourceProxy);
    }
    @Override
    public void setValue(ResourceProxy resourceProxy, String value)
    {
        // this is a virtual field and can't be set, right?
    }
    @Override
    public boolean isInternal()
    {
        return this.coreField.isInternal();
    }
    @Override
    public boolean isVirtual()
    {
        //proxy fields are always virtual
        return true;
    }
    @Override
    public String serialize(Value rdfValue, Locale language) throws IOException
    {
        //this is not really true, but we don't seem to use it
        return this.coreField.serialize(rdfValue, language);
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

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
