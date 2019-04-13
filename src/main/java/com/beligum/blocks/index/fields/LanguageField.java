package com.beligum.blocks.index.fields;

import com.beligum.base.server.R;
import com.beligum.blocks.index.entries.AbstractIndexEntry;
import com.beligum.blocks.index.ifaces.ResourceIndexEntry;
import com.beligum.blocks.index.ifaces.ResourceProxy;
import com.beligum.blocks.rdf.RdfFactory;
import com.beligum.blocks.rdf.ifaces.RdfClass;

import java.util.Locale;

/**
 * Created by bram on Apr 12, 2019
 */
public class LanguageField extends JsonField
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public LanguageField()
    {
        super("language");
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getValue(ResourceProxy resourceProxy)
    {
        return this.serialize(resourceProxy.getLanguage());
    }
    @Override
    public boolean hasValue(ResourceProxy resourceProxy)
    {
        return resourceProxy.getLanguage() != null;
    }
    @Override
    public void setValue(ResourceProxy resourceProxy, String value)
    {
        if (resourceProxy instanceof AbstractIndexEntry) {
            ((AbstractIndexEntry) resourceProxy).setLanguage(this.deserialize(value));
        }
    }

    public String serialize(Locale value)
    {
        return value == null ? null : value.getLanguage();
    }
    public Locale deserialize(String value)
    {
        return value == null ? null : R.configuration().getLocaleForLanguage(value);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
