package com.beligum.blocks.html.db;

import com.beligum.blocks.html.models.ifaces.EntityID;
import org.jsoup.nodes.Element;

/**
 * Created by wouter on 21/11/14.
 */
public abstract class BlocksDB
{
    public abstract Element get(EntityID id);

    public abstract void put(EntityID id, Element element);

    public abstract String getNewIDForEntity(String entity);

}
