package com.beligum.blocks.models.resources.orient;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.models.resources.interfaces.Node;
import com.beligum.blocks.models.resources.interfaces.Resource;
import com.beligum.blocks.utils.UrlTools;
import com.orientechnologies.orient.core.db.record.ORecordElement;
import com.orientechnologies.orient.core.record.impl.ODocument;

import java.util.*;

/**
 * Created by wouter on 13/05/15.
 **/
public class OrientResource extends OrientNode implements Resource
{




    // The default resource that this localized resource translates
    // if not this is corrected on save.
    protected ODocument defaultResource;
    // The translated fields of this resource
    protected ODocument localizedResource;

    protected HashMap<String, HashMap<String, String>> context;


    // Resource can only be created by resourceFactory
    public OrientResource(ODocument defaultVertex, ODocument localizedResource) {
        if (localizedResource != null) {
            this.language = BlocksConfig.instance().getLocaleForLanguage((String) localizedResource.field(ParserConstants.JSONLD_LANGUAGE));
        } else {
            this.language = Locale.ROOT;
        }

        this.localizedResource = localizedResource;
        this.defaultResource = defaultVertex;
    }

    // -------- GETTERS AND SETTERS ---------
    @Override
    public Object getDBId()
    {
        return defaultResource.getIdentity().toString();
    }

    @Override
    public String getBlockId()
    {
        return this.defaultResource.field(ParserConstants.JSONLD_ID);
    }
    @Override
    public Node getRdfType()
    {
        return get(OrientResourceFactory.TYPE_FIELD);
    }
    @Override
    public void setRdfType(Node node)
    {
        set(OrientResourceFactory.TYPE_FIELD, node);
    }
    @Override
    public Locale getLanguage()
    {
        Locale retVal = this.language;
        if (retVal == null) {
            if (localizedResource == null) {
                retVal = Locale.ROOT;
            } else {
                retVal = BlocksConfig.instance().getLocaleForLanguage((String) localizedResource.field(ParserConstants.JSONLD_LANGUAGE));
            }
        }
        return retVal;
    }


    // ------- FUNCTIONS FOR RESOURCE INTERFACE ------

    @Override
    public void add(String key, Node node)
    {
        try {
            key = addFieldToContext(key);

            if (!node.isNull()) {
                if (node.isResource()) {
                    add(this.defaultResource, key, node);
                }
                else {
                    if (this.localizedResource != null && node.getLanguage().getLanguage().equals(this.language.getLanguage())) {
                        // add to default
                        add(this.localizedResource, key, node);
                    }
                    else if (node.getLanguage().getLanguage().equals(Locale.ROOT.getLanguage())) {
                        // add to translation
                        add(this.defaultResource, key, node);
                    }
                    else {
                        Logger.error("Node has wrong language. Could not add to resource.");
//                        Resource otherLocalized = OrientResourceFactory.instance().createResource(this.getBlockId(), node.getLanguage());
//                        otherLocalized.set(key, node);
                    }
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
                ODocument vertex = defaultResource;
                if (node.isResource()) {
                    ArrayList<ODocument> list = new ArrayList<>();
                    list.add((ODocument)node.getValue());
                    vertex.field(key, list);
                } else if (this.localizedResource != null && node.getLanguage().getLanguage().equals(this.language.getLanguage())) {
                    vertex = this.localizedResource;
                    vertex.field(key, node.getValue());
                } else if (node.getLanguage().equals(Locale.ROOT)) {
                    vertex.field(key, node.getValue());
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
            Object fieldValue = null;
            if (this.localizedResource != null) {
                fieldValue = this.localizedResource.field(key);
            }

            if (fieldValue == null) {
                fieldValue = this.defaultResource.field(key);
                // If this value is not an other resource then it has no language because
                // it sits in the defaultResource
                if (!(fieldValue instanceof ORecordElement)) {
                    lang = Locale.ROOT;
                }
            }

            retVal = OrientResourceFactory.instance().asNode(fieldValue, lang);
        } catch (Exception e) {
            Logger.error("Could not find value for field. Invalid fieldname: " + key);
            retVal = OrientResourceFactory.instance().asNode(null, this.getLanguage());
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
            Object fieldValue = null;
            if (this.localizedResource != null) {
                fieldValue = this.localizedResource.field(key);
            }

            if (fieldValue == null) {
                fieldValue = this.defaultResource.field(key);
                lang = Locale.ROOT;
                if (fieldValue != null) {
                    fieldValue = this.defaultResource.removeField(key);
                }
            }
            else {
                fieldValue = this.localizedResource.removeField(key);
            }
            removeFieldFromContext(key);
            retVal = OrientResourceFactory.instance().asNode(fieldValue, lang);
        } catch(Exception e) {
            Logger.error("Could not remove value from resource: invalid fieldname: " + key);
            retVal = OrientResourceFactory.instance().asNode(null, lang);
        }
        return retVal;
    }


    @Override
    public Set<String> getFields()
    {
        Set<String> retVal = new HashSet();
        if (this.isResource()) {
            for (String fieldName: this.defaultResource.fieldNames()) {
                if (isPlainField(fieldName)) {
                    retVal.add(fieldName);
                }
            }
            if (this.localizedResource != null) {
                for (String fieldName: this.localizedResource.fieldNames()) {
                    if (isPlainField(fieldName)) {
                        retVal.add(fieldName);
                    }
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

    // -------- PRIVATE METHODS ---------

    private void add(ODocument vertex, String key, Node node)
    {
        Object existingField = vertex.field(key);
        if (!node.isNull()) {
            if (existingField == null) {
                if (node.isResource()) {
                    List<Object> t = new ArrayList<Object>();
                    t.add(node.getValue());
                    vertex.field(key, t);
                } else {
                    vertex.field(key, node.getValue());
                }
            }
            else if (existingField instanceof List) {
                List valueList = ((List) existingField);
                if (node.isIterable()) {
                    for (Node val: node)
                        valueList.add(val.getValue());
                } else {
                    valueList.add(node.getValue());
                }
            }
            else {
                List newValues = new ArrayList();
                newValues.add(existingField);
                newValues.add(node.getValue());
                vertex.field(key, newValues);
            }
        }
    }

    private boolean isPlainField(String fieldName) {
        boolean retVal = true;
        if (fieldName.equals(ParserConstants.JSONLD_ID) || fieldName.equals(OrientResourceFactory.TYPE_FIELD) || fieldName.equals(ParserConstants.JSONLD_CONTEXT)) {
            retVal = false;
        } else if (fieldName.startsWith(OrientResourceFactory.LOCALIZED)) {
            retVal = false;
        }
        return retVal;
    }


    private String addFieldToContext(String field) throws Exception
    {
        // Create a context for this resource
        if (this.defaultResource.field(ParserConstants.JSONLD_CONTEXT) == null) {
            this.defaultResource.field(ParserConstants.JSONLD_CONTEXT, new HashMap<String, String>());
        }

        // Create a short field name
        String shortFieldName = getShortFieldName(field);
        if (field.equals(shortFieldName)) {
            field = UrlTools.createLocalType(shortFieldName);
        }

        HashMap<String, String> context = this.defaultResource.field(ParserConstants.JSONLD_CONTEXT);
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
        if (this.defaultResource.field(ParserConstants.JSONLD_CONTEXT) == null) {
            this.defaultResource.field(ParserConstants.JSONLD_CONTEXT, new HashMap<String, String>());
        }

        HashMap<String, String> context = this.defaultResource.field(ParserConstants.JSONLD_CONTEXT);
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

    @Override
    public Object getValue()
    {
        return this.defaultResource;
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
        return null;
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
