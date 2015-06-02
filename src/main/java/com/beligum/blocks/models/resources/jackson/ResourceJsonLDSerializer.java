package com.beligum.blocks.models.resources.jackson;

import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.models.resources.interfaces.Node;
import com.beligum.blocks.models.resources.interfaces.Resource;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

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
            jgen.writeStringField(ParserConstants.JSONLD_ID, resource.getBlockId());
        }

        // Add @type to json
        if (resource.getRdfType() != null) {
            Node typeNode = resource.getRdfType();
            jgen.writeFieldName(ParserConstants.JSONLD_TYPE);
            if (typeNode.isIterable()) {
                jgen.writeStartArray();
                for (Node fieldValue: typeNode) {
                    jgen.writeString(fieldValue.asString());
                }
                jgen.writeEndArray();
            } else if (typeNode.isString()) {
                jgen.writeString(typeNode.asString());
            } else {
                jgen.writeString("");
            }
        }

        // Add other fields to json
        Iterator<String> it = resource.getFields().iterator();
        while (it.hasNext()) {
            String field = it.next();

                Node fieldNode = resource.get(field);
                jgen.writeFieldName(field);
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
            jgen.writeFieldName("@value");
            jgen.writeBoolean(field.getBoolean());
            jgen.writeEndObject();
        } else if (field.isInt()) {
            jgen.writeStartObject();
            jgen.writeFieldName("@value");
            jgen.writeNumber(field.getInteger());
            jgen.writeEndObject();
        } else if (field.isDouble()) {
            jgen.writeStartObject();
            jgen.writeFieldName("@value");
            jgen.writeNumber(field.getDouble());
            jgen.writeEndObject();
        } else if (field.isLong()) {
            jgen.writeStartObject();
            jgen.writeFieldName("@value");
            jgen.writeString(field.getLong().toString());
            jgen.writeEndObject();
        } else if (field.isString()) {
            jgen.writeStartObject();
            jgen.writeFieldName("@value");
            jgen.writeString(field.asString());
            if (field.getLanguage() != Locale.ROOT) {
                jgen.writeFieldName("@language");
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