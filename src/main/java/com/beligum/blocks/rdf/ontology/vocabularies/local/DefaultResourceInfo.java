package com.beligum.blocks.rdf.ontology.vocabularies.local;

import com.beligum.blocks.endpoints.ifaces.ResourceInfo;

import java.net.URI;
import java.util.Locale;

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
    private Locale language;

    //-----CONSTRUCTORS-----
    //For json deserialization
    private DefaultResourceInfo()
    {
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
    @Override
    public Locale getLanguage()
    {
        return language;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
