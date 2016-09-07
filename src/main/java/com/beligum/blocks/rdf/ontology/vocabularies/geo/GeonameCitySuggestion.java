package com.beligum.blocks.rdf.ontology.vocabularies.geo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.StringUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by bram on 3/12/16.
 */
public class GeonameCitySuggestion extends AbstractGeonameSuggestion
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    //the least specific parent, just below country
    private String adminName1;
    private String adminName2;
    private String adminName3;
    private String adminName4;
    //the closest parent: most specific
    private String adminName5;
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
        final int MAX_SPECIFIC_LEVEL = 2;
        int specificationLevel = 0;
        StringBuilder descriptiveName = new StringBuilder();
        Set<String> specifications = new HashSet<>();
        specifications.add(this.name);

        specificationLevel = this.specify(descriptiveName, specifications, MAX_SPECIFIC_LEVEL, specificationLevel, this.toponymName);
        specificationLevel = this.specify(descriptiveName, specifications, MAX_SPECIFIC_LEVEL, specificationLevel, this.adminName5);
        specificationLevel = this.specify(descriptiveName, specifications, MAX_SPECIFIC_LEVEL, specificationLevel, this.adminName4);
        specificationLevel = this.specify(descriptiveName, specifications, MAX_SPECIFIC_LEVEL, specificationLevel, this.adminName3);
        specificationLevel = this.specify(descriptiveName, specifications, MAX_SPECIFIC_LEVEL, specificationLevel, this.adminName2);
        specificationLevel = this.specify(descriptiveName, specifications, MAX_SPECIFIC_LEVEL, specificationLevel, this.adminName1);

        return descriptiveName.toString() + ", " + (this.getCountryName() == null ? this.getCountryCode() : this.getCountryName());
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private int specify(StringBuilder descriptiveName, Set<String> specifications, final int MAX_SPECIFIC_LEVEL, int specificationLevel, String value)
    {
        //Note: don't add empty values and don't add doubles
        if (specificationLevel<MAX_SPECIFIC_LEVEL && !StringUtils.isEmpty(value) && !specifications.contains(value)) {
            if (descriptiveName.length()!=0) {
                descriptiveName.append(", ");
            }
            descriptiveName.append(value);

            specifications.add(value);
            specificationLevel++;
        }

        return specificationLevel;
    }
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
    private String getAdminName2()
    {
        return adminName2;
    }
    @JsonProperty
    private void setAdminName2(String adminName2)
    {
        this.adminName2 = adminName2;
    }
    @JsonIgnore
    private String getAdminName3()
    {
        return adminName3;
    }
    @JsonProperty
    private void setAdminName3(String adminName3)
    {
        this.adminName3 = adminName3;
    }
    @JsonIgnore
    private String getAdminName4()
    {
        return adminName4;
    }
    @JsonProperty
    private void setAdminName4(String adminName4)
    {
        this.adminName4 = adminName4;
    }
    @JsonIgnore
    private String getAdminName5()
    {
        return adminName5;
    }
    @JsonProperty
    private void setAdminName5(String adminName5)
    {
        this.adminName5 = adminName5;
    }
}
