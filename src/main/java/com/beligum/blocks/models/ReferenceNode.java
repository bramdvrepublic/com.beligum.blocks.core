package com.beligum.blocks.models;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Locale;

/**
 * Created by wouter on 26/08/15.
 */
public class ReferenceNode extends NodeImpl
{
    private URI reference = null;

    public ReferenceNode(Object value)
    {
        super(value, Locale.ROOT);
    }

    @Override
    public Object getValue() {
        if (reference != null) {
            return reference;
        } else {
            return super.getValue();
        }
    }

    @Override
    public void setValue(Object value) {
        if (value instanceof String) {
            try {
                UriBuilder.fromUri((String)value).build();
            } catch (Exception e) {
                super.setValue(value);
            }
        }
    }

    @Override
    public boolean isReference() {
        boolean retVal = false;
        if (reference != null) {
            retVal = true;
        }
        return retVal;
    }
}
