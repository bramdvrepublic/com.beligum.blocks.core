package com.beligum.blocks.database;

import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.database.interfaces.BlocksDatabase;
import com.beligum.blocks.pages.ifaces.MasterWebPage;
import com.beligum.blocks.pages.ifaces.WebPage;
import com.beligum.blocks.resources.dummy.DummyNode;
import com.beligum.blocks.resources.dummy.DummyResource;
import com.beligum.blocks.resources.interfaces.Node;
import com.beligum.blocks.resources.interfaces.Resource;
import com.beligum.blocks.routing.ifaces.WebNode;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by wouter on 22/06/15.
 */
public class DummyBlocksDatabase implements BlocksDatabase
{
    private static DummyBlocksDatabase instance;

    private DummyBlocksDatabase() {

    }

    public static DummyBlocksDatabase instance() {
        if (DummyBlocksDatabase.instance == null) {
            DummyBlocksDatabase.instance = new DummyBlocksDatabase();
        }
        return DummyBlocksDatabase.instance;
    }

    @Override
    public MasterWebPage createMasterWebPage(URI id)
    {
        return null;
    }
    @Override
    public WebPage createLocalizedPage(MasterWebPage masterWebPage, URI id, Locale locale)
    {
        return null;
    }
    @Override
    public MasterWebPage getMasterWebPage(URI id)
    {
        return null;
    }
    @Override
    public WebPage getLocalizedWebPage(URI id, Locale locale)
    {
        return null;
    }
    @Override
    public WebPage deleteWebPage(URI id, Locale locale)
    {
        return null;
    }
    @Override
    public WebPage saveWebPage(WebPage webPage, boolean doVersion)
    {
        return null;
    }
    @Override
    public WebNode createRootWebNode(String host)
    {
        return null;
    }
    @Override
    public WebNode getRootWebNode(String host)
    {
        return null;
    }
    @Override
    public WebNode createWebNode(WebNode from, String path, Locale locale)
    {
        return null;
    }
    @Override
    public Resource createResource(URI id, URI rdfType, Locale language)
    {
        HashMap<String, Object> vertex = new HashMap<String, Object>();
        HashMap<String, Object> localized = new HashMap<String, Object>();
        vertex.put(ParserConstants.JSONLD_ID, id);
        vertex.put(ParserConstants.JSONLD_ID, rdfType);
        Resource retVal = new DummyResource(vertex, localized, language);

        return retVal;
    }
    @Override
    public Resource getResource(URI id, Locale language)
    {
        return null;
    }
    @Override
    public Resource saveResource(Resource resource)
    {
        return null;
    }
    @Override
    public Resource deleteResource(Resource resource)
    {
        return null;
    }
    @Override
    public Node createNode(Object value, Locale language)
    {
        Node retVal = null;
        if (value instanceof Resource || value instanceof Node) {
            retVal = (Node)value;
        } else {
            if (value instanceof List && ((List)value).size() == 2
                && ((List)value).get(0) instanceof HashMap
                && ((HashMap<String, Object>)((List)value).get(0)).containsKey(ParserConstants.JSONLD_ID)) {
                retVal = new DummyResource((HashMap<String, Object>)((List)value).get(0), (HashMap<String, Object>)((List)value).get(1), language);
            } else if (value instanceof Map && ((Map)value).containsKey(ParserConstants.JSONLD_ID)) {

                retVal = new DummyResource((Map)value, new HashMap<String, Object>(), language);
            } else {
                retVal = new DummyNode(value, language);
            }
        }

        return retVal;
    }
}
