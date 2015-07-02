//package com.beligum.blocks.resources;
//
//import com.beligum.base.utils.Logger;
//import com.beligum.blocks.config.BlocksConfig;
//import com.beligum.blocks.config.ParserConstants;
//import com.beligum.blocks.database.OBlocksDatabase;
//import com.beligum.blocks.database.interfaces.BlocksDatabase;
//import com.beligum.blocks.resources.interfaces.Node;
//import com.orientechnologies.orient.core.db.record.ORecordElement;
//import com.orientechnologies.orient.core.record.impl.ODocument;
//
//import javax.ws.rs.core.UriBuilder;
//import java.net.URI;
//import java.util.*;
//
///**
// * Created by wouter on 13/05/15.
// **/
//public class OrientResource extends AbstractResource
//{
//
//    // The default resource that this localized resource translates
//    // if not this is corrected on save.
//    protected ODocument defaultResource;
//    // The translated fields of this resource
//    protected ODocument localizedResource;
//
//
//    // Resource can only be created by resourceFactory
//    public OrientResource(ODocument defaultVertex, ODocument localizedResource) {
//        if (localizedResource != null) {
//            this.getLanguage = BlocksConfig.instance().getLocaleForLanguage((String) localizedResource.field(ParserConstants.JSONLD_LANGUAGE));
//        } else {
//            this.getLanguage = Locale.ROOT;
//        }
//
//        this.localizedResource = localizedResource;
//        this.defaultResource = defaultVertex;
//    }
//
//    // -------- GETTERS AND SETTERS ---------
//    @Override
//    public Object getDBId()
//    {
//        return defaultResource.getIdentity().toString();
//    }
//
//    @Override
//    public Locale getLanguage()
//    {
//        Locale retVal = this.getLanguage;
//        if (retVal == null) {
//            if (localizedResource == null) {
//                retVal = Locale.ROOT;
//            } else {
//                retVal = BlocksConfig.instance().getLocaleForLanguage((String) localizedResource.field(ParserConstants.JSONLD_LANGUAGE));
//            }
//        }
//        return retVal;
//    }
//
//    // ------- FUNCTIONS FOR RESOURCE INTERFACE ------
//
//    @Override
//    public void addFieldDirect(String key, Node node) {
//        if (!node.isNull()) {
//            if (node.isResource()) {
//                add(this.defaultResource, key, node);
//            }
//            else {
//                if (this.localizedResource != null && node.getLanguage().getLanguage().equals(this.getLanguage.getLanguage())) {
//                    // add to default
//                    add(this.localizedResource, key, node);
//                }
//                else if (node.getLanguage().getLanguage().equals(Locale.ROOT.getLanguage())) {
//                    // add to translation
//                    add(this.defaultResource, key, node);
//                }
//                else {
//                    Logger.error("Node has wrong getLanguage. Could not add to resource.");
//                    //                        Resource otherLocalized = OrientResourceFactory.instance().createResource(this.getBlockId(), node.getLanguage());
//                    //                        otherLocalized.set(key, node);
//                }
//            }
//        }
//    }
//
//    @Override
//    public void setFieldDirect(String key, Object value, Locale locale) {
//        if (value != null) {
//            ODocument vertex = defaultResource;
//            if (value instanceof ODocument) {
//                ArrayList<ODocument> list = new ArrayList<>();
//                list.add((ODocument)value);
//                vertex.field(key, list);
//            } else if (this.localizedResource != null && locale.getLanguage().equals(this.getLanguage.getLanguage())) {
//                vertex = this.localizedResource;
//                vertex.field(key, value);
//            } else if (locale.equals(Locale.ROOT)) {
//                vertex.field(key, value);
//            } else {
//                Logger.error("Node has wrong getLanguage. Could not add to resource.");
//                //                    Resource otherLocalized = OrientResourceFactory.instance().createResource(this.getBlockId(), node.getLanguage());
//                //                    otherLocalized.set(key, node);
//            }
//        }
//    }
//
//    @Override
//    public Node getFieldDirect(String key) {
//        Locale lang = this.getLanguage();
//        Object fieldValue = null;
//        if (this.localizedResource != null) {
//            fieldValue = this.localizedResource.field(key);
//        }
//
//        if (fieldValue == null) {
//            fieldValue = this.defaultResource.field(key);
//            // If this value is not an other resource then it has no getLanguage because
//            // it sits in the defaultResource
//            if (!(fieldValue instanceof ORecordElement)) {
//                lang = Locale.ROOT;
//            }
//        }
//
//        return getDatabase().createNode(fieldValue, lang);
//    }
//
//    @Override
//    public Node removeFieldDirect(String key) {
//        Locale lang = this.getLanguage();
//        Object fieldValue = null;
//        if (this.localizedResource != null) {
//            fieldValue = this.localizedResource.field(key);
//        }
//
//        if (fieldValue == null) {
//            fieldValue = this.defaultResource.field(key);
//            lang = Locale.ROOT;
//            if (fieldValue != null) {
//                fieldValue = this.defaultResource.removeField(key);
//            }
//        }
//        else {
//            fieldValue = this.localizedResource.removeField(key);
//        }
//        return getDatabase().createNode(fieldValue, lang);
//    }
//
//
//    @Override
//    public Set<URI> getFields()
//    {
//        Set<String> fields = new HashSet();
//        Set<URI> retVal = new HashSet();
//        if (this.isResource()) {
//            fields.addAll(Arrays.asList(this.defaultResource.fieldNames()));
//
//            if (this.localizedResource != null) {
//                fields.addAll(Arrays.asList(this.localizedResource.fieldNames()));
//            }
//
//            for (String key: fields) {
//                String field = this.getContext().get(key);
//                if (field != null) {
//                    retVal.add(UriBuilder.fromUri(field).build());
//                }
//            }
//        }
//        return retVal;
//    }
//    @Override
//    public Set<URI> getLocalizedFields()
//    {
//        Set<String> fields = new HashSet();
//        Set<URI> retVal = new HashSet();
//        fields.addAll(Arrays.asList(this.localizedResource.fieldNames()));
//
//
//        for (String key: fields) {
//            String field = this.getContext().get(key);
//            if (field != null) {
//                retVal.add(UriBuilder.fromUri(field).build());
//            }
//        }
//
//        return retVal;
//    }
//    @Override
//    public Set<URI> getRootFields()
//    {
//        Set<String> fields = new HashSet();
//        Set<URI> retVal = new HashSet();
//        fields.addAll(Arrays.asList(this.defaultResource.fieldNames()));
//
//
//        for (String key: fields) {
//            String field = this.getContext().get(key);
//            if (field != null) {
//                retVal.add(UriBuilder.fromUri(field).build());
//            }
//        }
//
//        return retVal;
//    }
//
//    @Override
//    public BlocksDatabase getDatabase() {
//        return OBlocksDatabase.instance();
//    }
//
//
//    @Override
//    public void setCreatedAt(Calendar date)
//    {
//        this.localizedResource.field(OBlocksDatabase.RESOURCE_CREATED_AT, date.getTime());
//    }
//    @Override
//    public Calendar getCreatedAt()
//    {
//        Calendar retVal = null;
//        Date date = this.localizedResource.field(OBlocksDatabase.RESOURCE_CREATED_AT);
//        if (date != null) {
//            retVal = Calendar.getInstance();
//            retVal.setTime(date);
//        }
//        return retVal;
//    }
//
//    @Override
//    public void setCreatedBy(String user)
//    {
//        this.localizedResource.field(OBlocksDatabase.RESOURCE_CREATED_BY, user);
//    }
//
//    @Override
//    public String getCreatedBy()
//    {
//        return this.localizedResource.field(OBlocksDatabase.RESOURCE_CREATED_BY);
//    }
//
//    @Override
//    public void setUpdatedAt(Calendar date)
//    {
//        this.localizedResource.field(OBlocksDatabase.RESOURCE_UPDATED_AT, date.getTime());
//    }
//
//    @Override
//    public Calendar getUpdatedAt()
//    {
//        Calendar retVal = null;
//        Date date = this.localizedResource.field(OBlocksDatabase.RESOURCE_UPDATED_AT);
//        if (date != null) {
//            retVal = Calendar.getInstance();
//            retVal.setTime(date);
//        }
//        return retVal;
//    }
//
//    @Override
//    public void setUpdatedBy(String user)
//    {
//        this.localizedResource.field(OBlocksDatabase.RESOURCE_UPDATED_BY, user);
//    }
//
//    @Override
//    public String getUpdatedBy()
//    {
//        return this.localizedResource.field(OBlocksDatabase.RESOURCE_UPDATED_BY);
//    }
//
//
//    // -------- PRIVATE METHODS ---------
//
//    private void add(ODocument vertex, String key, Node node)
//    {
//        Object existingField = vertex.field(key);
//        if (!node.isNull()) {
//            if (existingField == null) {
//                if (node.isResource()) {
//                    List<Object> t = new ArrayList<Object>();
//                    t.add(node.getValue());
//                    vertex.field(key, t);
//                } else {
//                    vertex.field(key, node.getValue());
//                }
//            }
//            else if (existingField instanceof List) {
//                List valueList = ((List) existingField);
//                if (node.isIterable()) {
//                    for (Node val: node)
//                        valueList.add(val.getValue());
//                } else {
//                    valueList.add(node.getValue());
//                }
//            }
//            else {
//                List newValues = new ArrayList();
//                newValues.add(existingField);
//                newValues.add(node.getValue());
//                vertex.field(key, newValues);
//            }
//        }
//    }
//
//    private boolean isPlainField(String fieldName) {
//        boolean retVal = true;
//        if (fieldName.equals(ParserConstants.JSONLD_ID) || fieldName.equals(OBlocksDatabase.RESOURCE_TYPE_FIELD) || fieldName.equals(ParserConstants.JSONLD_CONTEXT)) {
//            retVal = false;
//        } else if (fieldName.startsWith(OBlocksDatabase.RESOURCE_LOCALIZED_FIELD)) {
//            retVal = false;
//        }
//        return retVal;
//    }
//
//    @Override
//    public Object getValue()
//    {
//        return this.defaultResource;
//    }
//
//
//
//
//}
