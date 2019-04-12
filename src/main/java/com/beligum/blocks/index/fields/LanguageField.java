package com.beligum.blocks.index.fields;

import com.beligum.base.server.R;
import com.beligum.blocks.index.entries.AbstractIndexEntry;
import com.beligum.blocks.index.ifaces.ResourceIndexEntry;
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
    public String getValue(ResourceIndexEntry indexEntry)
    {
        return this.serialize(indexEntry.getLanguage());
    }
    @Override
    public boolean hasValue(ResourceIndexEntry indexEntry)
    {
        return ((AbstractIndexEntry)indexEntry).hasLanguage();
    }
    @Override
    public void setValue(ResourceIndexEntry indexEntry, String value)
    {
        ((AbstractIndexEntry)indexEntry).setLanguage(this.deserialize(value));
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
