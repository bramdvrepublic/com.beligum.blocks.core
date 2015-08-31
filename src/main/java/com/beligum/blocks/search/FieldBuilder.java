package com.beligum.blocks.search;

import java.net.URI;
import java.util.ArrayList;

/**
 * Created by wouter on 26/08/15.
 */
public class FieldBuilder
{
    ArrayList<String> fields = new ArrayList<String>();

    protected FieldBuilder(String field) {
        fields.add(field);
    }

    protected FieldBuilder(URI field) {
        fields.add(field.toString());
    }

    public FieldBuilder join(String field) {
        fields.add(field);
        return this;
    }

    public FieldBuilder join(URI field) {
        fields.add(field.toString());
        return this;
    }

    public String execute() {
        return null;
    }

}
