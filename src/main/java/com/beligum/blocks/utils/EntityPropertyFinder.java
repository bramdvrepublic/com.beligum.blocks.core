package com.beligum.blocks.utils;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.models.Entity;
import com.beligum.blocks.models.EntityField;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wouter on 9/04/15.
 */
public class EntityPropertyFinder
{
    private HashMap<String, Integer> propertyCounter = new HashMap<>();

    public EntityField getProperty(String key, HashMap<String, Object> properties, String language) {
        return findProperty(key, properties, getPropertyIndex(key), language);
    }

    public void propertyFound(String key) {
        propertyCounter.put(key, getPropertyIndex(key) + 1);
    }

    public EntityField getFirstProperty(String key, HashMap<String, Object> properties, String language) {
        return findProperty(key, properties, 0, language);
    }

    public Integer getPropertyIndex(String key) {
        if (!propertyCounter.containsKey(key)) propertyCounter.put(key, 0);
        return propertyCounter.get(key);
    }

    protected EntityField findProperty(String key, HashMap<String, Object> properties, Integer index, String language) {
        EntityField retVal = null;
        if (properties.containsKey(key)) {
            if (properties.get(key) instanceof List) {
                // We found an entity
                ArrayList<Object> property = (ArrayList<Object>)properties.get(key);
                if (property.size() > index) {
                    retVal = new Entity((HashMap<String, Object>)property.get(index));
                }
            } else if (properties.get(key) instanceof Map) {
                HashMap<String, Object> property = (HashMap<String, Object>)properties.get(key);
                if (property.containsKey(language) && property.get(language) instanceof List) {
                    ArrayList<String> values = (ArrayList<String>)property.get(language);
                    if (values.size() > index) {
                        retVal = new EntityField(values.get(index));
                    }
                }
                if (retVal == null && (property.containsKey(EntityField.NO_LANGUAGE) && property.get(language) instanceof List)) {
                    ArrayList<String> values = (ArrayList<String>)property.get(language);
                    if (values.size() > index) {
                        retVal = new EntityField(values.get(index));
                    }
                }
            } else {
                Logger.error("Unknown value when fetching property from entity");
            }
        }
        return retVal;
    }
}
