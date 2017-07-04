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
        COUNTRY(new String[] { "A" }, new String[] {
                        //independent political entity
                        "PCLI",
                        //historical political entity
                        "PCLH"
        }, GeonameCountrySuggestion.class),

        //Note: see the Geonames ontology mapping for these values:
        // http://www.geonames.org/export/codes.html
        // http://www.geonames.org/ontology/mappings_v3.01.rdf
        // Especially, we want this value to match the OWL restrictions that map to http://schema.org/City
        CITY(new String[] { "P" }, new String[] {
                        //populated place
                        "PPL",
                        //seat of a first-order administrative division
                        "PPLA",
                        //seat of a second-order administrative division
                        "PPLA2",
                        //seat of a third-order administrative division
                        "PPLA3",
                        //seat of a fourth-order administrative division
                        "PPLA4",
                        //capital of a political entity
                        "PPLC",
                        //farm village; a populated place where the population is largely engaged in agricultural activities
                        "PPLF",
                        //seat of government of a political entity
                        "PPLG",
                        //populated locality; an area similar to a locality but with a small group of dwellings or other buildings
                        "PPLL",
                        //populated places; cities, towns, villages, or other agglomerations of buildings where people live and work
                        "PPLS",
                        //section of populated place
                        "PPLX"
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
