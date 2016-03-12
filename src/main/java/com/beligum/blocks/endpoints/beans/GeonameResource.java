package com.beligum.blocks.endpoints.beans;

import com.beligum.blocks.endpoints.ifaces.AutocompleteValue;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.util.List;

/**
 * Created by bram on 3/12/16.
 */
public class GeonameResource extends AbstractGeoname implements AutocompleteValue
{
    //-----CONSTANTS-----
    //special value for 'lang' that maps to external documentation
    private static final String LINK_LANGUAGE = "link";

    //-----VARIABLES-----
    private URI resourceType;
    private String name;
    private String toponymName;
    private String geonameId;
    private List<GeonameLangValue> alternateName;

    //temp values...
    private transient boolean triedLink;
    private transient URI cachedLink;

    //-----CONSTRUCTORS-----
    public GeonameResource()
    {
    }

    //-----PUBLIC METHODS-----
    @Override
    public URI getResourceUri()
    {
        return URI.create(GEONAMES_URI_PREFIX + this.geonameId + GEONAMES_URI_SUFFIX);
    }
    @Override
    public URI getResourceType()
    {
        return resourceType;
    }
    @Override
    public String getText()
    {
        return name;
    }
    @Override
    public URI getLink()
    {
        if (!this.triedLink) {
            for (GeonameLangValue val : this.alternateName) {
                if (val != null && val.getLang() != null && val.getLang().equals(LINK_LANGUAGE)) {
                    this.cachedLink = URI.create(val.getValue());
                    //we stop at first sight of a link
                    break;
                }
            }

            this.triedLink = true;
        }

        return this.cachedLink;
    }

    //-----PROTECTED METHODS-----
    //see http://stackoverflow.com/questions/11872914/write-only-properties-with-jackson
    @JacksonInject(RESOURCE_TYPE_INJECTABLE)
    private void setResourceType(URI resourceType)
    {
        this.resourceType = resourceType;
    }
    @JsonProperty
    private void setName(String name)
    {
        this.name = name;
    }
    @JsonProperty
    private void setToponymName(String toponymName)
    {
        this.toponymName = toponymName;
    }
    @JsonProperty
    private void setGeonameId(String geonameId)
    {
        this.geonameId = geonameId;
    }
    @JsonProperty
    private void setAlternateName(List<GeonameLangValue> alternateName)
    {
        this.alternateName = alternateName;
    }
    @JsonIgnore
    private String getName()
    {
        return name;
    }
    @JsonIgnore
    private String getToponymName()
    {
        return toponymName;
    }
    @JsonIgnore
    private String getGeonameId()
    {
        return geonameId;
    }
    @JsonIgnore
    private List<GeonameLangValue> getAlternateName()
    {
        return alternateName;
    }
    @JsonIgnore
    private boolean isTriedLink()
    {
        return triedLink;
    }
    @JsonIgnore
    private URI getCachedLink()
    {
        return cachedLink;
    }

    //-----PRIVATE METHODS-----

}
