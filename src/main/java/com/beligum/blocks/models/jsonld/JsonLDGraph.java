package com.beligum.blocks.models.jsonld;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.models.jsonld.interfaces.Node;
import com.beligum.blocks.models.jsonld.interfaces.Resource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.util.*;

/**
* Created by wouter on 23/04/15.
*/

public class JsonLDGraph
{
    public static final String ID = "@id";
    public static final String VALUE = "@value";
    public static final String LANGUAGE = "@language";


    private Locale language;
    private HashMap<String, Resource> resources = new HashMap<String, Resource>();
    private Resource mainResource;

    public JsonLDGraph() {

    }

    public JsonLDGraph(Resource resource, Locale language) {
        this.language = language;
        this.mainResource = resource;
    }

    public JsonLDGraph(JsonNode graphNode,  Locale language) {
        this.language = language;
        ArrayList<Node> retVal = new ArrayList<>();
//        if (context != null) this.context = context;
        if (graphNode.isArray()) {
            Iterator<JsonNode> it = graphNode.iterator();

            while (it.hasNext()) {
                Node node = this.createNode(it.next(), null);
            }

        } else if (graphNode.isObject()) {
            Node node = this.createNode(graphNode, null);
        } else {
            Logger.error("Node is graph is not a resource?");
        }
    }

    public Resource getFirstResource() {
        Resource retVal = null;
        if (resources.size() > 0) {
            retVal = resources.get(0);
        }
        return retVal;
    }


    public Resource getMainResource() {
        return getMainResource(null);
    }

    public Resource getMainResource(String id) {
        if (this.mainResource == null) {
            this.mainResource = this.resources.get(id);
        }
        return this.mainResource;
    }

    public Node createNode(JsonNode jsonNode, Locale language) {
        Node retVal = null;
        if (jsonNode.isObject()) {
            if (jsonNode.has(VALUE)) {
                language = null;
                if (jsonNode.has(LANGUAGE)) {
                    String lang = jsonNode.get(LANGUAGE).asText();
                    if (lang == null) {
                        language = Locale.ROOT;
                    } else {
                        language = Blocks.config().getLocaleForLanguage(lang);
                    }
                }
                retVal = createNode(jsonNode.get(VALUE), language);
            } else {
                String id = jsonNode.get(ID).asText();
                Resource resource = OrientResourceFactory.instance().createResource(id, language);
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
                if (!resources.containsKey(resource.getBlockId())) {
                    resources.put(resource.getBlockId(), resource);
                } else {
                    Resource old = resources.get(resource.getBlockId());
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
                    retVal = OrientResourceFactory.instance().asNode(jsonNode.asLong(0), language);
                } else {
                    retVal = OrientResourceFactory.instance().asNode(jsonNode.asInt(0), language);
                }
            } else {
                retVal = OrientResourceFactory.instance().asNode((Double)jsonNode.asDouble(0), language);
            }
        } else if (jsonNode.isBoolean()) {
            retVal = OrientResourceFactory.instance().asNode(jsonNode.asBoolean(false), language);
        } else if (jsonNode.isTextual()) {
            retVal = OrientResourceFactory.instance().asNode(jsonNode.asText(""), language);
        } else if (jsonNode.isArray()) {

        } else {

        }
        return retVal;
    }

    private void processResourceFields(Resource resource, String key, JsonNode jsonNode) {
        if (jsonNode.isArray()) {
            JsonNode firstElement = jsonNode.get(0);

            /*
            * If this list contains string nodes we filter for only the nodes for our current language
            * or if there are no nodes for our current language all the nodes with our default language
            * */
            if (firstElement != null && firstElement.isObject() && firstElement.has(LANGUAGE)  && firstElement.get(LANGUAGE) != null) {
                // filter for current lang
                ArrayNode defaultLang = JsonNodeFactory.instance.arrayNode();
                ArrayNode currentLang = JsonNodeFactory.instance.arrayNode();
                Iterator<JsonNode> iterator = jsonNode.iterator();
                while (iterator.hasNext()) {
                    JsonNode node = iterator.next();
                    if (node.get(LANGUAGE).asText().equals(Blocks.config().getDefaultLanguage())) {
                        defaultLang.add(node);
                    } else if (node.get(LANGUAGE).asText().equals(this.language)) {
                        currentLang.add(node);
                    }
                }
                if (currentLang.size() == 0) {
                    jsonNode = defaultLang;
                } else {
                    jsonNode = currentLang;
                }
            }
            Iterator<JsonNode> iterator = jsonNode.iterator();
            while (iterator.hasNext()) {
                processResourceFields(resource, key, iterator.next());
            }
        } else {
            resource.add(key, createNode(jsonNode, null));
        }
    }

//    public JsonLDContext getContext() {
//        return this.getContext();
//    }


}
