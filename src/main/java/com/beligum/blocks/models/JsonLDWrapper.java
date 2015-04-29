//package com.beligum.blocks.models;
//
//import com.beligum.base.utils.Logger;
//import com.beligum.blocks.base.Blocks;
//import com.beligum.blocks.identifiers.BlockId;
//import com.beligum.blocks.models.interfaces.BlocksStorable;
//import com.beligum.blocks.models.jsonld.ResourceNode;
//import com.fasterxml.jackson.annotation.JsonProperty;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * Created by wouter on 17/04/15.
// */
//public class JsonLDWrapper implements BlocksStorable
//{
//    @JsonProperty("@graph")
//    public ArrayList<HashMap<String, Object>> graph;
//    @JsonProperty("@context")
//    public HashMap<String, Object> context;
//
//    private HashMap<String, HashMap<String, Object>> cachedResources;
//    private String mainResource;
//    private HashMap<String, String> cachedFields;
//    private HashMap<String, String> expanded;
//
//    private void cache()
//    {
//        this.cachedResources = new HashMap<>();
//        this.cachedFields = new HashMap<>();
//        this.expanded = new HashMap<>();
//        for (HashMap<String, Object> resource : graph) {
//            if (resource.containsKey("@id") && resource.get("@id") instanceof String) {
//                String id = (String) resource.get("@id");
//                cachedResources.put(id, resource);
//                if (id.startsWith(Blocks.config().getDefaultRdfPrefix())) {
//                    this.mainResource = id;
//                }
//            }
//        }
//
//        for (String key : context.keySet()) {
//            if (context.get(key) instanceof String) {
//                if (!expanded.containsKey(key))
//                    context.put(key, expand(key, (String) context.get(key)));
//                cachedFields.put((String) context.get(key), key);
//            }
//            else if (context.get(key) instanceof Map) {
//                if (((HashMap) context.get(key)).get("@id") instanceof String) {
//                    expand(null, (String) ((HashMap) context.get(key)).get("@id"));
//                    cachedFields.put((String) ((HashMap) context.get(key)).get("@id"), key);
//                }
//            }
//        }
//    }
//
//    public String expand(String shortUri, String longUri)
//    {
//        String retVal = longUri;
//        if (!longUri.startsWith("http://")) {
//            if (longUri.contains(":")) {
//                String[] paths = longUri.split(":");
//                if (this.expanded.containsKey(paths[0])) {
//                    retVal = this.expanded.get(paths[0]) + paths[1];
//                }
//                else if (this.context.containsKey(paths[0]) && this.context.get(paths[0]) instanceof String) {
//                    this.expand(paths[0], (String) this.context.get(paths[0]));
//                    retVal = this.expanded.get(paths[0]) + paths[1];
//                }
//                else {
//                    Logger.debug("Could not expand url:" + longUri);
//                }
//            }
//        }
//        if (shortUri != null)
//            this.expanded.put(shortUri, retVal);
//        return retVal;
//    }
//
//    public Resource getMainResource(String language)
//    {
//        if (cachedFields == null || cachedResources == null)
//            this.cache();
//        return getResource(this.mainResource, language);
//    }
//
//    public ResourceNode getResource(String id, String language)
//    {
//
//        return ResourceNode.createResource(cachedResources.get(id), this, language);
//    }
//
//
//    @Override
//    public String getCreatedBy()
//    {
//        return null;
//    }
//    @Override
//    public void setCreatedBy(String created_by)
//    {
//
//    }
//    @Override
//    public String getUpdatedBy()
//    {
//        return null;
//    }
//    @Override
//    public void setUpdatedBy(String updated_by)
//    {
//
//    }
//    @Override
//    public String getCreatedAt()
//    {
//        return null;
//    }
//    @Override
//    public void setCreatedAt(String createdAt)
//    {
//
//    }
//    @Override
//    public String getUpdatedAt()
//    {
//        return null;
//    }
//    @Override
//    public void setUpdatedAt(String updatedAt)
//    {
//
//    }
//
//    //    public RDFNode getProperty(Resource resource, String property) {
//    //        String id = resource.getId().toString();
//    //        HashMap<String, Object> res = cachedResources.get(id);
//    //
//    //    }
//
//}
