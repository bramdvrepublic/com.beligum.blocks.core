package com.beligum.blocks.models.jsonld.jsondb;

import com.beligum.blocks.models.jsonld.interfaces.Node;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;

/**
 * Created by wouter on 23/04/15.
 */
public class BlankNode implements Node
{


    public BlankNode()
    {}

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
    public boolean isIterable()
    {
        return false;
    }
    @Override
    public boolean isNull()
    {
        return true;
    }

    @Override
    public ArrayList<Node> getIterable()
    {
        return null;
    }
    @Override
    public String toString()
    {
        return this.asString();
    }

    @Override
    public String asString()
    {
        return "";
    }
    @Override
    public Locale getLanguage()
    {
        return null;
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
    public Object getValue()
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
