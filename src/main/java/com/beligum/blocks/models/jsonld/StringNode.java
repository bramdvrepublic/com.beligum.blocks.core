package com.beligum.blocks.models.jsonld;

import java.io.StringWriter;

/**
 * Created by wouter on 23/04/15.
 */
public class StringNode extends BlankNode
{
    private String internalObject;
    private String language;

    public StringNode(String value) {
        this.internalObject = value;
    }

    public StringNode(String value, String language) {
        this.internalObject = value;
        this.language = language;
    }

    @Override
    public boolean isString()
    {
        return true;
    }

    @Override
    public boolean isNull()
    {
        return this.internalObject == null;
    }


    @Override
    public String getString()
    {
        return internalObject;
    }

    public void write(StringWriter writer, boolean expanded) {
        writer.append("\"").append(this.internalObject).append("\"");
    }

    @Override
    public Node copy()
    {
        return new StringNode(internalObject);
    }
}
