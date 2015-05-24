package com.beligum.blocks.models.jsonld.jsondb;

import com.beligum.blocks.models.jsonld.interfaces.Node;

import java.io.StringWriter;

/**
 * Created by wouter on 23/04/15.
 */
public class IntegerNode extends BlankNode
{
    private Integer internalObject;

    public IntegerNode(Integer value) {
        this.internalObject = value;
    }


    @Override
    public boolean isInt()
    {
        return true;
    }

    @Override
    public boolean isNull()
    {
        return this.internalObject == null;
    }

    @Override
    public Integer getInteger()
    {
        return internalObject;
    }

    @Override
    public String asString()
    {
        return internalObject.toString();
    }

    public void write(StringWriter writer, boolean expanded) {
        writer.append(Integer.toString(this.internalObject));
    }

    public Node copy() {
        return new IntegerNode(internalObject);
    }
}
