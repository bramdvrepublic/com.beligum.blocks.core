package com.beligum.blocks.models.resources.map;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.models.resources.interfaces.Node;
import com.beligum.blocks.models.resources.interfaces.Resource;
import com.beligum.blocks.utils.UrlTools;

import java.util.*;

/**
 * Created by wouter on 31/05/15.
 */
public class MapResource extends MapNode implements Resource
{
    protected static String DB_ID;
    protected Map<String, Object> resource;



    protected MapResource(Map<String, Object> vertex, Locale locale) {
        this.resource = vertex;
    }

    // -------- GETTERS AND SETTERS ---------
    @Override
    public Object getDBId()
    {
        return resource.get(DB_ID);
    }

    @Override
    public String getBlockId()
    {
        return (String)this.resource.get(ParserConstants.JSONLD_ID);
    }
    @Override
    public Node getRdfType()
    {
        return get(ParserConstants.JSONLD_TYPE);
    }
    @Override
    public void setRdfType(Node node)
    {
        set(ParserConstants.JSONLD_TYPE, node);
    }



    // ------- FUNCTIONS FOR RESOURCE INTERFACE ------

    @Override
    public void add(String key, Node node)
    {
        try {
            key = addFieldToContext(key);

            if (!node.isNull()) {
                if (node.getLanguage().getLanguage().equals(this.language.getLanguage()) || node.getLanguage().equals(Locale.ROOT)) {
                    // add to default
                    if (this.resource.containsKey(key)) {

                        Object value = this.resource.get(key);
                        List newValue = null;
                        if (value instanceof List) {
                            newValue = (List)value;
                        } else {
                            newValue = new ArrayList();
                            newValue.add(value);
                        }

                        for (Node n: node) {
                            newValue.add(n.getValue());
                        }
                        this.resource.put(key, newValue);
                    } else {
                        this.resource.put(key, node.getValue());
                    }
                }
                else {
                    Logger.error("Node has wrong language. Could not add to resource.");
                }
            }

        } catch (Exception e) {
            Logger.error("Could not set field of resource. Fieldname is invalid: " + key);
        }
    }


    @Override
    public void set(String key, Node node)
    {
        // TODO special case for @Type
        try {
            key = addFieldToContext(key);

            if (!node.isNull()) {
                if (node.isResource()) {
                    ArrayList<Map> list = new ArrayList<>();
                    list.add((Map)node.getValue());
                    this.resource.put(key, list);
                } else if (node.getLanguage().equals(Locale.ROOT) ||node.getLanguage().getLanguage().equals(this.language.getLanguage())) {
                    this.resource.put(key, node.getValue());
                } else {
                    Logger.error("Node has wrong language. Could not add to resource.");
                    //                    Resource otherLocalized = OrientResourceFactory.instance().createResource(this.getBlockId(), node.getLanguage());
                    //                    otherLocalized.set(key, node);
                }
            }
        } catch (Exception e) {
            Logger.error("Could not set field of resource. Fieldname is invalid: " + key);
        }
    }


    @Override
    public Node get(String key)
    {
        Node retVal = null;
        try {

            key = getShortFieldName(key);
            Locale lang = this.getLanguage();
            Object fieldValue = this.resource.get(key);


            retVal = MapResourceFactory.instance().asNode(fieldValue, lang);
        } catch (Exception e) {
            Logger.error("Could not find value for field. Invalid fieldname: " + key);
            retVal = MapResourceFactory.instance().asNode(null, this.getLanguage());
        }
        return retVal;
    }

    @Override
    public boolean isEmpty()
    {
        return getFields().size() > 0;
    }

    @Override
    public Node remove(String key)
    {
        Node retVal = null;
        Locale lang = this.getLanguage();
        try {
            key = getShortFieldName(key);
            Object fieldValue = this.resource.remove(key);
            removeFieldFromContext(key);
            retVal = MapResourceFactory.instance().asNode(fieldValue, lang);
        } catch(Exception e) {
            Logger.error("Could not remove value from resource: invalid fieldname: " + key);
            retVal = MapResourceFactory.instance().asNode(null, lang);
        }
        return retVal;
    }


    @Override
    public Set<String> getFields()
    {
        Set<String> retVal = new HashSet();
        if (this.isResource()) {
            for (String fieldName: this.resource.keySet()) {
                if (isPlainField(fieldName)) {
                    retVal.add(fieldName);
                }
            }
        }
        return retVal;
    }


    @Override
    public Resource copy()
    {
        return null;
    }

    @Override
    public void wrap(Resource resource)
    {

    }
    @Override
    public void merge(Resource resource)
    {

    }

    // ------- PRIVATE METHODS --------

    private boolean isPlainField(String fieldName) {
        boolean retVal = true;
        if (fieldName.equals(ParserConstants.JSONLD_ID) || fieldName.equals(ParserConstants.JSONLD_TYPE) || fieldName.equals(ParserConstants.JSONLD_CONTEXT)) {
            retVal = false;
        }
        return retVal;
    }


    private String addFieldToContext(String field) throws Exception
    {
        // Create a context for this resource
        if (this.resource.get(ParserConstants.JSONLD_CONTEXT) == null) {
            this.resource.put(ParserConstants.JSONLD_CONTEXT, new HashMap<String, String>());
        }

        // Create a short field name
        String shortFieldName = getShortFieldName(field);
        if (field.equals(shortFieldName)) {
            field = UrlTools.createLocalType(shortFieldName);
        }

        HashMap<String, String> context = (HashMap<String, String>)this.resource.get(ParserConstants.JSONLD_CONTEXT);
        if (!context.containsKey(shortFieldName)) {
            context.put(shortFieldName, field);
        } else if (!context.get(shortFieldName).equals(field)) {
            throw new Exception("A similar fieldname already exists");
        }
        return shortFieldName;
    }

    private void removeFieldFromContext(String shortFieldName) throws Exception
    {
        // Create a context for this resource
        if (this.resource.get(ParserConstants.JSONLD_CONTEXT) == null) {
            this.resource.put(ParserConstants.JSONLD_CONTEXT, new HashMap<String, String>());
        }

        HashMap<String, String> context = (HashMap<String, String>)this.resource.get(ParserConstants.JSONLD_CONTEXT);
        if (context.containsKey(shortFieldName)) {
            context.remove(shortFieldName);
        }
    }

    private String getShortFieldName(String field) throws Exception
    {
        String retVal = field;
        int index = Math.max(Math.max(field.lastIndexOf("/"), field.lastIndexOf("#")), field.lastIndexOf(":"));

        if (field.startsWith("http://")) {
            retVal = field.substring(index+1);
        } else if (index > 0){
            throw new Exception("The fieldname is not valid");
        }

        if (retVal.contains(".")) {
            throw new Exception("The fieldname is not valid");
        }

        return retVal;
    }
}
