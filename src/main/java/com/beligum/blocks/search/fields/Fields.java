package com.beligum.blocks.search.fields;

import java.net.URI;
import java.util.Locale;

/**
 * Created by wouter on 9/09/15.
 */
public class Fields
{
    public static CustomField get(URI uri) {
        return new CustomField(uri);
    }

    public static CustomField get(URI uri, Locale locale) {
        return new CustomField(uri, locale);
    }

    public static JoinField join(URI uri) {
        return new JoinField(uri);
    }

    public static JoinField join(URI uri, Locale locale) {
        return new JoinField(uri, locale);
    }

    public static IdField id() {
        return new IdField();
    }

    public static TypeField type() {
        return new TypeField();
    }

    public static AllFields all() {
        return new AllFields();
    }


}
