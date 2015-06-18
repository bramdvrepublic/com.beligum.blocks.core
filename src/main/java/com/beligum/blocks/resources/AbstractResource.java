package com.beligum.blocks.resources;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.database.OBlocksDatabase;
import com.beligum.blocks.resources.interfaces.Node;
import com.beligum.blocks.resources.interfaces.Resource;
import com.beligum.blocks.resources.jackson.ResourceJsonDeserializer;
import com.beligum.blocks.resources.jackson.ResourceJsonSerializer;
import com.beligum.blocks.utils.RdfTools;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.*;

/**
 * Created by wouter on 3/06/15.
 */
public abstract class AbstractResource extends AbstractNode implements Resource
{

    @Override
    public URI getBlockId()
    {
        return UriBuilder.fromUri(getFieldDirect(ParserConstants.JSONLD_ID).asString()).build();
    }
    @Override
    public Node getRdfType()
    {
        return getDatabase().createNode(getFieldDirect(OBlocksDatabase.RESOURCE_TYPE_FIELD), Locale.ROOT);
    }

    @Override
    public void setRdfType(Node node)
    {
        List list = new ArrayList();
        for (Node n: node) {
            list.add(n);
        }
        this.setFieldDirect(OBlocksDatabase.RESOURCE_TYPE_FIELD, node.getValue(), node.getLanguage());
    }

    @Override
    public void add(URI field, Node node)
    {
        String key = addFieldToContext(field);
        addFieldDirect(key, node);
    }

    @Override
    public void set(URI field, Node node)
    {
        // TODO special case for @Type

        String key = addFieldToContext(field);
        setFieldDirect(key, node.getValue(), node.getLanguage());
    }

    @Override
    public Node get(URI field)
    {
        String key = RdfTools.makeDbFieldFromUri(field);

        return getFieldDirect(key);
    }

    @Override
    public Node get(String field)
    {
        URI key = UriBuilder.fromUri(field).build();
        return get(key);
    }

    @Override
    public Node remove(URI field)
    {
        Node retVal = null;
        String key = RdfTools.makeDbFieldFromUri(field);
        retVal = removeFieldDirect(key);
        removeFieldFromContext(key);


        return retVal;
    }

    @Override
    public boolean isEmpty()
    {
        return getFields().size() > 0;
    }


    public abstract void setFieldDirect(String key, Object value, Locale locale);

    public abstract Node getFieldDirect(String key);

    public abstract void addFieldDirect(String key, Node value);

    public abstract Node removeFieldDirect(String key);

    @Override
    public void merge(Resource resource)
    {

    }

    @Override
    public HashMap<String, String> getContext() {
        HashMap<String, String> retVal = (HashMap<String, String>)getFieldDirect(ParserConstants.JSONLD_CONTEXT).getValue();
        if (retVal == null) {
            retVal = new HashMap<String, String>();
            this.setFieldDirect(ParserConstants.JSONLD_CONTEXT, retVal, Locale.ROOT);
        }

        return retVal;
    }


    protected void removeFieldFromContext(String shortFieldName)
    {

        HashMap<String, String> context = this.getContext();
        if (context.containsKey(shortFieldName)) {
            context.remove(shortFieldName);
        }
    }

    protected String addFieldToContext(URI field)
    {

        // Create a short field name
        String shortFieldName = RdfTools.makeDbFieldFromUri(field);

        HashMap<String, String> context = this.getContext();
        if (!context.containsKey(shortFieldName)) {
            context.put(shortFieldName, field.toString());
        }
        return shortFieldName;
    }

    @Override
    public String toJson()
    {
        String retVal = null;
        // Jackson mapper that can serialize Resource objects
        // TODO move mapper to other class
        final SimpleModule module = new SimpleModule("customerSerializationModule", new Version(1, 0, 0, "static version"));
        module.addSerializer(Resource.class, new ResourceJsonSerializer());
        module.addDeserializer(Resource.class, new ResourceJsonDeserializer());

        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(module);
        try {
            retVal = objectMapper.writeValueAsString(this);
        } catch (Exception e) {
            Logger.error("Could not serialize resource.");
        }
        return retVal;
    }


    // ------- Methods from Node interface that are less relevant for Resource -----

    @Override
    public boolean isResource()
    {
        return true;
    }

    @Override
    public boolean isNull()
    {
        return false;
    }
    @Override
    public String asString()
    {
        return super.asString();
    }

    @Override
    public Double getDouble()
    {
        return null;
    }
    @Override
    public Integer getInteger()
    {
        return null;
    }
    @Override
    public Boolean getBoolean()
    {
        return null;
    }
    @Override
    public Long getLong()
    {
        return null;
    }


}