package com.beligum.blocks.rdf.ontology.vocabularies.geo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by bram on 3/12/16.
 */
public class GeonameCitySuggestion extends AbstractGeonameSuggestion
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private String countryCode;
    private String countryName;

    //-----CONSTRUCTORS-----
    public GeonameCitySuggestion()
    {
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getSubTitle()
    {
        return toponymName + ", " + (this.getCountryName() == null ? this.getCountryCode() : this.getCountryName());
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    @JsonIgnore
    private String getCountryCode()
    {
        return countryCode;
    }
    @JsonProperty
    private void setCountryCode(String countryCode)
    {
        this.countryCode = countryCode;
    }
    @JsonIgnore
    private String getCountryName()
    {
        return countryName;
    }
    @JsonProperty
    private void setCountryName(String countryName)
    {
        this.countryName = countryName;
    }
}
