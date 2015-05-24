package com.beligum.blocks.utils;

import com.beligum.blocks.models.BasicTemplate;
import com.beligum.blocks.models.jsonld.interfaces.Node;
import com.beligum.blocks.models.jsonld.interfaces.Resource;

import java.util.HashMap;

/**
* Created by wouter on 2/04/15.
*/
public class PropertyFinder
{
    private HashMap<String, Integer> propertyCounter = new HashMap<>();

    public BasicTemplate getProperty(String key, BasicTemplate template) {
        return findProperty(key, template, getPropertyIndex(key));
    }

    public void propertyFound(String key)
    {
        propertyCounter.put(key, getPropertyIndex(key) + 1);
    }

    public BasicTemplate getFirstProperty(String key, BasicTemplate template) {
        return findProperty(key, template, 0);
    }

    public Integer getPropertyIndex(String key)
    {
        if (!propertyCounter.containsKey(key))
            propertyCounter.put(key, 0);
        return propertyCounter.get(key);
    }

    public static BasicTemplate findProperty(String key, BasicTemplate template, int index) {
        BasicTemplate retVal = null;
        Node node = template.get(key);
//        if (node != null && node.isIterable() && node.getIterable().size() > index) {
//            retVal = new BasicTemplate((Resource)node.getIterable().get(index));
//        } else if (node != null && index == 0 && node.isResource()) {
//            retVal = new BasicTemplate((Resource)node);
//        }
        return retVal;
    }




}
