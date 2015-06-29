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
public class ResourceJsonSerializer extends JsonSerializer<Resource>
{
    Stack<HashMap<String, String>> contexts = new Stack<>();
    // Start point, begins with a resource (an object)
    @Override
    public void serialize(Resource value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
                                                                                                  JsonProcessingException
    {
        jgen.writeStartObject();
        printResource(jgen, value);
        jgen.writeEndObject();

    }

    // Print all fields in an object
    private void printResource(JsonGenerator jgen, Resource resource) throws IOException {
        HashMap<String, String> context = new HashMap<>();


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
        Iterator<URI> it = resource.getFields().iterator();
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

        // Write context
        jgen.writeFieldName(ParserConstants.JSONLD_CONTEXT);
        jgen.writeStartObject();
        for (String key: context.keySet()) {
            jgen.writeFieldName(key);
            jgen.writeString(context.get(key));
        }
        jgen.writeEndObject();




    }

    // Print a list of values
    private void printListNode(JsonGenerator jgen, Node field, Locale locale) throws IOException
    {
        if (field.isIterable()) {
            for (Node node: field) {
                printNode(jgen, node, locale);
            }
        } else {
            jgen.writeStartObject();
            printNode(jgen, field, locale);
            jgen.writeEndObject();
        }

    }


    // Print a simple value for a field
    // delegates for Resources and lists
    private void printNode(JsonGenerator jgen, Node field, Locale locale) throws IOException
    {
        if (field.isIterable()) {
            printListNode(jgen, field, locale);
        } else if (field.isResource()) {
            printResource(jgen, (Resource)field);
        } else {
            jgen.writeFieldName(ParserConstants.JSONLD_VALUE);
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
            if (field.getLanguage() != Locale.ROOT) {
                jgen.writeFieldName(ParserConstants.JSONLD_LANGUAGE);
                jgen.writeString(field.getLanguage().getLanguage());
            }
        }

    }



}
