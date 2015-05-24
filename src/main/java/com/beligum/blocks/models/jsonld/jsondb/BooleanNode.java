package com.beligum.blocks.models.jsonld.jsondb;

import com.beligum.blocks.models.jsonld.interfaces.Node;

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
    public String asString()
    {
        return internalObject.toString();
    }

    public Node copy() {
        return new BooleanNode(internalObject);
    }
}
