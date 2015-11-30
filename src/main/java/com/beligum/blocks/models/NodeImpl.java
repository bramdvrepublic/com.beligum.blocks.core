package com.beligum.blocks.models;

import com.beligum.blocks.models.factories.ResourceFactoryImpl;
import com.beligum.blocks.models.interfaces.ResourceFactory;

import java.util.Locale;

/**
 * Created by wouter on 22/06/15.
 */
public class NodeImpl extends AbstractNode
{
    protected Object value;

    public NodeImpl(Object value, Locale lang)
    {
        super(value, lang);
    }

    @Override
    public ResourceFactory getFactory()
    {
        return ResourceFactoryImpl.instance();
    }

    @Override
    public Object getValue()
    {
        return this.value;
    }

    @Override
    protected void setValue(Object value)
    {
        isString = false;
        isDouble = false;
        isLong = false;
        isBoolean = false;
        isIterable = false;
        isInt = false;
        this.value = value;
        if (value instanceof String) {
            String str = (String) value;

            if (isNumeric(str)) {
                try {
                    this.value = Double.parseDouble(str);

                    // Check if our numeric has decimals. If not it is a Long or an Int
                    if (this.getValue().equals(Math.rint((Double) this.getValue()))) {
                        Long longValue = ((Double) this.getValue()).longValue();
                        if (longValue < Integer.MAX_VALUE && longValue > Integer.MIN_VALUE) {
                            this.value = ((Double) this.getValue()).intValue();
                            this.isInt = true;
                        }
                        else {
                            this.value = longValue;
                            this.isLong = true;
                        }
                    }
                    else {
                        this.isDouble = true;
                    }
                }
                catch (Exception e) {
                    this.isString = true;
                }

            }
            // Special parse for boolean because Boolean.parseBoolean always returns false if string != 'true'
            // What would make all our strings into fals booleans
            else if (Boolean.parseBoolean(str) || (str.length() == 5 && str.toLowerCase().equals("false"))) {
                this.value = Boolean.parseBoolean(str);
                this.isBoolean = true;
            }
            else {
                this.isString = true;
            }

        }
        else if (this.getValue() instanceof Boolean) {
            isBoolean = true;
        }
        else if (this.getValue() instanceof Double) {
            isDouble = true;
        }
        else if (this.getValue() instanceof Float) {
            isDouble = true;
        }
        else if (this.getValue() instanceof Integer) {
            isInt = true;
        }
        else if (this.getValue() instanceof Long) {
            isLong = true;
        }
        else {
            this.value = null;
        }
    }
}
