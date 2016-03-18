package com.beligum.blocks.rdf.ontology.vocabularies.geo;

import com.beligum.blocks.config.RdfFactory;
import com.beligum.blocks.endpoints.ifaces.AutocompleteSuggestion;
import com.beligum.blocks.utils.RdfTools;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;

/**
 * Created by bram on 3/12/16.
 */
public abstract class AbstractGeonameSuggestion extends AbstractGeoname implements AutocompleteSuggestion
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    protected URI resourceType;
    protected String geonameId;
    protected String name;
    protected String toponymName;

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    public URI getResourceId()
    {
        return RdfTools.createRelativeResourceId(RdfFactory.getClassForResourceType(this.getResourceType()), geonameId);
    }
    @Override
    public URI getResourceType()
    {
        return resourceType;
    }
    @Override
    public String getTitle()
    {
        return name;
    }
    @Override
    public String getSubTitle()
    {
        return toponymName;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    @JacksonInject(RESOURCE_TYPE_INJECTABLE)
    private void setResourceType(URI resourceType)
    {
        this.resourceType = resourceType;
    }
    @JsonIgnore
    private String getGeonameId()
    {
        return geonameId;
    }
    @JsonProperty
    private void setGeonameId(String geonameId)
    {
        this.geonameId = geonameId;
    }
    @JsonIgnore
    private String getName()
    {
        return name;
    }
    @JsonProperty
    private void setName(String name)
    {
        this.name = name;
    }
    @JsonIgnore
    private String getToponymName()
    {
        return toponymName;
    }
    @JsonProperty
    private void setToponymName(String toponymName)
    {
        this.toponymName = toponymName;
    }
}
