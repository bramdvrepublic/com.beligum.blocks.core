package com.beligum.blocks.resources.jackson;

import com.beligum.blocks.resources.interfaces.Resource;

import java.net.URI;
import java.util.Iterator;

/**
 * Created by wouter on 29/06/15.
 */
public class ResourceLocalizedSerializer extends ResourceRootSerializer
{
    @Override
    protected boolean printRootFields() {
        return false;
    }

    @Override
    protected Iterator<URI> getFieldIterator(Resource resource) {
        return resource.getLocalizedFields().iterator();
    }
}
