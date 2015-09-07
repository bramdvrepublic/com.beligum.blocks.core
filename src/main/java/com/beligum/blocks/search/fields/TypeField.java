package com.beligum.blocks.search.fields;

import com.beligum.blocks.config.ParserConstants;

import java.util.Locale;

/**
 * Created by wouter on 3/09/15.
 */
public class TypeField extends Field
{

    public TypeField()
    {
        this.field = ParserConstants.JSONLD_TYPE;
        this.locale = Locale.ROOT;
    }

    @Override
    public String getField() {
        return this.getRawField();
    }

    @Override
    public String getRawField() {
        return this.field;
    }
}
