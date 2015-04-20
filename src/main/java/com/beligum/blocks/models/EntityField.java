package com.beligum.blocks.models;

import com.beligum.blocks.models.interfaces.NamedProperty;
import com.beligum.blocks.utils.URLFactory;

import java.util.HashMap;

/**
 * Created by wouter on 2/04/15.
 */
public class EntityField
{
    public static final String NO_LANGUAGE = "default";
    private String value;

    public EntityField() {

    }

    public EntityField(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

}
