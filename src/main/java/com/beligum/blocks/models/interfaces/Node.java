package com.beligum.blocks.models.interfaces;

import com.beligum.blocks.controllers.interfaces.PersistenceController;

import java.util.Locale;

/**
 * Created by wouter on 23/04/15.
 */
public interface Node extends Iterable<Node>
{
    public boolean isString();

    public boolean isDouble();

    public boolean isLong();

    public boolean isBoolean();

    public boolean isInt();

    public boolean isIterable();

    public boolean isMap();

    public boolean isReference();

    public boolean isNull();

    public boolean isResource();

    public String asString();

    public Locale getLanguage();

    public Double getDouble();

    public Integer getInteger();

    public Boolean getBoolean();

    public Long getLong();

    public Object getValue();

    public ResourceFactory getFactory();
}
