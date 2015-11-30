package com.beligum.blocks.models.jackson.resource;

import com.beligum.blocks.models.interfaces.Resource;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;

/**
 * Created by wouter on 29/06/15.
 */
public class ResourceRootSerializer extends ResourceSerializer
{
    @Override
    protected boolean printRootFields()
    {
        return true;
    }

    @Override
    protected Iterator<URI> getFieldIterator(Resource resource)
    {
        return resource.getRootFields().iterator();
    }

    @Override
    protected void nestResources(JsonGenerator jgen, Resource resource) throws IOException
    {
        printResourceReference(jgen, resource);
    }

}
