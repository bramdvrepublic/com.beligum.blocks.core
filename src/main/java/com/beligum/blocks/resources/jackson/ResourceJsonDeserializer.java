//package com.beligum.blocks.resources.jackson;
//
//import com.beligum.base.utils.Logger;
//import com.beligum.blocks.config.BlocksConfig;
//import com.beligum.blocks.config.ParserConstants;
//import com.beligum.blocks.database.DummyBlocksDatabase;
//import com.beligum.blocks.resources.dummy.DummyResource;
//import com.beligum.blocks.resources.interfaces.Node;
//import com.beligum.blocks.resources.interfaces.Resource;
//import com.fasterxml.jackson.core.JsonParser;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.core.JsonToken;
//import com.fasterxml.jackson.databind.DeserializationContext;
//import com.fasterxml.jackson.databind.JsonDeserializer;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.vaadin.sass.internal.selector.ParentSelector;
//
//import java.io.IOException;
//import java.util.*;
//
///**
//* Created by wouter on 21/05/15.
//*/
//public class ResourceJsonDeserializer extends JsonDeserializer<Resource>
//{
//    private Resource retVal = new DummyResource(new HashMap<String, Object>(), new HashMap<String, Object>(), Locale.ROOT);
//
//    @Override
//    public Resource deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException
//    {
//        JsonToken current;
//
//        current = jsonParser.nextToken();
//        if (current != JsonToken.START_OBJECT) {
//            Logger.debug("This is not an object. Can not parse Resource");
//            return null;
//        }
//        JsonNode node = jsonParser.readValueAsTree();
//
//
//
//        return (Resource)parseObject(node);
//    }
//
//    private Node parseObject(JsonNode jsonNode)
//    {
//        HashMap<String, Object> rootValues = new HashMap<>();
//        HashMap<String, Object> localValues = new HashMap<>();
//        Object value = null;
//        Locale locale = null;
//        boolean nodeFound = false;
//        boolean idFound = false;
//
//        if (jsonNode.isObject()) {
//            if (jsonNode.has(ParserConstants.JSONLD_VALUE)) {
//
//            } else if (jsonNode.has(ParserConstants.JSONLD_ID)) {
//
//            } else  {
//                // parse hashMap
//            }
//        }
//
//
////            if (current == JsonToken.START_ARRAY) {
////
////            } else if (current == JsonToken.START_OBJECT && fieldName.equals(ParserConstants.JSONLD_CONTEXT)) {
////                // parse context
////                parseObject(jsonParser, resource);
////            } else if (fieldName.equals(ParserConstants.JSONLD_VALUE)) {
////                // parse context
////            } else if (current == JsonToken.START_OBJECT) {
////
////            } else {
////
////            }
//
//    }
//
//
//    private Resource parseResource(JsonNode node) {
//
//        HashMap<String, String> context = new HashMap<String, String>();
//        HashMap<String, Object> rootValues = new HashMap<String, Object>();
//        HashMap<String, Object> localValues = new HashMap<String, Object>();
//        Locale locale = null;
//        if (node.has(ParserConstants.JSONLD_CONTEXT) && node.get(ParserConstants.JSONLD_CONTEXT).isObject()) {
//            Iterator<Map.Entry<String, JsonNode>> iterator = node.get(ParserConstants.JSONLD_CONTEXT).fields();
//            while (iterator.hasNext()) {
//                Map.Entry<String, JsonNode> entry = iterator.next();
//                context.put(entry.getKey(), entry.getValue().asText());
//            }
//        }
//
//        Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
//        while(iterator.hasNext()) {
//            Map.Entry<String, JsonNode> entry = iterator.next();
//            if (entry.getValue().isObject()) {
//                // fetch value
//                if (entry.getValue().has(ParserConstants.JSONLD_TYPE)) {
//                    // parse resource
//                    JsonNode types = entry.getValue().get(ParserConstants.JSONLD_TYPE);
//                    Set<String> typeSet = new HashSet<>();
//                    if (types.isArray()) {
//                        for (JsonNode valueNode: entry.getValue().findValues(ParserConstants.JSONLD_TYPE)) {
//                            typeSet.add(valueNode.asText());
//                        }
//                    } else {
//                        typeSet.add(entry.getValue().get(ParserConstants.JSONLD_TYPE).textValue());
//                    }
//                    rootValues.put(ParserConstants.JSONLD_TYPE, typeSet);
//                } else if (entry.getValue().has(ParserConstants.JSONLD_ID)) {
//                    entry.getValue().get(ParserConstants.JSONLD_TYPE).textValue();
//                } else if (entry.getValue().has(ParserConstants.JSONLD_CONTEXT)) {
//                    // skip because already parsed
//                } else if (entry.getValue().isArray()) {
//
//                } else if (entry.getValue().isObject()) {
//                    Node t = parseObject(entry.getValue());
//                    if (t.getLanguage().equals(Locale.ROOT)) {
//                        rootValues.put(entry.getKey(), t.getValue());
//                    } else {
//                        localValues.put(entry.getKey(), t.getValue());
//                        if (locale == null) locale = t.getLanguage();
//                    }
//                } else {
//                    rootValues.put(entry.getKey(), parseValue(entry.getValue()));
//                }
//
//            } else if (entry.getValue().isArray()) {
//                // parse list
//            } else {
//
//            }
//        }
//
//    }
//
//    private List<Object> parseList(JsonParser jsonParser) throws IOException
//    {
//        List<Object> retVal = new ArrayList<Object>();
//        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
//            String fieldName = jsonParser.getCurrentName();
//            JsonToken currentToken = jsonParser.getCurrentToken();
//            if (currentToken == JsonToken.START_ARRAY) {
//                // Flatten list inside list
//                List<Object> result = parseList(jsonParser);
//
//            } else if (currentToken == JsonToken.START_OBJECT) {
//                parseObject(jsonParser, null);
//            } else {
//
//            }
//        }
//        return retVal;
//    }
//
//    private Node parseValue(JsonNode jsonNode )
//    {
//        Node retVal = null;
//        return retVal;
//    }
//
//    private void parseNode(JsonParser jsonParser, Resource resource) throws IOException
//    {
//        String Locale = null;
//        Object value = null;
//        String fieldName = jsonParser.getCurrentName();
//        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
//            if (fieldName.equals(ParserConstants.JSONLD_VALUE)) {
//                value = jsonParser.getCurrentToken();
//            } else if (fieldName.equals(ParserConstants.JSONLD_LANGUAGE)) {
//                value = jsonParser.getCurrentToken();
//                jsonParser.nextToken();
//                fieldName= jsonParser.getCurrentName();
//            }
//
//        }
//    }
//
//
//}
