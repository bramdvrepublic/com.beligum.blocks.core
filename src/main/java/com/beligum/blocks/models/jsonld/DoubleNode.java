package com.beligum.blocks.models.jsonld;

import java.io.StringWriter;

/**
 * Created by wouter on 23/04/15.
 */
public class DoubleNode extends BlankNode
{
    private Double internalObject;

    public DoubleNode(Double value) {
        this.internalObject = value;
    }


    @Override
    public boolean isDouble()
    {
        return true;
    }

    @Override
    public boolean isNull()
    {
        return this.internalObject == null;
    }

    @Override
    public Double getDouble()
    {
        return internalObject;
    }

    @Override
    public String getString()
    {
        return internalObject.toString();
    }

    public void write(StringWriter writer, boolean expanded) {
        writer.append(Double.toString(this.internalObject));
    }

    public Node copy() {
        return new DoubleNode(internalObject);
    }
}
