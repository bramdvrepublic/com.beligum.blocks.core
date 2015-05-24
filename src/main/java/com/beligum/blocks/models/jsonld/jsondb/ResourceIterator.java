package com.beligum.blocks.models.jsonld.jsondb;

import com.beligum.blocks.models.jsonld.interfaces.Node;
import com.beligum.blocks.models.jsonld.interfaces.Resource;

import java.util.HashMap;

/**
* Created by wouter on 24/04/15.
*/
public class ResourceIterator extends ResourceImpl
{
    HashMap<String, Integer> counter = new HashMap<>();

    private ResourceIterator(Resource resource) {
            this.wrap(resource.unwrap());
    }

    public static ResourceIterator create(Resource resource) {
        ResourceIterator retVal = null;
        if (resource != null) {
            retVal = new ResourceIterator(resource);
        }
        return retVal;
    }

    @Override
    public Node get(String key) {
        Node retVal = super.get(key);
        if (retVal != null) {
            if (retVal.isIterable()) {
                int index = getPropertyIndex(key);
//                if (index < retVal.getIterable().size()) {
////                    retVal = retVal.getIterable().get(index);
//                }
            } else if (getPropertyIndex(key) > 0){
                retVal = new BlankNode();
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
        if (value != null && value.isIterable()) {
//            retVal = value.getIterable().size();
        } else if (value != null) {
            retVal = 1;
        }
        return retVal;
    }
}
