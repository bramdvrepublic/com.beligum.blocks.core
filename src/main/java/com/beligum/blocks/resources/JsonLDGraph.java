package com.beligum.blocks.resources;


/**
* Created by wouter on 23/04/15.
 *
 * Class to keep for deserializing JSONLD to resource
 *
*/

public class JsonLDGraph
{
//   private Locale language;
//    private HashMap<String, Resource> resources = new HashMap<String, Resource>();
//    private Resource mainResource;
//
//    public JsonLDGraph() {
//
//    }
//
//    public JsonLDGraph(Resource resource, Locale language) {
//        this.language = language;
//        this.mainResource = resource;
//    }
//
//    public JsonLDGraph(JsonNode graphNode,  Locale language) {
//        this.language = language;
//        ArrayList<Node> retVal = new ArrayList<>();
////        if (context != null) this.context = context;
//        if (graphNode.isArray()) {
//            Iterator<JsonNode> it = graphNode.iterator();
//
//            while (it.hasNext()) {
//                Node node = this.createNode(it.next(), null);
//            }
//
//        } else if (graphNode.isObject()) {
//            Node node = this.createNode(graphNode, null);
//        } else {
//            Logger.error("Node is graph is not a resource?");
//        }
//    }
//
//    public Resource getFirstResource() {
//        Resource retVal = null;
//        if (resources.size() > 0) {
//            retVal = resources.get(0);
//        }
//        return retVal;
//    }
//
//
//    public Resource getMainResource() {
//        return getMainResource(null);
//    }
//
//    public Resource getMainResource(String id) {
//        if (this.mainResource == null) {
//            this.mainResource = this.resources.get(id);
//        }
//        return this.mainResource;
//    }
//
//    public Node createNode(JsonNode jsonNode, Locale language) {
//        Node retVal = null;
//        if (jsonNode.isObject()) {
//            if (jsonNode.has(ParserConstants.JSONLD_VALUE)) {
//                language = null;
//                if (jsonNode.has(ParserConstants.JSONLD_LANGUAGE)) {
//                    String lang = jsonNode.get(ParserConstants.JSONLD_LANGUAGE).asText();
//                    if (lang == null) {
//                        language = Locale.ROOT;
//                    } else {
//                        language = BlocksConfig.instance().getLocaleForLanguage(lang);
//                    }
//                }
//                retVal = createNode(jsonNode.get(ParserConstants.JSONLD_VALUE), language);
//            } else {
//                String id = jsonNode.get(ParserConstants.JSONLD_ID).asText();
//                Resource resource = OrientResourceController.instance().createResource(id, language);
//                retVal = resource;
//                // loop through fields
//                Iterator<Map.Entry<String, JsonNode>> iterator = jsonNode.fields();
//                while (iterator.hasNext()) {
//                    Map.Entry<String, JsonNode> field = iterator.next();
//                    String key = field.getKey();
//                    JsonNode value = field.getValue();
//                    processResourceFields(resource, key, value);
//                }
//
//                // Fill reference Resources
//                if (!resources.containsKey(resource.getBlockId())) {
//                    resources.put(resource.getBlockId(), resource);
//                } else {
//                    Resource old = resources.get(resource.getBlockId());
////                    if (old.isEmpty()) {
////                        old.wrap(resource);
////                    } else if (resource.isEmpty()) {
////                        resource.wrap(old);
////                    }
//                }
//
//            }
//        } else if (jsonNode.isNumber()) {
//            if (jsonNode.isIntegralNumber()) {
//                if (jsonNode.isBigInteger()) {
//                    retVal = OrientResourceController.instance().asNode(jsonNode.asLong(0), language);
//                } else {
//                    retVal = OrientResourceController.instance().asNode(jsonNode.asInt(0), language);
//                }
//            } else {
//                retVal = OrientResourceController.instance().asNode((Double)jsonNode.asDouble(0), language);
//            }
//        } else if (jsonNode.isBoolean()) {
//            retVal = OrientResourceController.instance().asNode(jsonNode.asBoolean(false), language);
//        } else if (jsonNode.isTextual()) {
//            retVal = OrientResourceController.instance().asNode(jsonNode.asText(""), language);
//        } else if (jsonNode.isArray()) {
//
//        } else {
//
//        }
//        return retVal;
//    }
//
//    private void processResourceFields(Resource resource, URI key, JsonNode jsonNode) {
//        if (jsonNode.isArray()) {
//            JsonNode firstElement = jsonNode.get(0);
//
//            /*
//            * If this list contains string nodes we filter for only the nodes for our current language
//            * or if there are no nodes for our current language all the nodes with our default language
//            * */
//            if (firstElement != null && firstElement.isObject() && firstElement.has(ParserConstants.JSONLD_LANGUAGE)  && firstElement.get(ParserConstants.JSONLD_LANGUAGE) != null) {
//                // filter for current lang
//                ArrayNode defaultLang = JsonNodeFactory.instance.arrayNode();
//                ArrayNode currentLang = JsonNodeFactory.instance.arrayNode();
//                Iterator<JsonNode> iterator = jsonNode.iterator();
//                while (iterator.hasNext()) {
//                    JsonNode node = iterator.next();
//                    if (node.get(ParserConstants.JSONLD_LANGUAGE).asText().equals(BlocksConfig.instance().getDefaultLanguage())) {
//                        defaultLang.add(node);
//                    } else if (node.get(ParserConstants.JSONLD_LANGUAGE).asText().equals(this.language)) {
//                        currentLang.add(node);
//                    }
//                }
//                if (currentLang.size() == 0) {
//                    jsonNode = defaultLang;
//                } else {
//                    jsonNode = currentLang;
//                }
//            }
//            Iterator<JsonNode> iterator = jsonNode.iterator();
//            while (iterator.hasNext()) {
//                processResourceFields(resource, key, iterator.next());
//            }
//        } else {
//            resource.add(key, createNode(jsonNode, null));
//        }
//    }

//    public JsonLDContext getContext() {
//        return this.getContext();
//    }


}
