package com.beligum.blocks.rdf.ontology.vocabularies.local;

import com.beligum.blocks.endpoints.ifaces.AutocompleteValue;

import java.net.URI;

/**
 * Created by bram on 3/12/16.
 */
public class DefaultResourceValue implements AutocompleteValue
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private URI resourceUri;
    private URI resourceType;
    private String label;
    private URI link;
    private String name;

    //-----CONSTRUCTORS-----
    public DefaultResourceValue(URI resourceUri, URI resourceType, String label, URI link, String name)
    {
        this.resourceUri = resourceUri;
        this.resourceType = resourceType;
        this.label = label;
        this.link = link;
        this.name = name;
    }

    //-----PUBLIC METHODS-----
    @Override
    public URI getResourceUri()
    {
        return resourceUri;
    }
    @Override
    public URI getResourceType()
    {
        return resourceType;
    }
    @Override
    public String getLabel()
    {
        return label;
    }
    @Override
    public URI getLink()
    {
        return link;
    }
    @Override
    public boolean isExternalLink()
    {
        return false;
    }
    @Override
    public String getName()
    {
        return name;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
