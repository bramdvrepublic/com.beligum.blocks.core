package com.beligum.blocks.search.fields;

import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.utils.RdfTools;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by wouter on 1/09/15.
 */
public abstract class AbstractField implements Field
{
    protected String field;
    protected Locale locale;

    protected AbstractField() {

    }

    public AbstractField(URI field, Locale locale) {
        this.field = RdfTools.makeDbFieldFromUri(field);
        this.locale = locale;
    }

    public AbstractField(URI field) {
        this.field = RdfTools.makeDbFieldFromUri(field);
        this.locale = Locale.ROOT;
    }

    @Override
    public String getField() {
        String localizedField = this.field;

        if (!this.locale.equals(Locale.ROOT)) {
            localizedField = localizedField + ParserConstants.LOCALIZED_PROPERTY + "." + locale.getLanguage() + "." + ParserConstants.JSONLD_VALUE;
        } else {
            localizedField = localizedField + "." + ParserConstants.JSONLD_VALUE;
        }

        return localizedField;

    }

    @Override
    public String getRawField() {

        return getField()  + "." + ParserConstants.JSONLD_RAW;
    }

    @Override
    public String getRootFieldName()
    {
        return this.field;
    }




}
