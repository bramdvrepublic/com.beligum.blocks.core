package com.beligum.blocks.html.db;

import com.beligum.blocks.html.Cacher.TypeCacher;
import com.beligum.blocks.html.models.ifaces.EntityID;
import org.jsoup.nodes.Element;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Created by wouter on 21/11/14.
 */
public class BlocksMemDB extends BlocksDB
{

    private HashMap<String, HashMap<String, Element>> db =  new HashMap<String, HashMap<String, Element>>();

    public Element get(EntityID id) {
        Element retVal = null;
        if (id.hasImplementationID()) {
            retVal = db.get(id.getEntityName()).get(id.getImplementationID()).clone();
        }
        return retVal;
    }

    public void put(EntityID id, Element element) {
        if (!id.hasImplementationID()) {
            //id = Id.newImplementation(id);
            // create new ImplementationID;
        }
        db.get(id.getEntityName()).put(id.getImplementationID(), element);

    }

    public String getNewIDForEntity(String entity) {
        return randomNrGenerator();
    }

    private String randomNrGenerator() {
        return UUID.randomUUID().toString();
    }
}
