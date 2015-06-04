package com.beligum.blocks.models.resources.jackson;

import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.models.resources.interfaces.Node;
import com.beligum.blocks.models.resources.interfaces.Resource;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

/**
 * Created by wouter on 21/05/15.
 */
public class ResourceJsonSerializer extends JsonSerializer<Resource>
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
            jgen.writeBoolean(field.getBoolean());
        } else if (field.isInt()) {
            jgen.writeNumber(field.getInteger());
        } else if (field.isDouble()) {
            jgen.writeNumber(field.getDouble());
        } else if (field.isLong()) {
            jgen.writeString(field.getLong().toString());
        } else if (field.isString()) {
            jgen.writeString(field.toString());
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
            printNode(jgen, field, locale);
        }

    }


}
