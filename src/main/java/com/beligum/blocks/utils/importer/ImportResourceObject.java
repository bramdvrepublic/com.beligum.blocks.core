package com.beligum.blocks.utils.importer;

import java.net.URI;

public class ImportResourceObject extends ImportResource
{
    private URI resourceType;
    private URI usedProperty;

    public ImportResourceObject(){

    }

    public ImportResourceObject(URI resourceType,  URI usedProperty, Integer index)
    {
        this.resourceType = resourceType;
        this.usedProperty = usedProperty;
        super.index  = index;
    }

    public URI getResourceType()
    {
        return resourceType;
    }

    public URI getUsedProperty()
    {
        return usedProperty;
    }

}
