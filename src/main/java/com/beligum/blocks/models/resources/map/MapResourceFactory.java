package com.beligum.blocks.models.resources.map;

import com.beligum.blocks.models.resources.interfaces.Node;
import com.beligum.blocks.models.resources.interfaces.Resource;
import com.beligum.blocks.models.resources.interfaces.ResourceFactory;
import com.beligum.blocks.models.resources.jackson.ResourceJsonDeserializer;
import com.beligum.blocks.models.resources.jackson.ResourceJsonSerializer;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;

import java.util.Locale;
import java.util.Map;

/**
 * Created by wouter on 31/05/15.
 */
public class MapResourceFactory implements ResourceFactory
{
    private static MapResourceFactory instance;

    // We define a partial schema for the default properties of a resource
    private MapResourceFactory() {

    }

    public static MapResourceFactory instance() {
        if (MapResourceFactory.instance == null) {
            MapResourceFactory.instance = new MapResourceFactory();
        }
        return MapResourceFactory.instance;
    }

    @Override
    public Resource createResource(String id, String rdfType, Locale language)
    {
        return null;
    }

    @Override
    public Node asNode(Object value, Locale language)
    {
        Node retVal = new MapNode(value, language);
        return retVal;
    }

    public Resource asResource(Map value, Locale language)
    {
        Resource retVal = new MapResource(value, language);
        return retVal;
    }

}
