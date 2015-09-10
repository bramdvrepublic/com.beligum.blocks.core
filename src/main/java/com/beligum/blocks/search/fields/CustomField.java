package com.beligum.blocks.search.fields;

import com.beligum.blocks.utils.RdfTools;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by wouter on 9/09/15.
 */
public class CustomField extends AbstractField
{


    // Search root languages
    public CustomField(URI field) {
        super(field);
    }

    // Only search this locale
    public CustomField(URI field, Locale locale) {
        super(field, locale);
    }

}
