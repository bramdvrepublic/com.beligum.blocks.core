package com.beligum.blocks.resources.jackson;

import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.resources.interfaces.Node;
import com.beligum.blocks.resources.interfaces.Resource;
import com.beligum.blocks.utils.RdfTools;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * Created by wouter on 21/05/15.
 */
public class ResourceSerializer<T extends Resource> extends JsonSerializer
{

    @Override
    public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
                                                                                                  JsonProcessingException
    {

        printResource(jgen, (Resource)value);

    }

    // Print all fields in an object
    protected void printResource(JsonGenerator jgen, Resource resource) throws IOException {
        HashMap<String, String> context = new HashMap<>();

        jgen.writeStartObject();
        if (printRootFields()) {

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
        }

        // Add other fields to json
        Iterator<URI> it = getFieldIterator(resource);
        while (it.hasNext()) {
            URI field = it.next();
            String stringField = RdfTools.makeDbFieldFromUri(field);
            context.put(stringField, field.toString());
            Node fieldNode = resource.get(field);
            jgen.writeFieldName(stringField);
            jgen.writeStartArray();
            printListNode(jgen, fieldNode, resource.getLanguage());
            jgen.writeEndArray();

        }

        if (printRootFields()) {
            // Write context
            jgen.writeFieldName(ParserConstants.JSONLD_CONTEXT);
            jgen.writeStartObject();
            for (String key : context.keySet()) {
                jgen.writeFieldName(key);
                jgen.writeString(context.get(key));
            }
            jgen.writeEndObject();
        }

        jgen.writeEndObject();


    }

    protected boolean printRootFields() {
        return true;
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
        } else {
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
        printResource(jgen, resource);
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
        }
    }


}
