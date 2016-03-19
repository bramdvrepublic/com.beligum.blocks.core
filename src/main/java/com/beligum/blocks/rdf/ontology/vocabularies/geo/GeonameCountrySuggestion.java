package com.beligum.blocks.rdf.ontology.vocabularies.geo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by bram on 3/12/16.
 */
public class GeonameCountrySuggestion extends AbstractGeonameSuggestion
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    protected String countryName;

    //-----CONSTRUCTORS-----
    public GeonameCountrySuggestion()
    {
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getTitle()
    {
        String retVal = name;

        //sometimes the name of a country is completely different from what you expect (different language),
        //so we add clarification here. Also in subtitle, but that's always the full form (eg. "Republic of ...")
        if (!StringUtils.isEmpty(this.countryName) && !this.countryName.equals(this.name)) {
            //makes sense to use the official country name as the primary return value and add the searched-for value between brackets
            retVal = this.countryName+" ("+this.name+")";
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
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
