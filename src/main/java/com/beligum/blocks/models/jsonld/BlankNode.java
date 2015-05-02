package com.beligum.blocks.models.jsonld;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by wouter on 23/04/15.
 */
public class BlankNode implements Node
{

    public BlankNode() {

    }

    @Override
    public boolean isString()
    {
        return false;
    }
    @Override
    public boolean isDouble()
    {
        return false;
    }
    @Override
    public boolean isLong()
    {
        return false;
    }
    @Override
    public boolean isBoolean()
    {
        return false;
    }
    @Override
    public boolean isInt()
    {
        return false;
    }
    @Override
    public boolean isList()
    {
        return false;
    }
    @Override
    public boolean isNull()
    {
        return true;
    }
    @Override
    public LinkedHashMap<String, Node> getMap()
    {
        return null;
    }
    @Override
    public ArrayList<Node> getList()
    {
        return null;
    }
    @Override
    public String toString()
    {
        return this.getString();
    }

    @Override
    public String getString()
    {
        return "";
    }

    @Override
    public Double getDouble()
    {
        return null;
    }
    @Override
    public Integer getInteger()
    {
        return null;
    }
    @Override
    public Boolean getBoolean()
    {
        return false;
    }
    @Override
    public Long getLong()
    {
        return null;
    }
    @Override
    public boolean isResource()
    {
        return false;
    }

    @Override
    public Node copy()
    {
        return new BlankNode();
    }

}
