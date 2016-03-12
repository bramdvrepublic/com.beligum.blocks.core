package com.beligum.blocks.endpoints.beans;

import com.beligum.blocks.endpoints.ifaces.AutocompleteSuggestion;

import java.net.URI;

/**
 * Created by bram on 3/12/16.
 */
public class ResourceSuggestion implements AutocompleteSuggestion
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private URI resourceId;
    private URI resourceType;
    private String title;
    private String subTitle;

    //-----CONSTRUCTORS-----
    public ResourceSuggestion(URI resourceId, URI resourceType, String title, String subTitle)
    {
        this.resourceId = resourceId;
        this.resourceType = resourceType;
        this.title = title;
        this.subTitle = subTitle;
    }

    //-----PUBLIC METHODS-----
    @Override
    public URI getResourceId()
    {
        return resourceId;
    }
    public URI getResourceType()
    {
        return resourceType;
    }
    @Override
    public String getTitle()
    {
        return title;
    }
    @Override
    public String getSubTitle()
    {
        return subTitle;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
