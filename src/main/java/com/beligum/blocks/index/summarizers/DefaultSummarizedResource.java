package com.beligum.blocks.index.summarizers;

import com.beligum.blocks.index.ifaces.ResourceSummary;

import java.net.URI;

public class DefaultSummarizedResource implements ResourceSummary
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private String label;
    private String description;
    private URI image;

    //-----CONSTRUCTORS-----
    public DefaultSummarizedResource(String label, String description, URI image)
    {
        this.label = label;
        this.description = description;
        this.image = image;
    }


    //-----PUBLIC METHODS-----
    @Override
    public String getLabel()
    {
        return label;
    }
    @Override
    public String getDescription()
    {
        return description;
    }
    @Override
    public URI getImage()
    {
        return image;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
