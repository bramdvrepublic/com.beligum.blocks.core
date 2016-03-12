package com.beligum.blocks.endpoints.beans;

import com.beligum.blocks.endpoints.ifaces.AutocompleteSuggestion;

import java.net.URI;

/**
 * Created by bram on 3/12/16.
 */
public abstract class AbstractGeoname
{
    //-----CONSTANTS-----
    public static final String RESOURCE_TYPE_INJECTABLE = "resourceType";

    protected static final String GEONAMES_URI_PREFIX = "http://sws.geonames.org/";
    protected static final String GEONAMES_URI_SUFFIX = "/";

    public enum Type
    {
        COUNTRY("A", GeonameCountrySuggestion.class),
        CITY("P", GeonameCitySuggestion.class)
        ;

        public String featureClass;
        public Class<? extends AutocompleteSuggestion> suggestionClass;
        Type(String featureClass, Class<? extends AutocompleteSuggestion> suggestionClass)
        {
            this.featureClass = featureClass;
            this.suggestionClass = suggestionClass;
        }
    }

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----STATIC METHODS-----
    public static URI toGeonamesUri(String geonameId)
    {
        return URI.create(GEONAMES_URI_PREFIX + geonameId + GEONAMES_URI_SUFFIX);
    }
    public static String fromGeonamesUri(URI geonameUri)
    {
        String geonameUriStr = geonameUri.toString();
        return geonameUriStr.substring(GEONAMES_URI_PREFIX.length(), geonameUriStr.length()-GEONAMES_URI_SUFFIX.length());
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
