package com.beligum.blocks.models.jsonld;

import com.beligum.blocks.models.jsonld.interfaces.Node;

import java.util.*;

/**
 * Created by wouter on 14/05/15.
 */
public class OrientNode implements Node
{
    private Object wrappedObject = null;
    private Boolean isString = null;
    private Boolean isDouble = null;
    private Boolean isLong = null;
    private Boolean isBoolean = null;
    private Boolean isInt = null;
    private Boolean isList = null;
    private Locale language = Locale.ROOT;

    protected OrientNode() {
    }

    protected OrientNode(Object value) {
        this(value, null);

    }

    protected OrientNode(Boolean value) {
        this(value, null);

    }

    protected OrientNode(String value) {
        this(value, null);

    }

    protected OrientNode(Double value) {
        this(value, null);

    }

    protected OrientNode(Long value) {
        this(value, null);

    }

    protected OrientNode(Integer value) {
        this(value, null);

    }

    protected OrientNode(List value) {
        this(value, null);

    }

    protected OrientNode(Object object, Locale language) {
        if (language == null) language = Locale.ROOT;
        this.wrappedObject = object;
        this.language = language;
    }

    protected OrientNode(Boolean object, Locale language) {
        if (language == null) language = Locale.ROOT;
        this.wrappedObject = object;
        this.language = language;
        isBoolean = true;
    }

    protected OrientNode(String object, Locale language) {
        if (language == null) language = Locale.ROOT;
        this.wrappedObject = object;
        this.language = language;
        isString = true;
    }

    protected OrientNode(Double object, Locale language) {
        if (language == null) language = Locale.ROOT;
        this.wrappedObject = object;
        this.language = language;
        isDouble = true;
    }

    protected OrientNode(Long object, Locale language) {
        if (language == null) language = Locale.ROOT;
        this.wrappedObject = object;
        this.language = language;
        isLong = true;
    }

    protected OrientNode(Integer object, Locale language) {
        if (language == null) language = Locale.ROOT;
        this.wrappedObject = object;
        this.language = language;
        isInt = true;
    }

    protected OrientNode(List object, Locale language) {
        if (language == null) language = Locale.ROOT;
        this.wrappedObject = object;
        this.language = language;
        isList = true;
    }

    @Override
    public boolean isString()
    {
        boolean retVal = false;
        if ((isString != null && isString) || wrappedObject instanceof String){
            retVal = true;
            isString = true;
        }
        return retVal;
    }
    @Override
    public boolean isDouble()
    {
        boolean retVal = false;
        if ((isDouble != null && isDouble) || wrappedObject instanceof Double){
            retVal = true;
            isDouble = true;
        }
        return retVal;
    }
    @Override
    public boolean isLong()
    {
        boolean retVal = false;
        if ((isLong != null && isLong) || wrappedObject instanceof Long){
            retVal = true;
            isLong = true;
        }
        return retVal;
    }
    @Override
    public boolean isBoolean()
    {
        boolean retVal = false;
        if ((isBoolean != null && isBoolean) ||  wrappedObject instanceof Boolean){
            retVal = true;
            isBoolean = true;
        }
        return retVal;
    }
    @Override
    public boolean isInt()
    {
        boolean retVal = false;
        if ((isInt != null && isInt) ||  wrappedObject instanceof Integer){
            retVal = true;
            isInt = true;
        }
        return retVal;
    }
    @Override
    public boolean isIterable()
    {
        boolean retVal = false;
        if ((isList != null && isList) ||  wrappedObject instanceof List){
            retVal = true;
            isList = true;
        }
        return retVal;
    }
    @Override
    public boolean isNull()
    {
        return wrappedObject == null;
    }

    @Override
    public Iterable<Node> getIterable()
    {
        return new NodeIterable((Iterable)wrappedObject, language);
    }

    @Override
    public String asString()
    {
        String retVal = null;
        if (isString()) {
            retVal = (String)wrappedObject;
        } else if (isInt() || isBoolean() || isDouble() || isLong()) {
            retVal = wrappedObject.toString();
        }
        return retVal;
    }

    @Override
    public String toString()
    {
        return asString();
    }

    @Override
    public Locale getLanguage()
    {
        return language;
    }

    @Override
    public Double getDouble()
    {
        Double retVal = null;
        if (isDouble() || isInt() || isLong()) {
            retVal = (Double)wrappedObject;
        }
        return retVal;
    }
    @Override
    public Integer getInteger()
    {
        Integer retVal = null;
        if (isInt()) {
            retVal = (Integer)wrappedObject;
        }
        return retVal;
    }
    @Override
    public Boolean getBoolean()
    {
        Boolean retVal = null;
        if (isBoolean()) {
            retVal = (Boolean)wrappedObject;
        }
        return retVal;
    }
    @Override
    public Long getLong()
    {
        Long retVal = null;
        if (isLong() || isInt()) {
            retVal = (Long)wrappedObject;
        }
        return retVal;
    }

    @Override
    public Object getValue()
    {
        return wrappedObject;
    }


    @Override
    public boolean isResource()
    {
        return false;
    }


    @Override
    public Node copy()
    {
        return null;
    }
}
