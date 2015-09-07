package com.beligum.blocks.search.fields;

import java.util.Locale;

/**
 * Created by wouter on 3/09/15.
 */
public class AllFields extends Field
{
    public AllFields() {
        this.field = "_all";
        this.locale = Locale.ROOT;
    }

    @Override
    public String getField() {
        return this.field;
    }

    @Override
    public String getRawField() {
        return getField();
    }

}
