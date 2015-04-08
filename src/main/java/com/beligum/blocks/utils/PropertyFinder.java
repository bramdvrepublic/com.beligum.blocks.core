package com.beligum.blocks.utils;

import com.beligum.blocks.models.interfaces.NamedProperty;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by wouter on 2/04/15.
 */
public class PropertyFinder<T extends NamedProperty>
{
    private HashMap<String, Integer> propertyCounter = new HashMap<>();

    public T getProperty(String key, ArrayList<T> properties) {
        return findProperty(key, properties, getPropertyIndex(key));
    }

    public void propertyFound(String key) {
        propertyCounter.put(key, getPropertyIndex(key) + 1);
    }

    public T getFirstProperty(String key, ArrayList<T> properties) {
        return findProperty(key, properties, 0);
    }

    public Integer getPropertyIndex(String key) {
        if (!propertyCounter.containsKey(key)) propertyCounter.put(key, 0);
        return propertyCounter.get(key);
    }

    protected T findProperty(String key, ArrayList<T> properties, Integer index) {
        T retVal = null;
        Integer counter = 0;
        for (T property: properties) {
            if (property.getName().equals(key)) {
                if (index == counter) {
                    retVal = property;
                    break;
                } else {
                    counter++;
                }
            }
        }
        return retVal;
    }


}
