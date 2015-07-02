package com.beligum.blocks.resources.jackson;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.database.DummyBlocksController;
import com.beligum.blocks.resources.dummy.DummyResource;
import com.beligum.blocks.resources.interfaces.Node;
import com.beligum.blocks.resources.interfaces.Resource;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Created by wouter on 21/05/15.
 */
public class ResourceJsonDeserializer extends JsonDeserializer<Resource>
{

    @Override
    public Resource deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException
    {
        JsonNode node = jsonParser.readValueAsTree();
        Resource retVal = null;
        try {
            retVal =  parseResource(node);
        }
        catch (URISyntaxException e) {
            Logger.error("Exception while deserializing resource", e);
        }
        return retVal;
    }

    private Node parseObject(JsonNode jsonNode) throws URISyntaxException
    {
        Node retVal = null;


        if (jsonNode.isObject()) {
            if (jsonNode.has(ParserConstants.JSONLD_VALUE)) {
                Locale locale = Locale.ROOT;
                if (jsonNode.has(ParserConstants.JSONLD_LANGUAGE)) {
                    Locale l = BlocksConfig.instance().getLocaleForLanguage(jsonNode.get(ParserConstants.JSONLD_LANGUAGE).asText());
                    locale = l != null ? l : locale;
                }
                retVal = DummyBlocksController.instance().createNode(jsonNode.get(ParserConstants.JSONLD_VALUE).asText(), locale);
            } else if (jsonNode.has(ParserConstants.JSONLD_ID)) {
                retVal = parseResource(jsonNode);
            } else  {
                // parse hashMap
            }
        }
        return retVal;
    }


    protected Resource parseResource(JsonNode node) throws URISyntaxException
    {

        HashMap<String, URI> context = new HashMap<String, URI>();
        Resource resource = getEmptyResource();

        if (node.has(ParserConstants.JSONLD_CONTEXT) && node.get(ParserConstants.JSONLD_CONTEXT).isObject()) {
            Iterator<Map.Entry<String, JsonNode>> iterator = node.get(ParserConstants.JSONLD_CONTEXT).fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                context.put(entry.getKey(), new URI(entry.getValue().asText()));
            }
        }

        Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
        while(iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();

            // fetch value
            if (entry.getKey().equals(ParserConstants.JSONLD_TYPE)) {
                // parse resource
                JsonNode types = entry.getValue();
                Set<URI> typeSet = new HashSet<>();
                if (types.isArray()) {
                    for (JsonNode valueNode: entry.getValue().findValues(ParserConstants.JSONLD_TYPE)) {
                        typeSet.add(new URI(valueNode.asText()));
                    }
                } else {
                    typeSet.add(new URI(entry.getValue().get(ParserConstants.JSONLD_TYPE).textValue()));
                }
                resource.setRdfType(typeSet);
            } else if (entry.getKey().equals(ParserConstants.JSONLD_ID)) {
                resource.setBlockId(new URI(entry.getValue().asText()));
            } else if (entry.getKey().equals(ParserConstants.JSONLD_CONTEXT)) {
                // skip because already parsed
            } else if (context.containsKey(entry.getKey())) {
                // Only parse values that are in the context
                if (entry.getValue().isArray()) {
                    parseList(entry.getValue(), resource, context.get(entry.getKey()));
                }
                else if (entry.getValue().isObject()) {
                    Node value = parseObject(entry.getValue());
                    resource.add(context.get(entry.getKey()), value);
                }
                else {
                    resource.add(context.get(entry.getKey()), DummyBlocksController.instance().createNode(entry.getValue().asText(), Locale.ROOT));
                }
            }

        }

        return resource;
    }

    private void parseList(JsonNode listNode, Resource resource, URI fieldName) throws URISyntaxException
    {
        Iterator<JsonNode> iterator = listNode.iterator();
        while (iterator.hasNext()) {
            JsonNode node = iterator.next();
            if (node.isArray()) {
                parseList(node, resource, fieldName);
            } else if (node.isObject()) {
                resource.add(fieldName, parseObject(node));
            } else {
                resource.add(fieldName, DummyBlocksController.instance().createNode(node.asText(), Locale.ROOT));
            }
        }
    }

    protected Resource getEmptyResource() {
        return new DummyResource(new HashMap<String, Object>(), new HashMap<String, Object>());
    }


}
