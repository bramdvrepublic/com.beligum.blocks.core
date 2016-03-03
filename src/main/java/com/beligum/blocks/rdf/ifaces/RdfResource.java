package com.beligum.blocks.rdf.ifaces;

import com.beligum.base.models.ifaces.JsonObject;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.net.URI;

/**
 * This is the top-level interface for all RDF(S) related classes
 *
 * Created by bram on 3/2/16.
 */
public interface RdfResource extends JsonObject
{
    /**
     * The short, capitalized and camel-cased name that needs to be appended to the vocab to get the full describing URI for this resource.
     * Eg. WaterWell
     */
    String getName();

    /**
     * The site-specific ontology URI for this resource. Together with the name, it forms the full URI.
     * Eg. http://www.reinvention.be/ontology/
     */
    //note: data (URI and prefix) serialized in getFullName and getCurieName
    @JsonIgnore
    RdfVocabulary getVocabulary();

    /**
     * The full, absolute URI of this resource that is built from the vocabulary URI and the name
     * Eg. http://www.reinvention.be/ontology/WaterWell
     */
    URI getFullName();

    /**
     * The full, absolute URI of this resource that is built from the vocabulary CURIE and the name
     * Eg. mot:WaterWell
     */
    URI getCurieName();
}
