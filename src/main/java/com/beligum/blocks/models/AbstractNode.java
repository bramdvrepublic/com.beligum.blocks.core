package com.beligum.blocks.models;


import com.beligum.blocks.models.factories.ResourceFactoryImpl;
import com.beligum.blocks.models.interfaces.Node;

import java.text.DecimalFormatSymbols;
import java.util.*;

/**
 * Created by wouter on 31/05/15.
 */
public abstract class AbstractNode implements Node
{
    protected Object wrappedObject = null;
    protected Boolean isString = null;
    protected Boolean isDouble = null;
    protected Boolean isLong = null;
    protected Boolean isBoolean = null;
    protected Boolean isInt = null;
    protected Boolean isIterable = null;
    protected Boolean isMap = null;
    protected Locale language = Locale.ROOT;

    // ---- CONSTRUCTORS -------

    protected AbstractNode() {

    }

    protected AbstractNode(Object object, Locale language) {
        if (language == null) language = Locale.ROOT;
        this.wrappedObject = object;
        this.language = language;
        if (this.wrappedObject instanceof String) {
            String str = (String)object;

            if (isNumeric(str)) {
                try {
                    this.wrappedObject = Double.parseDouble(str);

                    // Check if our numeric has decimals. If not it is a Long or an Int
                    if (this.wrappedObject.equals(Math.rint((Double) this.wrappedObject))) {
                        Long longValue = ((Double)this.wrappedObject).longValue();
                        if (longValue < Integer.MAX_VALUE && longValue > Integer.MIN_VALUE) {
                            this.wrappedObject = ((Double)this.wrappedObject).intValue();
                            this.isInt = true;
                        } else {
                            this.wrappedObject = longValue;
                            this.isLong = true;
                        }
                    } else {
                        this.isDouble = true;
                    }
                } catch (Exception e) {
                    this.isString = true;
                }

            }
            // Special parse for boolean because Boolean.parseBoolean always returns false if string != 'true'
            // What would make all our strings into fals booleans
            else if (Boolean.parseBoolean(str) || (str.length() == 5 && str.toLowerCase().equals("false"))) {
                this.wrappedObject = Boolean.parseBoolean(str);
                this.isBoolean = true;
            } else {
                this.isString = true;
            }
        } else if (this.wrappedObject instanceof List || this.wrappedObject instanceof Set) {
            isIterable = true;
        } else if (this.wrappedObject instanceof Map) {
            isMap = true;
        } else if (this.wrappedObject instanceof Boolean) {
            isBoolean = true;
        } else if (this.wrappedObject instanceof Double) {
            isDouble = true;
        } else if (this.wrappedObject instanceof Float) {
            isDouble = true;
        } else if (this.wrappedObject instanceof Integer) {
            isInt = true;
        } else if (this.wrappedObject instanceof Long) {
            isLong = true;
        } else {
            this.wrappedObject = null;
        }

    }


    @Override
    public boolean isString() {
        boolean retVal = false;
        if ((isString != null && isString) || wrappedObject instanceof String){
            retVal = true;
        }
        isString = retVal;
        return retVal;
    }
    @Override
    public boolean isDouble() {
        boolean retVal = false;
        if ((isDouble != null && isDouble) || wrappedObject instanceof Double){
            retVal = true;
        }
        isDouble = retVal;
        return retVal;
    }
    @Override
    public boolean isLong() {
        boolean retVal = false;
        if ((isLong != null && isLong) || wrappedObject instanceof Long){
            retVal = true;
        }
        isLong = retVal;
        return retVal;
    }
    @Override
    public boolean isBoolean() {
        boolean retVal = false;
        if ((isBoolean != null && isBoolean) ||  wrappedObject instanceof Boolean){
            retVal = true;
        }
        isBoolean = retVal;
        return retVal;
    }
    @Override
    public boolean isInt() {
        boolean retVal = false;
        if ((isInt != null && isInt) ||  wrappedObject instanceof Integer){
            retVal = true;
        }
        isInt = retVal;
        return retVal;
    }
    @Override
    public boolean isIterable() {
        boolean retVal = false;
        if ((isIterable != null && isIterable) ||  (wrappedObject instanceof Iterable  && !this.isResource())){
            retVal = true;
        }
        isIterable = retVal;
        return retVal;
    }

    @Override
    public boolean isMap() {
        boolean retVal = false;
        if ((isMap != null && isMap) ||  (wrappedObject instanceof Map  && !this.isResource())){
            retVal = true;
        }
        isIterable = retVal;
        return retVal;
    }

    @Override
    public boolean isNull()
    {
        return wrappedObject == null;
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
        if (isString()) {
            retVal = (String)wrappedObject;
        } else if (isInt() || isBoolean() || isDouble() || isLong()) {
            retVal = wrappedObject.toString();
        } else if (wrappedObject instanceof List) {
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
    public Iterator<Node> iterator()
    {
        Iterator retVal = null;
        if (this.isIterable()) {
            retVal = new NodeIterator(((Iterable)wrappedObject).iterator(), language);
        } else if (this.isResource()) {
            List list = new ArrayList();
            list.add(this);
            retVal = new NodeIterator(list.iterator(), language);
        } else {
            List list = new ArrayList();
            if (wrappedObject != null) {
                list.add(wrappedObject);
            }
            retVal = new NodeIterator(list.iterator(), language);
        }
        return retVal;
    }




    // ----- PRIVATE METHODS --------

    private boolean isNumeric(String str) {
        {
            str = str.trim();
            if (str.equals("")) return false;
            DecimalFormatSymbols currentLocaleSymbols = DecimalFormatSymbols.getInstance();
            char localeMinusSign = currentLocaleSymbols.getMinusSign();

            if ( !Character.isDigit( str.charAt( 0 ) ) && str.charAt( 0 ) != localeMinusSign ) return false;

            boolean isDecimalSeparatorFound = false;
            char localeDecimalSeparator = currentLocaleSymbols.getDecimalSeparator();

            for ( char c : str.substring( 1 ).toCharArray() )
            {
                if ( !Character.isDigit( c ) )
                {
                    if ( (c == ',' || c == '.')  && !isDecimalSeparatorFound )
                    {
                        isDecimalSeparatorFound = true;
                        continue;
                    }
                    return false;
                }
            }
            return true;
        }
    }

    private class NodeIterator implements Iterator<Node>
    {
        private Iterator internalIterator;
        private Locale locale;

        public NodeIterator(Iterator value, Locale locale) {
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
            retVal =  ResourceFactoryImpl.instance().createNode(value, locale);
            return retVal;

        }
        @Override
        public void remove()
        {
            internalIterator.remove();
        }
    }

}
