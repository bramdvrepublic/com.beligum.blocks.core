package com.beligum.blocks.models.factories;

import com.beligum.base.server.RequestContext;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.controllers.PersistenceControllerImpl;
import com.beligum.blocks.models.interfaces.ResourceFactory;
import com.beligum.blocks.models.NodeImpl;
import com.beligum.blocks.models.ResourceImpl;
import com.beligum.blocks.models.WebPageImpl;
import com.beligum.blocks.models.interfaces.WebPath;
import com.beligum.blocks.models.interfaces.Node;
import com.beligum.blocks.models.interfaces.Resource;
import com.beligum.blocks.models.interfaces.WebPage;
import com.beligum.blocks.models.sql.DBPath;
import com.fasterxml.jackson.core.JsonProcessingException;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by wouter on 6/07/15.
 */
public class ResourceFactoryImpl implements ResourceFactory
{
    private static ResourceFactoryImpl instance;

    private ResourceFactoryImpl() {

    }

    public static ResourceFactoryImpl instance() {
        if (ResourceFactoryImpl.instance == null) {
            ResourceFactoryImpl.instance = new ResourceFactoryImpl();
        }
        return ResourceFactoryImpl.instance;
    }

    @Override
    public WebPage createWebPage(URI masterWebPage, URI id, Locale locale)
    {
        return new WebPageImpl(masterWebPage, id, locale);
    }


    @Override
    public WebPath createPath(URI masterPage, Path path, Locale locale)
    {
        return new DBPath(masterPage, path, locale);
    }

    @Override
    public Resource createResource(URI id, URI rdfType, Locale language)
    {
        HashMap<String, Object> vertex = new HashMap<String, Object>();
        HashMap<String, Object> localized = new HashMap<String, Object>();
        Resource retVal = new ResourceImpl(vertex, localized, language);
        retVal.setBlockId(id);
        retVal.setRdfType(rdfType);

        return retVal;
    }


    public Node createNode(Object value, Locale language)
    {
        Node retVal = null;
        if (value instanceof Resource || value instanceof Node) {
            retVal = (Node)value;
        } else {
            if (isResource(value)) {
                retVal = getResource(value, language);
            } else if (value instanceof Map && ((Map) value).containsKey(ParserConstants.JSONLD_VALUE)) {
                retVal = createNode(((Map) value).get(ParserConstants.JSONLD_VALUE), language);
            } else {
                retVal = new NodeImpl(value, language);
            }
        }

        return retVal;
    }

    @Override
    public boolean isResource(Object value) {
        boolean retVal = false;
        if (value instanceof Resource) {
            retVal = true;
        }
        else if (value instanceof List && ((List)value).size() == 2
                 && ((List)value).get(0) instanceof HashMap
                 && ((HashMap<String, Object>)((List)value).get(0)).containsKey(ParserConstants.JSONLD_ID)) {
            retVal = true;
        } else if (value instanceof Map && ((Map)value).containsKey(ParserConstants.JSONLD_ID)) {
            retVal = true;
        }
        return retVal;
    }

    @Override
    public Resource getResource(Object value, Locale language) {
        Resource retVal = null;
        if (value instanceof Resource) {
            retVal = (Resource)value;
        }
        // Is this a resource wrapped as a list?
        else if (value instanceof List && ((List)value).size() == 2
                 && ((List)value).get(0) instanceof HashMap
                 && ((HashMap<String, Object>)((List)value).get(0)).containsKey(ParserConstants.JSONLD_ID)) {
            HashMap<String, Object> rootVector = (HashMap<String, Object>) ((List) value).get(0);
            HashMap<String, Object> localVector = (HashMap<String, Object>) ((List) value).get(1);

            retVal = new ResourceImpl(rootVector, localVector, language);
        }
        // This is a reference to a resource
        else if (value instanceof Map && ((Map)value).containsKey(ParserConstants.JSONLD_ID)) {
            try {
                retVal = PersistenceControllerImpl.instance().getResource(UriBuilder.fromUri((String) ((Map) value).get(ParserConstants.JSONLD_ID)).build(), language);
            }
            catch (Exception e) {
                Logger.error("Could not fetch resource as child of parent resource");
                // TODO how to catch this?
            }
        }
        return retVal;
    }

    @Override
    public URI getResourceId(Object value) {
        URI retVal = null;
        if (value instanceof Resource) {
            retVal = ((Resource)value).getBlockId();
        }
        // Is this a resource wrapped as a list?
        else if (value instanceof List && ((List)value).size() == 2
                 && ((List)value).get(0) instanceof HashMap
                 && ((HashMap<String, Object>)((List)value).get(0)).containsKey(ParserConstants.JSONLD_ID)) {
            String id = (String)((HashMap<String, Object>)((List)value).get(0)).get(ParserConstants.JSONLD_ID);

            retVal = UriBuilder.fromUri(id).build();
        }
        // This is a reference to a resource
        else if (value instanceof Map && ((Map)value).containsKey(ParserConstants.JSONLD_ID)) {
            retVal = UriBuilder.fromUri((String) ((Map) value).get(ParserConstants.JSONLD_ID)).build();
        }
        return retVal;
    }


}
