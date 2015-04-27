package com.beligum.blocks.models.jsonld;

import com.beligum.base.utils.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import java.io.StringWriter;
import java.util.*;

/**
 * Created by wouter on 23/04/15.
 */

public class JsonLDGraph
{
    public static final String ID = "@id";
    public static final String VALUE = "@value";


    private int blankNodeCounter = 0;
    private Set idSet = new HashSet<String>();
    private JsonLDContext context = new JsonLDContext();
    HashMap<String, ResourceNode> resources = new HashMap<String, ResourceNode>();
    ResourceNode mainResource;

    public JsonLDGraph() {

    }

    public JsonLDGraph(ResourceNode resource) {
        this.mainResource = resource;
    }

    public JsonLDGraph(JsonNode graphNode, JsonLDContext context) {

        ArrayList<Node> retVal = new ArrayList<>();
        if (context != null) this.context = context;
        if (graphNode.isArray()) {
            Iterator<JsonNode> it = graphNode.iterator();

            while (it.hasNext()) {
                Node node = this.createNode(it.next());
            }

        } else if (graphNode.isObject()) {
            Node node = this.createNode(graphNode);
        } else {
            Logger.error("Node is graph is not a resource?");
        }
    }

    public ResourceNode getFirstResource() {
        ResourceNode retVal = null;
        if (resources.size() > 0) {
            retVal = resources.get(0);
        }
        return retVal;
    }


    public ResourceNode getMainResource() {
        return getMainResource(null);
    }

    public ResourceNode getMainResource(String id) {
        if (this.mainResource == null) {
            this.mainResource = this.resources.get(id);
        }
        return this.mainResource;
    }

    public Node createNode(JsonNode jsonNode) {
        Node retVal = null;
        if (jsonNode.isObject()) {
            if (jsonNode.has(VALUE)) {
                retVal = createNode(jsonNode.get(VALUE));
            } else {
                ResourceNode resource = new ResourceNode();
                retVal = resource;
                // loop through fields
                Iterator<Map.Entry<String, JsonNode>> iterator = jsonNode.fields();
                while (iterator.hasNext()) {
                    Map.Entry<String, JsonNode> field = iterator.next();
                    String key = field.getKey();
                    JsonNode value = field.getValue();
                    processResourceFields(resource, key, value);
                }

                // Fill reference Resources
                if (!resources.containsKey(resource.getId())) {
                    resources.put(resource.getId(), resource);
                } else {
                    ResourceNode old = resources.get(resource.getId());
                    if (old.isEmpty()) {
                        old.wrap(resource.unwrap());
                    } else if (resource.isEmpty()) {
                        resource.wrap(old.unwrap());
                    }
                }

            }
        } else if (jsonNode.isNumber()) {
            if (jsonNode.isIntegralNumber()) {
                if (jsonNode.isBigInteger()) {
                    retVal = new LongNode(jsonNode.asLong(0));
                } else {
                    retVal = new IntegerNode(jsonNode.asInt(0));
                }
            } else {
                retVal = new DoubleNode(jsonNode.asDouble(0));
            }
        } else if (jsonNode.isBoolean()) {
            retVal = new BooleanNode(jsonNode.asBoolean(false));
        } else if (jsonNode.isTextual()) {
            retVal = new StringNode(jsonNode.asText(""));
        } else if (jsonNode.isArray()) {

        } else {

        }
        return retVal;
    }

    private void processResourceFields(ResourceNode resource, String key, JsonNode jsonNode) {
        if (jsonNode.isArray()) {
            Iterator<JsonNode> iterator = jsonNode.iterator();
            while (iterator.hasNext()) {
                processResourceFields(resource, key, iterator.next());
            }
        } else {
            resource.add(key, createNode(jsonNode));
        }
    }

    public JsonLDContext getContext() {
        return this.getContext();
    }

//    public ResourceNodeInf getResource(String id) {
//        return this.resources.get(id);
//    }

//    public HashMap<String, ResourceNode> getResources() {
//        return this.resources;
//    }

//    public String write(boolean expanded) {
//        StringWriter writer = new StringWriter();
//        int count = 0;
//        writer.append("{@graph: [");
//        for (ProxyResourceNode proxy: resources.values()) {
//            if (count>0) writer.append(", ");
//            proxy.write(writer, expanded);
//        }
//        writer.append("]");
//        if (this.context != null) {
//            writer.append(", ");
//            this.context.write(writer, expanded);
//        }
//
//        writer.append("}");
//        return writer.toString();
//    }


}
