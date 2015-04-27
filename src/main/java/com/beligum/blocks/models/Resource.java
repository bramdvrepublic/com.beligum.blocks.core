package com.beligum.blocks.models;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.ParserConstants;
import com.hp.hpl.jena.vocabulary.RDFS;

import java.util.*;

/**
 * Created by wouter on 17/04/15.
 *
 * This class wraps a expanded Resource HashMap(Json-Ld)
 */
public class Resource
{
//    public static final String ID_PROPERTY = "@id";
//    public static final String LANGUAGE_PROPERTY = "@language";
//    public static final String TYPE_PROPERTY = "@type";
//    public static final String VALUE_PROPERTY = "@value";
//
//    private HashMap<String, Object> wrappedResource = new HashMap<String, Object>();
//    private HashMap<String, Integer> propertyCounter = new HashMap<>();
//    private ResourceContext model;
//
//    public Resource() {
//        this.model = new ResourceContext();
//    }
//
//    public Resource (HashMap<String, Object> resource, ResourceContext model) {
//
//        HashMap<String, String> context = model.getContext();
//        if (resource == null) {
//            wrappedResource = new HashMap<String, Object>();
//        } else {
//            this.wrappedResource = resource;
//        }
//        if (model == null) {
//            this.model = new ResourceContext(this);
//        }
//            this.model = model;
//    }
//
//    public void setModel(ResourceContext model) {
//        this.model = model;
//    }
//
//    public void setWrappedResource(HashMap<String, Object> resource) {
//        this.wrappedResource = resource;
//    }
//
//    // add resource to graph and return reference
//
//
//    public void addProperty(String name, Resource resource) {
//        addProperty(name, resource.wrappedResource);
//        resource.setModel(this.getModel());
//    }
//
////    public void addProperty(String name, Object value) {
////        if (!this.wrappedResource.containsKey(name)) {
////            this.wrappedResource.put(name, value);
////        } else if (this.wrappedResource.get(name) instanceof List) {
////            if (value instanceof List) {
////                ((List<Object>)this.wrappedResource.get(name)).addAll((List<Object>) value);
////            } else {
////                ((List<Object>)this.wrappedResource.get(name)).add(value);
////            }
////        } else {
////            ArrayList<Object> wrappingList = new ArrayList<>();
////            wrappingList.add(this.wrappedResource.get(name));
////            if (value instanceof List) {
////                wrappingList.addAll((List<Object>) value);
////            } else {
////                wrappingList.add(value);
////            }
////            this.wrappedResource.put(name, wrappingList);
////        }
////    }
////
////
//
////    public String getProperty(String name) {
////        Object value = wrappedResource.get(name);
////        String retVal = null;
////        if (value instanceof String && getPropertyIndex(name) == 0) {
////            retVal = (String)value;
////        } else if (value instanceof Map && getPropertyIndex(name) == 0) {
////            retVal = getMappedPropertyValue((Map<String, Object>)value);
////        } else if (value instanceof List) {
////            if (((List)value).size() > getPropertyIndex(name)) {
////                value = ((List)value).get(getPropertyIndex(name));
////            }
////            if (value instanceof String) {
////                retVal = (String)value;
////            } else {
////                retVal = getMappedPropertyValue((Map<String, Object>)value);
////            }
////
////        }
////        return retVal;
////    }
//
//    public Resource getResource(String name) {
//
//        Object value = wrappedResource.get(name);
//        Resource retVal = null;
//        if (value instanceof String && getPropertyIndex(name) == 0) {
//            retVal = this.model.getResource((String)value);
//        } else if (value instanceof Map && getPropertyIndex(name) == 0) {
//            retVal = getMappedResourceValue((Map<String, Object>) value);
//        } else if (value instanceof List) {
//            if (((List)value).size() > getPropertyIndex(name)) {
//                value = ((List)value).get(getPropertyIndex(name));
//            }
//            retVal = getMappedResourceValue((Map<String, Object>) value);
//        }
//        return retVal;
//    }
//
//    public Resource getResourceAtIndex(String name, int index) {
//
//        Resource retVal = null;
//        if (index == 0) {
//            retVal = getResource(name);
//        } else {
//            Object value = wrappedResource.get(name);
//            if (value instanceof List) {
//                if (((List) value).size() > index) {
//                    value = ((List) value).get(index);
//                }
//                retVal = getMappedResourceValue((Map<String, Object>) value);
//            }
//        }
//        return retVal;
//    }
//
//
//    public Integer getPropertyValueCount(String property) {
//        Object value = wrappedResource.get(property);
//        Integer retVal = null;
//        if (value == null) {
//            retVal = 0;
//        } else if (value instanceof List) {
//            retVal = ((List)value).size();
//        } else {
//            retVal = 1;
//        }
//        return retVal;
//    }
//
//    private String getMappedPropertyValue(Map<String, Object> value) {
//        String retVal = null;
//        if (value.containsKey(Resource.VALUE_PROPERTY)) {
//            retVal = (String)value.get(Resource.VALUE_PROPERTY);
//        } else {
//            Logger.debug("This is probably a resource. Fetch with getResource()");
//        }
//        return retVal;
//    }
//
//    private Resource getMappedResourceValue(Map<String, Object> value) {
//        Resource retVal = null;
//        if (value.containsKey(Resource.ID_PROPERTY)) {
//            retVal = this.model.getResource((String)value.get(Resource.ID_PROPERTY));
//        } else {
//            Logger.debug("This is probably a property. Fetch with getProperty()");
//        }
//        return retVal;
//    }
//
//    public void wrap(Resource resource) {
//        this.wrappedResource = resource.wrappedResource;
//        this.model = resource.model;
//    }
//
//    public void copy(Resource resource) {
//        this.wrappedResource = this.doCopyMap(resource.wrappedResource);
//        this.model = resource.model;
//    }
//
//    public HashMap<String, Object> getWrappedResource() {
//        return this.wrappedResource;
//    }
//
//    public void setId(String id) {
//        this.wrappedResource.put(ParserConstants.JSONLD_ID, id);
//    }
//
//    public String getId() {
//        return (String)this.wrappedResource.get(ParserConstants.JSONLD_ID);
//    }
//
//    public String setType(String type) {
//        return (String)this.wrappedResource.put(ParserConstants.JSONLD_TYPE, type);
//    }
//
//    public String getType() {
//        return (String)this.wrappedResource.get(ParserConstants.JSONLD_TYPE);
//    }
//
//    public List getPropertyList(String key) {
//        List<HashMap<String, Object>> retVal = null;
//        if (wrappedResource.containsKey(key)) {
//            retVal = (List<HashMap<String, Object>>) wrappedResource.get(key);
//        }
//        return retVal;
//    }
//
//
//    public HashMap<String, Object> getProperty(String key) {
//        HashMap<String, Object> retVal = null;
//        try {
//            List<HashMap<String, Object>> t = getPropertyList(key);
//            retVal = t.get(0);
//
//        } catch (Exception e) {
//            Logger.debug("Reading unknown property from expanded Json-Ld Object: expected list with hashMap");
//        }
//        return retVal;
//    }
//
//    public HashMap<String, Object> getPropertyAtIndex(String key, int index) {
//        HashMap<String, Object> retVal = null;
//        try {
//            List<HashMap<String, Object>>  list = getPropertyList(key);
//            if (list != null && list.size() <= index) {
//                retVal = list.get(index);
//            }
//
//        } catch (Exception e) {
//            Logger.debug("Reading unknown property from expanded Json-Ld Object: expected list with hashMap");
//        }
//        return retVal;
//    }
//
//    public void addPropertyValue(String key, String value, String language, String type) {
//        List<HashMap<String, Object>>  list = getPropertyList(key);
//        if (list == null) {
//            list = new ArrayList<HashMap<String, Object>>();
//        }
//        HashMap<String, Object> item= new HashMap();
//        item.put(ParserConstants.JSONLD_VALUE, value);
//        if (language != null) {
//            item.put(ParserConstants.JSONLD_LANGUAGE, language);
//        }
//        if (type != null) {
//            item.put(ParserConstants.JSONLD_TYPE, type);
//        }
//        list.add(item);
//    }
//
//    private Object fixResource(Object resource) {
//        Object retVal = resource;
//        if (resource instanceof Map && !((Map)resource).containsKey(Resource.VALUE_PROPERTY)) {
//            if (!((Map)resource).containsKey(Resource.ID_PROPERTY)) {
//                String id = this.model.getNextBlankNodeID();
//                ((Map)resource).put(Resource.ID_PROPERTY, id);
//                HashMap<String, Object> proxyResource = new HashMap<>();
//                proxyResource.put(Resource.ID_PROPERTY, id);
//                retVal = proxyResource;
//                this.model.getGraph().add((HashMap<String, Object>)resource);
//            }
//        }
//        return retVal;
//    }
//
//
//    public void addProperty(String key, HashMap<String, Object> property) {
//        fixResource(property);
//        List<HashMap<String, Object>>  list = getPropertyList(key);
//        if (list == null) {
//            list = new ArrayList<HashMap<String, Object>>();
//            this.wrappedResource.put(key, list);
//        }
//        list.add(property);
//    }
//
//    public void setProperty(String key, int index, HashMap<String, Object> property) {
//        fixResource(property);
//        List<HashMap<String, Object>>  list = getPropertyList(key);
//        if (list == null) {
//            list = new ArrayList<HashMap<String, Object>>();
//        }
//        if (list.size() > index) {
//            list.set(index, property);
//        } else {
//            list.add(property);
//        }
//    }
//
//    public void setPropertyValue(String key, int index, Object value, String language, String type) {
//
//        List<HashMap<String, Object>>  list = getPropertyList(key);
//        if (list == null) {
//            list = new ArrayList<HashMap<String, Object>>();
//            this.wrappedResource.put(key, list);
//        }
//        HashMap<String, Object> item= new HashMap();
//        item.put(ParserConstants.JSONLD_VALUE, value);
//        if (language != null) {
//            item.put(ParserConstants.JSONLD_LANGUAGE, language);
//        }
//        if (type != null) {
//            item.put(ParserConstants.JSONLD_TYPE, type);
//        }
//        if (list.size() <= index) {
//            list.add(item);
//        } else {
//            list.set(index, item);
//        }
//    }
//
//    public void setPropertyValue(String key, Object value, String language, String type) {
//        setPropertyValue(key, 0, value, language, type);
//    }
//
//    public void setPropertyValue(String key, Object value) {
//        setPropertyValue(key, 0, value, null, null);
//    }
//
//    public void setPropertyValue(String key, Object value, String language) {
//        setPropertyValue(key, 0, value, language, null);
//    }
//
//    public Boolean getBooleanPropertyValue(String key) {
//        Boolean retVal = false;
//        HashMap<String, Object> item = getProperty(key);
//        if (item != null && item.containsKey(ParserConstants.JSONLD_VALUE)) {
//            retVal = (Boolean)item.get(ParserConstants.JSONLD_VALUE);
//        }
//        return retVal;
//    }
//
//    public String getPropertyValue(String key) {
//        String retVal = null;
//        HashMap<String, Object> item = getProperty(key);
//        if (item != null && item.containsKey(ParserConstants.JSONLD_VALUE)) {
//            retVal = (String)item.get(ParserConstants.JSONLD_VALUE);
//        }
//        return retVal;
//    }
//
//    public ArrayList<String> getPropertyValues(String key) {
//        ArrayList<String> retVal = new ArrayList<>();
//        List<HashMap<String, Object>>  list = getPropertyList(key);
//        for (HashMap<String, Object> value: list) {
//            retVal.add((String) value.get(ParserConstants.JSONLD_VALUE));
//        }
//
//        return retVal;
//    }
//
//    public LinkedHashSet<String> getPropertyValuesSet(String key)
//    {
//        LinkedHashSet<String> retVal = new LinkedHashSet<String>();
//        List<HashMap<String, Object>> list = getPropertyList(key);
//        for (HashMap<String, Object> value : list) {
//            retVal.add((String) value.get(ParserConstants.JSONLD_VALUE));
//        }
//        return retVal;
//    }
//
//    public HashMap<String, Object> copy() {
//        return doCopyMap(this.getWrappedResource());
//
//    }
//
//    private HashMap<String, Object> doCopyMap(HashMap<String, Object> from) {
//        HashMap<String, Object> to = new HashMap<>();
//        for (String key: from.keySet()) {
//            if (from.get(key) instanceof List) {
//                to.put(key, doCopyList((List)from.get(key)));
//            } else if (from.get(key) instanceof List) {
//                to.put(key, doCopyMap((LinkedHashMap) from.get(key)));
//            } else {
//                to.put(key, from.get(key));
//            }
//        }
//        return to;
//    }
//
//    private List<Object> doCopyList(List<Object> from) {
//        List<Object> to = new ArrayList();
//        for (Object value: from) {
//            if (value instanceof Map) {
//                to.add(doCopyMap((HashMap<String, Object>) value));
//            } else if (value instanceof List) {
//                to.add(doCopyList((List) value));
//            } else {
//                to.add(value);
//            }
//        }
//        return to;
//    }
//
//
//    public Integer getPropertyIndex(String name) {
//        if (!propertyCounter.containsKey(name)) {
//            propertyCounter.put(name, 0);
//        }
//        return propertyCounter.get(name);
//    }
//
//    public Integer incrementPropertyIndex(String name) {
//        if (!propertyCounter.containsKey(name)) {
//            propertyCounter.put(name, 0);
//        }
//        return propertyCounter.put(name, propertyCounter.get(name) + 1);
//    }
//
//    public ResourceContext getModel() {
//        return this.model;
//    }
//
//
////    public static Resource createResource(HashMap<String, Object> resource, ResourceContext model) {
////        // create resource
////
////
//////
//////        for (String key: resource.keySet()) {
//////            Object propertyValue = resource.get(key);
//////            Object newKey = context.get(key);
//////            String absoluteKey = null;
//////            HashMap<String, Object> child = null;
//////
//////            if (newKey instanceof String) {
//////                absoluteKey = (String)newKey;
//////            } else if (newKey instanceof Map) {
//////                child = (HashMap<String, Object>)newKey;
//////                if (child.containsKey(Resource.ID_PROPERTY))  {
//////                    absoluteKey = (String)child.get(Resource.ID_PROPERTY);
//////                } else {
//////                    Logger.debug("Unknown key for value: key is map but no @id found");
//////                    continue;
//////                }
//////            } else if (newKey == null && key.equals(Resource.ID_PROPERTY) ) {
//////                absoluteKey = RDFS.Resource.toString();
//////            } else {
//////                Logger.debug("Unknown key for value: key not found in context");
//////                continue;
//////            }
//////
//////            if (propertyValue instanceof  String) {
//////
//////                    retVal.addProperty(absoluteKey, propertyValue);
//////
//////            } else if (propertyValue instanceof List) {
//////                List<Object> values = (List<Object>)propertyValue;
//////                ArrayList<Object> noLang = new ArrayList<>();
//////                ArrayList<Object> lang = new ArrayList<>();
//////                for (Object value: values) {
//////                    if (value instanceof String) {
//////                        noLang.add(value);
//////                    } else if (value instanceof Map && ((Map)value).get(Resource.LANGUAGE_PROPERTY) != null && ((Map)value).get(Resource.LANGUAGE_PROPERTY).equals(language)) {
//////                        lang.add(value);
//////                    }
//////                }
//////                int langSize = lang.size();
//////                int nolangSize = noLang.size();
//////                while(langSize < nolangSize) {
//////                    lang.add(noLang.get(langSize));
//////                    langSize++;
//////                }
//////                retVal.addProperty(absoluteKey, lang);
//////            } else if (propertyValue instanceof Map) {
//////                if (((Map)propertyValue).get(Resource.LANGUAGE_PROPERTY) != null && ((Map)propertyValue).get(Resource.LANGUAGE_PROPERTY).equals(language)) {
//////                    retVal.addProperty(absoluteKey, propertyValue);
//////                }
//////            } else {
//////                Logger.debug("Unknown property: skip property");
//////            }
//////
//////        }
////        return retVal;
////    }

}
