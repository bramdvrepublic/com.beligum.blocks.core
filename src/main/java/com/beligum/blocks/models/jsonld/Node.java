package com.beligum.blocks.models.jsonld;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by wouter on 23/04/15.
 */
public interface Node
{
    public boolean isString();

    public boolean isDouble();

    public boolean isLong();

    public boolean isBoolean();

    public boolean isInt();

    public boolean isList();

    public boolean isNull();

    public LinkedHashMap<String, Node> getMap();

    public ArrayList<Node> getList();

    public String getString();

    public Double getDouble();

    public Integer getInteger();

    public Boolean getBoolean();

    public Long getLong();

    public boolean isResource();

    public Node copy();



}
