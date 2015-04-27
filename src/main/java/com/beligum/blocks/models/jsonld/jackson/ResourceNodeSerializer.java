package com.beligum.blocks.models.jsonld.jackson;

import com.beligum.blocks.models.jsonld.JsonLDGraph;
import com.beligum.blocks.models.jsonld.Node;
import com.beligum.blocks.models.jsonld.ResourceNode;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by wouter on 26/04/15.
 */

public class ResourceNodeSerializer extends JsonSerializer<ResourceNode>
{

    @Override
    public void serialize(ResourceNode value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
                                                                                                     JsonProcessingException
    {
        printResource(jgen, value);

    }

    private void printNode(JsonGenerator jgen, Node field) throws IOException
    {
        if (field.isList()) {
            jgen.writeStartArray();
            for (Node node: field.getList()) {
                printNode(jgen, node);
            }
            jgen.writeEndArray();
        } else if (field.isBoolean()) {
            jgen.writeBoolean(field.getBoolean());
        } else if (field.isInt()) {
            jgen.writeNumber(field.getInteger());
        } else if (field.isDouble()) {
            jgen.writeNumber(field.getDouble());
        } else if (field.isLong()) {
            jgen.writeString(field.getLong().toString());
        } else if (field.isString()) {
            jgen.writeString(field.getString());
        } else if (field.isResource()) {
            printResource(jgen, (ResourceNode)field);

        }

    }

    private void printResource(JsonGenerator jgen, ResourceNode resource) throws IOException {
        jgen.writeStartObject();
            if (resource.getId() != null && !resource.getId().startsWith("_b:")) {
                jgen.writeStringField("@id", resource.getId());
            } else if (resource.getId() == null) {
                int x = 0;
            }

            Iterator<String> it = resource.getFields().iterator();
            while (it.hasNext()) {
                String field = it.next();
                if (!field.equals("@id")) {
                    Node fieldNode = resource.get(field);
                    jgen.writeFieldName(field);
                    printNode(jgen, fieldNode);
                }
            }
            // write ohter fields
            jgen.writeEndObject();


    }


}