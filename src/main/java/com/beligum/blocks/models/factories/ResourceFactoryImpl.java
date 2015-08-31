package com.beligum.blocks.models.factories;

import com.beligum.base.server.RequestContext;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.controllers.PersistenceControllerImpl;
import com.beligum.blocks.models.ListNode;
import com.beligum.blocks.models.interfaces.ResourceFactory;
import com.beligum.blocks.models.NodeImpl;
import com.beligum.blocks.models.ResourceImpl;
import com.beligum.blocks.models.WebPageImpl;
import com.beligum.blocks.models.interfaces.WebPath;
import com.beligum.blocks.models.interfaces.Node;
import com.beligum.blocks.models.interfaces.Resource;
import com.beligum.blocks.models.interfaces.WebPage;
import com.beligum.blocks.models.jackson.NodeDeserializer;
import com.beligum.blocks.models.jackson.NodeSerializer;
import com.beligum.blocks.models.jackson.page.PageDeserializer;
import com.beligum.blocks.models.jackson.page.PageSerializer;
import com.beligum.blocks.models.jackson.path.PathDeserializer;
import com.beligum.blocks.models.jackson.path.PathSerializer;
import com.beligum.blocks.models.sql.DBPath;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.w3c.dom.NodeList;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by wouter on 6/07/15.
 */
public class ResourceFactoryImpl implements ResourceFactory
{

    private ObjectMapper resourceMapper;
    private ObjectMapper pageMapper;


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
    public WebPage createWebPage(URI id, Locale locale)
    {
        return new WebPageImpl(id, locale);
    }



    @Override
    public Resource createResource(URI id, URI rdfType, Locale language)
    {
        Resource retVal = new ResourceImpl(language);
        retVal.setBlockId(id);
        retVal.setRdfType(rdfType);

        return retVal;
    }

    @Override
    public Node createNode(Object value, Locale language)
    {
        Node retVal = null;
        if (value instanceof Resource || value instanceof Node) {
            retVal = (Node)value;
        } else {
            if (value instanceof Map && ((Map) value).containsKey(ParserConstants.JSONLD_VALUE)) {
                retVal = createNode(((Map) value).get(ParserConstants.JSONLD_VALUE), language);
            } else if (value instanceof List || value instanceof Set) {
                // TDOD create ListNode
                ListNode listNode = new ListNode(language);
                for (Object v: (Collection)value) {
                    Node node = createNode(value, language);
                    listNode.add(node);
                }
                retVal = listNode;
            } else {
                retVal = new NodeImpl(value, language);
            }
        }

        return retVal;
    }


    @Override
    public Resource deserializeResource(byte[] source, Locale locale) throws IOException
    {
        ObjectMapper mapper = getResourceMapper(false);
        Resource retVal = mapper.readValue(source, Resource.class);
        retVal.setLanguage(locale);
        return retVal;
    }

    @Override
    public WebPage deserializeWebpage(byte[] source, Locale locale) throws IOException
    {
        ObjectMapper mapper = getPageMapper(false);
        WebPage retVal = mapper.readValue(source, WebPage.class);
        retVal.setLanguage(locale);
        return retVal;
    }

    @Override
    public String serializeResource(Resource resource, boolean makeReferences) throws JsonProcessingException
    {
        ObjectMapper mapper = getResourceMapper(makeReferences);
        return mapper.writeValueAsString(resource);
    }

    @Override
    public String serializeWebpage(WebPage page, boolean makeReferences) throws JsonProcessingException
    {
        ObjectMapper mapper = getPageMapper(makeReferences);
        return mapper.writeValueAsString(page);
    }


//    ------------- PROTECTED METHODS

    protected ObjectMapper getResourceMapper(boolean makeReferences)
    {
        if (this.resourceMapper == null) {
            this.resourceMapper = new ObjectMapper().registerModule(new SimpleModule().addSerializer(Resource.class, new NodeSerializer()).addDeserializer(Resource.class, new NodeDeserializer()));;
        }
        return this.resourceMapper;
    }

    protected ObjectMapper getPageMapper(boolean makeReferences)
    {
        if (this.pageMapper == null) {
            this.pageMapper = new ObjectMapper().registerModule(new SimpleModule().addSerializer(WebPage.class, new PageSerializer<WebPage>()).addDeserializer(WebPage.class, new PageDeserializer()));;
        }
        return this.pageMapper;
    }


}
