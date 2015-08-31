package com.beligum.blocks.models.jackson;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.models.ReferenceNode;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * Created by wouter on 26/08/15.
 */
public class NodeDeserializer extends JsonDeserializer<Resource>
{
    @Override
    public Resource deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JsonProcessingException
    {
        // Parse the tree
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

    // ------------ PROTECTED METHODS ----------------------

    /*
    * Parses a resource
    * */
    protected Resource parseResource(JsonNode node) throws URISyntaxException
    {

        HashMap<String, URI> context = new HashMap<String, URI>();
        Resource resource = createNewResource(node);

        // Search for a context. We will need this to iterate all the fields
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

            // We found the type. Add them to the resource
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
                    Node value = parseObject(entry.getValue(), resource, context.get(entry.getKey()));
                    if (value != null) {
                        resource.add(context.get(entry.getKey()), value);
                    }
                }
                else {
                    resource.add(context.get(entry.getKey()), ResourceFactoryImpl.instance().createNode(entry.getValue().asText(), Locale.ROOT));
                }
            }

        }

        parseSpecialFields(node, resource);

        return resource;
    }

    /*
    * Returns a new resource
    * */
    protected Resource createNewResource(JsonNode node) {
        return new ResourceImpl(Locale.ROOT);
    }

    /*
    * To be overridden by deserializers that want to add special properties
    * */
    protected void parseSpecialFields(JsonNode node, Resource resource) {

    }

    // ------------ PRIVATE METHODS ----------------------

    /*
    * We found a property with an object.
    * If this object has a
    *   - @value property: then it is a node
    *   - @id without @type it is a reference to a resource
    *   - @id with @type it is a resource
    *   - none of the above, check if the properties are language codes
    *
    * */
    private Node parseObject(JsonNode jsonNode, Resource resource, URI field) throws URISyntaxException
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
            } else if (jsonNode.has(ParserConstants.JSONLD_ID) && jsonNode.has(ParserConstants.JSONLD_TYPE)) {
                retVal = parseResource(jsonNode);
            } else if (jsonNode.has(ParserConstants.JSONLD_ID)) {
                // parse hashMap
                retVal = new ReferenceNode(jsonNode.get(ParserConstants.JSONLD_ID));
            } else {
                Iterator<String> fieldNames = jsonNode.fieldNames();
                while (fieldNames.hasNext()) {
                    Locale locale = BlocksConfig.instance().getLocaleForLanguage(fieldNames.next());
                    if (locale != null && jsonNode.get(locale.getLanguage()).isArray()) {
                        parseList(jsonNode.get(locale.getLanguage()), resource, field);
                    } else {
                        Logger.error("Null value found while parsing resource");
                    }
                }
            }
        }
        return retVal;
    }


    private void parseList(JsonNode listNode, Resource resource, URI fieldName) throws URISyntaxException
    {
        Iterator<JsonNode> iterator = listNode.iterator();
        while (iterator.hasNext()) {
            JsonNode node = iterator.next();
            if (node.isArray()) {
                parseList(node, resource, fieldName);
            } else if (node.isObject()) {
                resource.add(fieldName, parseObject(node, resource, fieldName));
            } else {
                resource.add(fieldName, ResourceFactoryImpl.instance().createNode(node.asText(), Locale.ROOT));
            }
        }
    }




}
