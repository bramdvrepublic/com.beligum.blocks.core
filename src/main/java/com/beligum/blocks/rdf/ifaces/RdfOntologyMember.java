package com.beligum.blocks.rdf.ifaces;

import com.beligum.base.filesystem.MessagesFileEntry;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.net.URI;

/**
 * On top of the RdfResource, an ontology member has an ontology, ontology-related names, and a few general purpose extras.
 */
public interface RdfOntologyMember extends RdfResource
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----PUBLIC METHODS-----
    /**
     * The site-specific ontology URI for this class. Together with the name, it forms the full URI.
     * Eg. http://www.reinvention.be/ontology/
     */
    //note: data (URI and prefix) serialized in getFullName and getCurieName
    @JsonIgnore
    RdfOntology getOntology();

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
     * This is the admin-side of this value; returns the key to this resource bundle
     */
    String getTitleKey();

    /**
     * The human readable describing phrase for this class, to be used to build admin-side selection lists etc.
     * Eg. Water well
     */
    String getTitle();

    /**
     * The human readable describing phrase for this class, to be used to build admin-side selection lists etc.
     * This is a more low-level (eg. API) accessor to this value, so know what you're doing.
     */
    @JsonIgnore
    MessagesFileEntry getTitleMessage();

    /**
     * The human readable describing phrase for this class, to be used in public HTML pages as a describing label next to the value of this class.
     * This is the admin-side of this value; returns the key to this resource bundle
     */
    String getLabelKey();

    /**
     * The human readable describing phrase for this class, to be used in public HTML pages as a describing label next to the value of this class.
     * Eg. Water well
     */
    String getLabel();

    /**
     * The human readable describing phrase for this class, to be used in public HTML pages as a describing label next to the value of this class.
     * This is a more low-level (eg. API) accessor to this value, so know what you're doing.
     */
    @JsonIgnore
    MessagesFileEntry getLabelMessage();

    /**
     * Optional (can be null) list of other ontology URIs that describe the same concept of the class described by this class.
     * Eg. http://dbpedia.org/page/Water_well
     */
    @JsonIgnore
    URI[] getIsSameAs();
}
