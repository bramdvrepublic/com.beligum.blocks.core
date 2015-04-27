package com.beligum.blocks.models.jsonld;

import java.io.StringWriter;

/**
 * Created by wouter on 23/04/15.
 */
public class BooleanNode extends BlankNode
{
    private Boolean internalObject;

    public BooleanNode(boolean bool) {
        this.internalObject = bool;
    }

    @Override
    public boolean isNull()
    {
        return this.internalObject == null;
    }

    @Override
    public boolean isBoolean()
    {
        return true;
    }

    @Override
    public Boolean getBoolean()
    {
        return internalObject;
    }

    @Override
    public String getString()
    {
        return internalObject.toString();
    }

    public void write(StringWriter writer, boolean expanded) {
        writer.append(Boolean.toString(this.internalObject));
    }

    public Node copy() {
        return new BooleanNode(internalObject);
    }
}
