package com.beligum.blocks.rdf.ifaces;

import com.beligum.base.models.ifaces.JsonObject;

import java.net.URI;

/**
 * Created by bram on 2/26/16.
 */
public interface RdfClass extends JsonObject
{
    /**
     * The short, capitalized and camel-cased name that needs to be appended to the vocab to get the full describing URI for this class.
     * Eg. WaterWell
     */
    String getName();

    /**
     * The site-specific ontology URI for this class. Together with the name, it forms the full URI.
     * Eg. http://www.reinvention.be/ontology/
     */
    URI getVocabulary();

    /**
     * The site-specific ontology prefix of the full vocabulary URI for this class. Together with the name, it forms the CURIE.
     * Note: don't add the colon.
     * Eg. mot
     */
    String getVocabularyPrefix();

    /**
     * The full, absolute URI of this class that is built from the vocabulary URI and the name
     * Eg. http://www.reinvention.be/ontology/WaterWell
     */
    URI getFullName();

    /**
     * The full, absolute URI of this class that is built from the vocabulary CURIE and the name
     * Eg. mot:WaterWell
     */
    URI getCurieName();

    /**
     * The human readable describing phrase for this class, to be used to build admin-side selection lists etc.
     * Eg. Water well
     */
    String getTitle();

    /**
     * The human readable describing phrase for this class, to be used in public HTML pages as a describing label next to the value of this class.
     * Eg. Water well
     */
    String getLabel();

    /**
     * Optional (can be null) list of other ontology URIs that describe the same concept of the class described by this class.
     * Eg. http://dbpedia.org/page/Water_well
     */
    URI[] getIsSameAs();
}
