package com.beligum.blocks.models.resources;

import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.models.resources.interfaces.Node;
import com.beligum.blocks.models.resources.interfaces.Resource;
import com.beligum.blocks.models.resources.orient.OrientResourceController;
import com.beligum.blocks.utils.RdfTools;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.*;

/**
 * Created by wouter on 3/06/15.
 */
public abstract class AbstractResource extends AbstractNode implements Resource
{

    @Override
    public String getBlockId()
    {
        return getFieldDirect(ParserConstants.JSONLD_ID).asString();
    }
    @Override
    public Node getRdfType()
    {
        return getResourceController().asNode(getFieldDirect(OrientResourceController.TYPE_FIELD), Locale.ROOT);
    }
    @Override
    public void setRdfType(Node node)
    {
        List list = new ArrayList();
        for (Node n: node) {
            list.add(n);
        }
        this.setFieldDirect(OrientResourceController.TYPE_FIELD, node.getValue(), node.getLanguage());
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
