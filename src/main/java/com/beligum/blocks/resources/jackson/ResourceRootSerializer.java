package com.beligum.blocks.resources.jackson;

import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.resources.interfaces.Node;
import com.beligum.blocks.resources.interfaces.Resource;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.Locale;

/**
 * Created by wouter on 29/06/15.
 */
public class ResourceRootSerializer extends ResourceSerializer
{
    @Override
    protected boolean printRootFields() {
        return true;
    }

    @Override
    protected Iterator<URI> getFieldIterator(Resource resource) {
        return resource.getRootFields().iterator();
    }

    @Override
    protected void printNode(JsonGenerator jgen, Node field, Locale locale) throws IOException
    {
        if (field.isIterable()) {
            printListNode(jgen, field, locale);
        } else if (field.isResource()) {
            nestResources(jgen, (Resource) field);
        } else {
            writeValue(jgen, field);
        }

    }

    @Override
    protected void nestResources(JsonGenerator jgen, Resource resource) throws IOException
    {
        jgen.writeStartObject();
        jgen.writeFieldName(ParserConstants.JSONLD_ID);
        jgen.writeString(resource.getBlockId().toString());
        jgen.writeEndObject();
    }

}
