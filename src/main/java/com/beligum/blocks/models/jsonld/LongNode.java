package com.beligum.blocks.models.jsonld;

import java.io.StringWriter;

/**
 * Created by wouter on 23/04/15.
 */
public class LongNode extends BlankNode
{
    private Long internalObject;

    public LongNode(Long value) {
        this.internalObject = value;
    }



    @Override
    public boolean isLong()
    {
        return true;
    }

    @Override
    public Long getLong()
    {
        return internalObject;
    }

    @Override
    public boolean isNull()
    {
        return this.internalObject == null;
    }

    @Override
    public String getString()
    {
        return internalObject.toString();
    }

    public void write(StringWriter writer, boolean expanded) {
        writer.append(Long.toString(this.internalObject));
    }

    public Node copy() {
        return new LongNode(internalObject);
    }
}
