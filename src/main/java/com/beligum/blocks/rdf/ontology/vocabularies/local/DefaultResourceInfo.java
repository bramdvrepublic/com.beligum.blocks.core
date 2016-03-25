package com.beligum.blocks.rdf.ontology.vocabularies.local;

import com.beligum.blocks.endpoints.ifaces.ResourceInfo;

import java.net.URI;

/**
 * Created by bram on 3/12/16.
 */
public class DefaultResourceInfo implements ResourceInfo
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private URI resourceUri;
    private URI resourceType;
    private String label;
    private URI link;
    private boolean externalLink;
    private URI image;
    private String name;

    //-----CONSTRUCTORS-----
    //For json deserialization
    private DefaultResourceInfo()
    {
        this(null, null, null, null, null, null);
    }
    public DefaultResourceInfo(URI resourceUri, URI resourceType, String label, URI link, URI image, String name)
    {
        this.resourceUri = resourceUri;
        this.resourceType = resourceType;
        this.label = label;
        this.link = link;
        this.externalLink = false;
        this.image = image;
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
        return externalLink;
    }
    @Override
    public URI getImage()
    {
        return image;
    }
    @Override
    public String getName()
    {
        return name;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
