package com.beligum.blocks.models;

import com.beligum.blocks.models.interfaces.NamedProperty;
import com.beligum.blocks.utils.URLFactory;

/**
 * Created by wouter on 2/04/15.
 */
public class EntityField implements NamedProperty
{
    protected String name;

    protected String value;

    public EntityField() {

    }

    public EntityField(String name) {
        this.name = name;
    }

    public EntityField(String name, String value) {
        if (name != null) {
            this.name = URLFactory.makeAbsoluteRdfValue(name);
        } else {
            this.name = null;
        }
        this.value = value;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = URLFactory.makeAbsoluteRdfValue(name);
    }
    public String getValue()
    {
        return value;
    }
    public void setValue(String value)
    {
        this.value = value;
    }
}
