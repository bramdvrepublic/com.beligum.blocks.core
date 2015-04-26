package com.beligum.blocks.utils;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.models.BasicTemplate;
import com.beligum.blocks.models.EntityField;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wouter on 2/04/15.
 */
public class PropertyFinder
{
    private HashMap<String, Integer> propertyCounter = new HashMap<>();

    public BasicTemplate getProperty(String key, ArrayList<BasicTemplate> properties)
    {
        return findProperty(key, properties, getPropertyIndex(key));
    }

    public void propertyFound(String key)
    {
        propertyCounter.put(key, getPropertyIndex(key) + 1);
    }

    public BasicTemplate getFirstProperty(String key, ArrayList<BasicTemplate> properties)
    {
        return findProperty(key, properties, 0);
    }

    public Integer getPropertyIndex(String key)
    {
        if (!propertyCounter.containsKey(key))
            propertyCounter.put(key, 0);
        return propertyCounter.get(key);
    }

    public static BasicTemplate findProperty(String key, ArrayList<BasicTemplate> properties, Integer index)
    {
        BasicTemplate retVal = null;
        if (properties == null)
            return retVal;
        Integer counter = 0;
        for (BasicTemplate property : properties) {
            if (property.getName() == "" || property.getName().equals(key)) {
                if (index == counter) {
                    retVal = property;
                    break;
                }
                else {
                    counter++;
                }
            }
        }
        return retVal;
    }

    public static EntityField findProperty(String key, HashMap<String, Object> properties, Integer index, String language)
    {
        EntityField retVal = null;
        if (properties == null)
            return retVal;
        if (properties.containsKey(key)) {
            if (properties.get(key) instanceof List) {
                // We found an entity
                ArrayList<Object> property = (ArrayList<Object>) properties.get(key);
                if (property.size() > index) {
                    retVal = Blocks.factory().createEntity((HashMap<String, Object>) property.get(index));
                }
            }
            else if (properties.get(key) instanceof Map) {
                HashMap<String, Object> property = (HashMap<String, Object>) properties.get(key);
                if (property.containsKey(language) && property.get(language) instanceof List) {
                    ArrayList<String> values = (ArrayList<String>) property.get(language);
                    if (values.size() > index) {
                        retVal = new EntityField(values.get(index));
                    }
                }
                if (retVal == null && (property.containsKey(EntityField.NO_LANGUAGE) && property.get(EntityField.NO_LANGUAGE) instanceof List)) {
                    ArrayList<String> values = (ArrayList<String>) property.get(EntityField.NO_LANGUAGE);
                    if (values.size() > index) {
                        retVal = new EntityField(values.get(index));
                    }
                }
            }
            else {
                Logger.error("Unknown value when fetching property from entity");
            }
        }
        return retVal;
    }

    public static Integer getFieldsLeft(String key, HashMap<String, Object> properties, String language, int index)
    {
        Integer retVal = 0;
        if (properties.containsKey(key)) {
            if (properties.get(key) instanceof List) {
                // We found an entity
                ArrayList<Object> property = (ArrayList<Object>) properties.get(key);
                retVal = property.size();
            }
            else if (properties.get(key) instanceof Map) {
                HashMap<String, Object> property = (HashMap<String, Object>) properties.get(key);
                if (property.containsKey(language) && property.get(language) instanceof List) {
                    retVal = ((ArrayList<String>) property.get(language)).size();

                }
                if ((property.containsKey(EntityField.NO_LANGUAGE) && property.get(EntityField.NO_LANGUAGE) instanceof List)) {
                    Integer size = ((ArrayList<String>) property.get(EntityField.NO_LANGUAGE)).size();
                    if (size > retVal)
                        retVal = size;
                }
            }
        }
        retVal = retVal - index;
        if (retVal < 0)
            retVal = 0;
        return retVal;
    }

}
