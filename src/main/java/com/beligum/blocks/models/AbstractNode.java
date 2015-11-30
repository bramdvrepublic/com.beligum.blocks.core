package com.beligum.blocks.models;

import com.beligum.blocks.models.factories.ResourceFactoryImpl;
import com.beligum.blocks.models.interfaces.Node;

import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Created by wouter on 31/05/15.
 */
public abstract class AbstractNode implements Node
{
    protected Boolean isString = null;
    protected Boolean isDouble = null;
    protected Boolean isLong = null;
    protected Boolean isBoolean = null;
    protected Boolean isInt = null;
    protected Boolean isIterable = null;
    protected Locale language = Locale.ROOT;

    // ---- CONSTRUCTORS -------

    protected AbstractNode()
    {

    }

    protected AbstractNode(Object object, Locale language)
    {
        if (language == null)
            language = Locale.ROOT;
        this.language = language;
        this.setValue(object);
    }

    @Override
    public boolean isString()
    {
        boolean retVal = false;
        if ((isString != null && isString) || getValue() instanceof String) {
            retVal = true;
        }
        isString = retVal;
        return retVal;
    }
    @Override
    public boolean isDouble()
    {
        boolean retVal = false;
        if ((isDouble != null && isDouble) || getValue() instanceof Double) {
            retVal = true;
        }
        isDouble = retVal;
        return retVal;
    }
    @Override
    public boolean isLong()
    {
        boolean retVal = false;
        if ((isLong != null && isLong) || getValue() instanceof Long) {
            retVal = true;
        }
        isLong = retVal;
        return retVal;
    }
    @Override
    public boolean isBoolean()
    {
        boolean retVal = false;
        if ((isBoolean != null && isBoolean) || getValue() instanceof Boolean) {
            retVal = true;
        }
        isBoolean = retVal;
        return retVal;
    }
    @Override
    public boolean isInt()
    {
        boolean retVal = false;
        if ((isInt != null && isInt) || getValue() instanceof Integer) {
            retVal = true;
        }
        isInt = retVal;
        return retVal;
    }
    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;

        if (this.getValue() == o)
            return true;

        boolean retVal = false;
        if (this.getValue() == null) {
            retVal = o == null;
        }
        else if (o instanceof Node) {
            retVal = this.getValue().equals(((Node) o).getValue());
        }
        else {
            retVal = this.getValue().equals(o);
        }
        return retVal;

    }
    @Override
    public int hashCode()
    {
        return this.getValue() != null ? this.getValue().hashCode() : 0;
    }
    @Override
    public boolean isIterable()
    {
        return false;
    }

    @Override
    public boolean isMap()
    {
        return false;
    }

    @Override
    public boolean isReference()
    {
        return false;
    }

    @Override
    public boolean isNull()
    {
        return getValue() == null;
    }
    @Override
    public boolean isResource()
    {
        return false;
    }

    @Override
    public String asString()
    {
        String retVal = null;
        if (isNull()) {
            retVal = "";
        }
        else if (isString()) {
            retVal = (String) getValue();
        }
        else if (isInt() || isBoolean() || isDouble() || isLong()) {
            retVal = getValue().toString();
        }
        else if (getValue() instanceof List) {
            retVal = getValue().toString();
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
            retVal = (Double) getValue();
        }
        return retVal;
    }
    @Override
    public Integer getInteger()
    {
        Integer retVal = null;
        if (isInt()) {
            retVal = (Integer) getValue();
        }
        return retVal;
    }
    @Override
    public Boolean getBoolean()
    {
        Boolean retVal = null;
        if (isBoolean()) {
            retVal = (Boolean) getValue();
        }
        return retVal;
    }
    @Override
    public Long getLong()
    {
        Long retVal = null;
        if (isLong() || isInt()) {
            retVal = (Long) getValue();
        }
        return retVal;
    }

    @Override
    public abstract Object getValue();

    protected abstract void setValue(Object value);

    @Override
    public Iterator<Node> iterator()
    {
        List list = new ArrayList<Node>();
        list.add(this);
        return list.iterator();
    }

    // ----- PROTECTED METHODS --------

    protected boolean isNumeric(String str)
    {
        {
            str = str.trim();
            if (str.equals(""))
                return false;
            DecimalFormatSymbols currentLocaleSymbols = DecimalFormatSymbols.getInstance();
            char localeMinusSign = currentLocaleSymbols.getMinusSign();

            if (!Character.isDigit(str.charAt(0)) && str.charAt(0) != localeMinusSign)
                return false;

            boolean isDecimalSeparatorFound = false;
            char localeDecimalSeparator = currentLocaleSymbols.getDecimalSeparator();

            for (char c : str.substring(1).toCharArray()) {
                if (!Character.isDigit(c)) {
                    if ((c == ',' || c == '.') && !isDecimalSeparatorFound) {
                        isDecimalSeparatorFound = true;
                        continue;
                    }
                    return false;
                }
            }
            return true;
        }
    }

    protected class NodeIterator implements Iterator<Node>
    {
        private Iterator internalIterator;
        private Locale locale;

        public NodeIterator(Iterator value, Locale locale)
        {
            internalIterator = value;
            this.locale = locale;
        }

        @Override
        public boolean hasNext()
        {
            return internalIterator.hasNext();
        }

        @Override
        public Node next()
        {
            Node retVal = null;
            Object value = internalIterator.next();
            retVal = ResourceFactoryImpl.instance().createNode(value, locale);
            return retVal;

        }
        @Override
        public void remove()
        {
            internalIterator.remove();
        }
    }

}
