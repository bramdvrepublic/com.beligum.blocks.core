package com.beligum.blocks.rdf.ontology.vocabularies.geo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.StringUtils;

/**
 * Created by bram on 3/12/16.
 */
public class GeonameCitySuggestion extends AbstractGeonameSuggestion
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private String adminName1;
    private String adminCode1;
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
        StringBuilder descriptiveName = new StringBuilder();

        if (this.toponymName!=null && !this.toponymName.equals(this.name)) {
            descriptiveName.append(toponymName);
        }

        if (!StringUtils.isEmpty(this.adminName1)) {
            if (descriptiveName.length()!=0) {
                descriptiveName.append(", ");
            }
            descriptiveName.append(this.adminName1);
        }

        return descriptiveName.toString() + ", " + (this.getCountryName() == null ? this.getCountryCode() : this.getCountryName());
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
    @JsonIgnore
    private String getAdminName1()
    {
        return adminName1;
    }
    @JsonProperty
    private void setAdminName1(String adminName1)
    {
        this.adminName1 = adminName1;
    }
    @JsonIgnore
    private String getAdminCode1()
    {
        return adminCode1;
    }
    @JsonProperty
    private void setAdminCode1(String adminCode1)
    {
        this.adminCode1 = adminCode1;
    }
}
