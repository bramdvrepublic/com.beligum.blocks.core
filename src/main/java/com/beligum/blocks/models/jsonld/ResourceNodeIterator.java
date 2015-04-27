package com.beligum.blocks.models.jsonld;

import java.util.HashMap;

/**
 * Created by wouter on 24/04/15.
 */
public class ResourceNodeIterator extends ResourceNode
{
    HashMap<String, Integer> counter = new HashMap<>();

    private ResourceNodeIterator(ResourceNode resource) {
            this.wrap(resource.unwrap());
    }

    public static ResourceNodeIterator create(ResourceNode resource) {
        ResourceNodeIterator retVal = null;
        if (resource != null) {
            retVal = new ResourceNodeIterator(resource);
        }
        return retVal;
    }

    @Override
    public Node get(String key) {
        Node retVal = super.get(key);
        if (retVal != null) {
            if (retVal.isList()) {
                int index = getPropertyIndex(key);
                if (index < retVal.getList().size()) {
                    retVal = retVal.getList().get(index);
                }
            } else if (getPropertyIndex(key) > 0){
                retVal = null;
            }
        }
        return retVal;
    }


    public void incrementPropertyIndex(String key) {
        if (!counter.containsKey(key)) {
            counter.put(key, 0);
        }
        counter.put(key, counter.get(key)+1);
    }

    public int getPropertyIndex(String key) {
        if (!counter.containsKey(key)) {
            counter.put(key, 0);
        }
        return counter.get(key);
    }

    public int getPropertyValueCount(String key) {
        int retVal = 0;
        Node value = super.get(key);
        if (value != null && value.isList()) {
            retVal = value.getList().size();
        } else if (value != null) {
            retVal = 1;
        }
        return retVal;
    }
}
