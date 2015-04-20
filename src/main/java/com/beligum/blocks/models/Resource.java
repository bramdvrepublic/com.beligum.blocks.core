package com.beligum.blocks.models;

import com.beligum.base.utils.Logger;
import com.hp.hpl.jena.vocabulary.RDFS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wouter on 17/04/15.
 */
public class Resource
{
    public static final String ID_PROPERTY = "@id";
    public static final String LANGUAGE_PROPERTY = "@language";
    public static final String TYPE_PROPERTY = "@type";
    public static final String VALUE_PROPERTY = "@value";

    String language;
    HashMap<String, Object> wrappedResource;
    HashMap<String, Integer> propertyCounter;
    JsonLDWrapper model;

    private Resource() {
        wrappedResource = new HashMap<>();
        propertyCounter = new HashMap<>();
    }

    public void setModel(JsonLDWrapper jsonLD) {
        this.model = jsonLD;
    }

    public void setLanguage(String language)
    {
        this.language = language;
    }

    public void addProperty(String name, Object value) {
        if (!this.wrappedResource.containsKey(name)) {
            this.wrappedResource.put(name, value);
        } else if (this.wrappedResource.get(name) instanceof List) {
            if (value instanceof List) {
                ((List<Object>)this.wrappedResource.get(name)).addAll((List<Object>) value);
            } else {
                ((List<Object>)this.wrappedResource.get(name)).add(value);
            }
        } else {
            ArrayList<Object> wrappingList = new ArrayList<>();
            wrappingList.add(this.wrappedResource.get(name));
            if (value instanceof List) {
                wrappingList.addAll((List<Object>) value);
            } else {
                wrappingList.add(value);
            }
            this.wrappedResource.put(name, wrappingList);
        }
    }

    public String getProperty(String name) {
        Object value = wrappedResource.get(name);
        String retVal = null;
        if (value instanceof String && getPropertyIndex(name) == 0) {
            retVal = (String)value;
        } else if (value instanceof Map && getPropertyIndex(name) == 0) {
            retVal = getMappedPropertyValue((Map<String, Object>)value);
        } else if (value instanceof List) {
            if (((List)value).size() > getPropertyIndex(name)) {
                value = ((List)value).get(getPropertyIndex(name));
            }
            if (value instanceof String) {
                retVal = (String)value;
            } else {
                retVal = getMappedPropertyValue((Map<String, Object>)value);
            }

        }
        return retVal;
    }

    public Resource getResource(String name) {

        Object value = wrappedResource.get(name);
        Resource retVal = null;
        if (value instanceof String && getPropertyIndex(name) == 0) {
            retVal = this.model.getResource((String)value, this.language);
        } else if (value instanceof Map && getPropertyIndex(name) == 0) {
            retVal = getMappedResourceValue((Map<String, Object>) value);
        } else if (value instanceof List) {
            if (((List)value).size() > getPropertyIndex(name)) {
                value = ((List)value).get(getPropertyIndex(name));
            }
            retVal = getMappedResourceValue((Map<String, Object>) value);
        }
        return retVal;
    }

    public Integer getPropertyValueCount(String property) {
        Object value = wrappedResource.get(property);
        Integer retVal = null;
        if (value == null) {
            retVal = 0;
        } else if (value instanceof List) {
            retVal = ((List)value).size();
        } else {
            retVal = 1;
        }
        return retVal;
    }

    private String getMappedPropertyValue(Map<String, Object> value) {
        String retVal = null;
        if (value.containsKey(Resource.LANGUAGE_PROPERTY)) {
            retVal = (String)value.get(Resource.VALUE_PROPERTY);
        } else {
            Logger.debug("This is probably a resource. Fetch with getResource()");
        }
        return retVal;
    }

    private Resource getMappedResourceValue(Map<String, Object> value) {
        Resource retVal = null;
        if (value.containsKey(Resource.ID_PROPERTY)) {
            retVal = this.model.getResource((String)value.get(Resource.ID_PROPERTY), this.language);
        } else {
            Logger.debug("This is probably a property. Fetch with getProperty()");
        }
        return retVal;
    }

    public Integer getPropertyIndex(String name) {
        if (!propertyCounter.containsKey(name)) {
            propertyCounter.put(name, 0);
        }
        return propertyCounter.get(name);
    }

    public Integer incrementPropertyIndex(String name) {
        if (!propertyCounter.containsKey(name)) {
            propertyCounter.put(name, 0);
        }
        return propertyCounter.put(name, propertyCounter.get(name) + 1);
    }



    public static Resource createResource(HashMap<String, Object> resource, JsonLDWrapper model, String language) {

        HashMap<String, Object> context = model.context;
        if (resource == null) return null;

        Resource retVal = new Resource();
        retVal.setModel(model);
        for (String key: resource.keySet()) {
            Object propertyValue = resource.get(key);
            Object newKey = context.get(key);
            String absoluteKey = null;
            HashMap<String, Object> child = null;
            if (newKey instanceof String) {
                absoluteKey = (String)newKey;
            } else if (newKey instanceof Map) {
                child = (HashMap<String, Object>)newKey;
                if (child.containsKey(Resource.ID_PROPERTY))  {
                    absoluteKey = (String)child.get(Resource.ID_PROPERTY);
                } else {
                    Logger.debug("Unknown key for value: key is map but no @id found");
                    continue;
                }
            } else if (newKey == null && key.equals(Resource.ID_PROPERTY) ) {
                absoluteKey = RDFS.Resource.toString();
            } else {
                Logger.debug("Unknown key for value: key not found in context");
                continue;
            }

            if (propertyValue instanceof  String) {

                    retVal.addProperty(absoluteKey, propertyValue);

            } else if (propertyValue instanceof List) {
                List<Object> values = (List<Object>)propertyValue;
                ArrayList<Object> noLang = new ArrayList<>();
                ArrayList<Object> lang = new ArrayList<>();
                for (Object value: values) {
                    if (value instanceof String) {
                        noLang.add(value);
                    } else if (value instanceof Map && ((Map)value).get(Resource.LANGUAGE_PROPERTY) != null && ((Map)value).get(Resource.LANGUAGE_PROPERTY).equals(language)) {
                        lang.add(value);
                    }
                }
                int langSize = lang.size();
                int nolangSize = noLang.size();
                while(langSize < nolangSize) {
                    lang.add(noLang.get(langSize));
                    langSize++;
                }
                retVal.addProperty(absoluteKey, lang);
            } else if (propertyValue instanceof Map) {
                if (((Map)propertyValue).get(Resource.LANGUAGE_PROPERTY) != null && ((Map)propertyValue).get(Resource.LANGUAGE_PROPERTY).equals(language)) {
                    retVal.addProperty(absoluteKey, propertyValue);
                }
            } else {
                Logger.debug("Unknown property: skip property");
            }

        }
        return retVal;
    }

}
