package com.beligum.blocks.models.jackson;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.models.interfaces.Node;
import com.beligum.blocks.models.interfaces.Resource;
import com.beligum.blocks.utils.RdfTools;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * Created by wouter on 26/08/15.
 */
public class NodeSerializer<T extends Node> extends JsonSerializer<Node>
{
    protected Stack<URI> serializedResources = new Stack<URI>();

    @Override
    public void serialize(Node value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException
    {
        serializedResources = new Stack<URI>();
        printNode(jgen, value, value.getLanguage());
        // Reset resources cache for next run
        serializedResources = new Stack<URI>();
    }

    // Print all fields in an object
    protected void printResource(JsonGenerator jgen, Resource resource) throws IOException {
        Map<String, String> context = resource.getContext();

        jgen.writeStartObject();

        // Add @id to json
        if (resource.getBlockId() != null) {
            jgen.writeStringField(ParserConstants.JSONLD_ID, resource.getBlockId().toString());
        }

        // Add @type to json
        if (resource.getRdfType() != null) {
            Set<URI> typeNode = resource.getRdfType();
            jgen.writeFieldName(ParserConstants.JSONLD_TYPE);
            if (typeNode != null) {
                jgen.writeStartArray();
                for (URI fieldValue : typeNode) {
                    jgen.writeString(fieldValue.toString());
                }
                jgen.writeEndArray();
            }
        }

        // Add other fields to json
        Iterator<URI> it = getFieldIterator(resource);
        while (it.hasNext()) {
            URI field = it.next();
            String stringField = RdfTools.makeDbFieldFromUri(field);
            context.put(stringField, field.toString());

            // Write the default Value (Locale.ROOOT)
            Node fieldNode = resource.get(field);
            if (!fieldNode.isNull()) {
                jgen.writeFieldName(stringField);
                jgen.writeStartArray();
                printListNode(jgen, fieldNode, Locale.ROOT);
                jgen.writeEndArray();
            }

            // Check if this property has values in other locales
            Set<Locale> locales = resource.getLocalesForField(field);
            locales.remove(Locale.ROOT);
            if (locales.size() > 0) {
                // Add new field to context as container for localized values for this property
                String localeStringField = stringField + ParserConstants.LOCALIZED_PROPERTY;
                context.put(localeStringField, field.toString());
                jgen.writeFieldName(localeStringField);
                jgen.writeStartObject();

                for (Locale locale : resource.getLocalesForField(field)) {
                    if (locale != Locale.ROOT) {
                        fieldNode = resource.get(field, locale);
                        jgen.writeFieldName(locale.getLanguage());
                        jgen.writeStartArray();
                        printListNode(jgen, fieldNode, locale);
                        jgen.writeEndArray();
                    }
                }
                jgen.writeEndObject();
            }
        }


        // Add extra properties
        writeSpecialProperties(jgen, resource);


        // Write context
        jgen.writeFieldName(ParserConstants.JSONLD_CONTEXT);
        jgen.writeStartObject();
        for (String key : context.keySet()) {
            jgen.writeFieldName(key);
            jgen.writeString(context.get(key));
        }
        jgen.writeEndObject();

        // End of resource
        jgen.writeEndObject();


    }


    protected Iterator<URI> getFieldIterator(Resource resource) {
        return resource.getFields().iterator();
    }

    // Print a list of values
    protected void printListNode(JsonGenerator jgen, Node field, Locale locale) throws IOException
    {
        if (field.isIterable()) {
            for (Node node: field) {
                printNode(jgen, node, locale);
            }
        } else if (field.isResource()) {
            nestResources(jgen, (Resource) field);
        } else {
            printNode(jgen, field, locale);
        }

    }


    // Print a simple value for a field
    // delegates for Resources and lists
    protected void printNode(JsonGenerator jgen, Node field, Locale locale) throws IOException
    {
        if (field.isIterable()) {
            printListNode(jgen, field, locale);
        } else if (field.isResource()) {
            nestResources(jgen, (Resource) field);
        } else if (!field.isNull()) {
            jgen.writeStartObject();
            jgen.writeFieldName(ParserConstants.JSONLD_VALUE);
            writeValue(jgen, field);

            if (field.getLanguage() != Locale.ROOT) {

                jgen.writeFieldName(ParserConstants.JSONLD_LANGUAGE);
                jgen.writeString(field.getLanguage().getLanguage());

            }
            jgen.writeEndObject();
        }
    }


    // This methods only purpose is to be overwritten by the ResourceSimpleJsonSerializer
    // to prevent the serialization of nested objects
    protected void nestResources(JsonGenerator jgen, Resource resource) throws IOException
    {
        // if we are here a resource will be nested inside another resource
        if (serializedResources.contains(resource.getBlockId())) {
            printResourceReference(jgen, resource);
        } else {
            serializedResources.push(resource.getBlockId());
            printResource(jgen, resource);
            serializedResources.pop();
        }
    }

    protected void printResourceReference(JsonGenerator jgen, Resource resource) throws IOException
    {
        jgen.writeStartObject();
        jgen.writeFieldName(ParserConstants.JSONLD_ID);
        jgen.writeString(resource.getBlockId().toString());
        jgen.writeEndObject();
    }

    protected void writeValue(JsonGenerator jgen, Node field) throws IOException
    {

        if (field.isBoolean()) {
            jgen.writeBoolean(field.getBoolean());
        } else if (field.isInt()) {
            jgen.writeNumber(field.getInteger());
        } else if (field.isDouble()) {
            jgen.writeNumber(field.getDouble());
        } else if (field.isLong()) {
            jgen.writeString(field.getLong().toString());
        } else if (field.isString()) {
            jgen.writeString(field.toString());
        } else {
            Logger.error("No value was written to json. Unknown value.");
        }
    }

    /*
    * Method to ovewrwrite to write special properties inside the resource
    * */
    protected void writeSpecialProperties(JsonGenerator jgen, Resource resource) throws IOException
    {

    }
}
