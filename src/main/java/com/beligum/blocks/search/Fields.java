package com.beligum.blocks.search;

import java.net.URI;

/**
 * Created by wouter on 26/08/15.
 */
public class Fields
{
    public static FieldBuilder field(String field) {
        return new FieldBuilder(field);
    }

    public static FieldBuilder field(URI field) {
        return new FieldBuilder(field);
    }
}
