package com.beligum.blocks.models.jsonld.interfaces;

import java.util.Iterator;
import java.util.Locale;

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

    public boolean isIterable();

    public boolean isNull();

    public Iterable<Node> getIterable();

    public String asString();

    public Locale getLanguage();

    public Double getDouble();

    public Integer getInteger();

    public Boolean getBoolean();

    public Long getLong();

    public Object getValue();

    public boolean isResource();

    public Node copy();



}
