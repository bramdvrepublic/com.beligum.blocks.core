package com.beligum.blocks.endpoints.beans;

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

    //-----CONSTRUCTORS-----
    public GeonameCitySuggestion()
    {
    }

    //-----PUBLIC METHODS-----

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
}
