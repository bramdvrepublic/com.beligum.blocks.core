package com.beligum.blocks.rdf.ontology.vocabularies.geo;

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
        //Note: see the Geonames ontology mapping for these values:
        // http://www.geonames.org/ontology/mappings_v3.01.rdf
        // Especially, we want this value to match the OWL restrictions that map to http://schema.org/Country
        COUNTRY(new String[] { "A" }, new String[] { "PCLI" }, GeonameCountrySuggestion.class),

        //Note: see the Geonames ontology mapping for these values:
        // http://www.geonames.org/ontology/mappings_v3.01.rdf
        // Especially, we want this value to match the OWL restrictions that map to http://schema.org/City
        CITY(new String[] { "P" }, new String[] {
                        "PPL",
                        //for one reason of the other (eg. London), capitals aren't found that easily...
                        "PPLC"
        }, GeonameCitySuggestion.class);

        public String[] featureClasses;
        public String[] featureCodes;
        public Class<? extends AutocompleteSuggestion> suggestionClass;
        Type(String[] featureClasses, String[] featureCodes, Class<? extends AutocompleteSuggestion> suggestionClass)
        {
            this.featureClasses = featureClasses;
            this.featureCodes = featureCodes;
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
        return geonameUriStr.substring(GEONAMES_URI_PREFIX.length(), geonameUriStr.length() - GEONAMES_URI_SUFFIX.length());
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
