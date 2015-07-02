package com.beligum.blocks.resources.jackson;

import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.resources.interfaces.Node;
import com.beligum.blocks.resources.interfaces.Resource;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

/**
 * Created by wouter on 26/04/15.
 */

public class ResourceJsonLDSerializer extends JsonSerializer<Resource>
{

    // Start point, begins with a resource (an object)
    @Override
    public void serialize(Resource value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
                                                                                                     JsonProcessingException
    {
        printResource(jgen, value);

    }

    // Print all fields in an object
    private void printResource(JsonGenerator jgen, Resource resource) throws IOException {
        jgen.writeStartObject();

        // Add @id to json
        if (resource.getBlockId() != null) {
            jgen.writeStringField(ParserConstants.JSONLD_ID, resource.getBlockId().toString());
        }

        // Add @type to json
        if (resource.getRdfType() != null) {
            Set<URI> typeNode = resource.getRdfType();
            jgen.writeFieldName(ParserConstants.JSONLD_TYPE);
                jgen.writeStartArray();
                for (URI fieldValue : typeNode) {
                    jgen.writeString(fieldValue.toString());
                }
                jgen.writeEndArray();

        }

        // Add other fields to json
        Iterator<URI> it = resource.getFields().iterator();
        while (it.hasNext()) {
            URI field = it.next();
                Node fieldNode = resource.get(field);
                jgen.writeFieldName(field.toString());
                printListNode(jgen, fieldNode, resource.getLanguage());
        }

        jgen.writeEndObject();


    }

    // Print a simple value for a field
    // delegates for Resources and lists
    private void printNode(JsonGenerator jgen, Node field, Locale locale) throws IOException
    {
        if (field.isIterable()) {
            printListNode(jgen, field, locale);
        } else if (field.isBoolean()) {
            jgen.writeStartObject();
            jgen.writeFieldName(ParserConstants.JSONLD_VALUE);
            jgen.writeBoolean(field.getBoolean());
            jgen.writeEndObject();
        } else if (field.isInt()) {
            jgen.writeStartObject();
            jgen.writeFieldName(ParserConstants.JSONLD_VALUE);
            jgen.writeNumber(field.getInteger());
            jgen.writeEndObject();
        } else if (field.isDouble()) {
            jgen.writeStartObject();
            jgen.writeFieldName(ParserConstants.JSONLD_VALUE);
            jgen.writeNumber(field.getDouble());
            jgen.writeEndObject();
        } else if (field.isLong()) {
            jgen.writeStartObject();
            jgen.writeFieldName(ParserConstants.JSONLD_VALUE);
            jgen.writeString(field.getLong().toString());
            jgen.writeEndObject();
        } else if (field.isString()) {
            jgen.writeStartObject();
            jgen.writeFieldName(ParserConstants.JSONLD_VALUE);
            jgen.writeString(field.asString());
            if (field.getLanguage() != Locale.ROOT) {
                jgen.writeFieldName(ParserConstants.JSONLD_LANGUAGE);
                jgen.writeString(field.getLanguage().getLanguage());
            }
            jgen.writeEndObject();
        } else if (field.isResource()) {
            printResource(jgen, (Resource)field);

        }

    }


    // Print a list of values
    private void printListNode(JsonGenerator jgen, Node field, Locale locale) throws IOException
    {
        if (field.isIterable()) {
            jgen.writeStartArray();
            for (Node node: field) {
                printNode(jgen, node, locale);
            }
            jgen.writeEndArray();
        } else {
            jgen.writeStartArray();
            printNode(jgen, field, locale);
            jgen.writeEndArray();
        }

    }



}