package com.beligum.blocks.search.fields;

import com.beligum.blocks.config.ParserConstants;

import java.util.Locale;

/**
 * Created by wouter on 3/09/15.
 */
public class IdField extends AbstractField
{

    public IdField()
    {
        this.field = ParserConstants.JSONLD_ID;
        this.locale = Locale.ROOT;
    }

    @Override
    public String getField() {
        return getRawField();
    }

    @Override
    public String getRawField() {
        return this.field;
    }

}
