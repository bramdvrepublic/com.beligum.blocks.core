package com.beligum.blocks.models.jackson.resource;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.controllers.PersistenceControllerImpl;
import com.beligum.blocks.models.ResourceImpl;
import com.beligum.blocks.models.factories.ResourceFactoryImpl;
import com.beligum.blocks.models.interfaces.Node;
import com.beligum.blocks.models.interfaces.Resource;
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
                retVal = ResourceFactoryImpl.instance().createNode(jsonNode.get(ParserConstants.JSONLD_VALUE).asText(), locale);
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

        // Set the language of this resource
        Locale locale = null;
        if (node.has(ParserConstants.JSONLD_LANGUAGE)) {
            String lang = node.get(ParserConstants.JSONLD_LANGUAGE).asText();
            locale = BlocksConfig.instance().getLocaleForLanguage(lang);
            if (locale == null) {
               locale = new Locale(lang);
            }
        } else {
            locale = Locale.ROOT;
        }

        Resource resource = getEmptyResource(locale);

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
                if (types.isArray()) {
                    for (JsonNode valueNode: entry.getValue()) {
                        resource.addRdfType(new URI(valueNode.asText()));
                    }
                } else {
                    resource.setRdfType(new URI(entry.getValue().asText()));
                }
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
                    resource.add(context.get(entry.getKey()), ResourceFactoryImpl.instance().createNode(entry.getValue().asText(), Locale.ROOT));
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
                resource.add(fieldName, ResourceFactoryImpl.instance().createNode(node.asText(), Locale.ROOT));
            }
        }
    }

    protected Resource getEmptyResource(Locale locale) {
        return new ResourceImpl(new HashMap<String, Object>(), new HashMap<String, Object>(), locale);
    }


}
