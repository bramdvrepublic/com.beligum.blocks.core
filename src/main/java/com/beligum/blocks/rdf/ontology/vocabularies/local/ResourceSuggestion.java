package com.beligum.blocks.rdf.ontology.vocabularies.local;

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
    private URI page;
    private String title;
    private String subTitle;

    //-----CONSTRUCTORS-----
    public ResourceSuggestion(URI resourceId, URI resourceType, URI page, String title, String subTitle)
    {
        this.resourceId = resourceId;
        this.resourceType = resourceType;
        this.title = title;
        this.subTitle = subTitle;
        this.page = page;
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getValue()
    {
        return resourceId == null ? null : resourceId.toString();
    }
    @Override
    public URI getResourceType()
    {
        return resourceType;
    }
    @Override
    public URI getPublicPage()
    {
        return page;
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
