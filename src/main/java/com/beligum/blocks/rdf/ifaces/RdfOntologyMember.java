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
     * This will only return false when this member was passed through the Builder.create() method
     * and thus converted from a proxy/stub to a fully initialized instance.
     */
    @JsonIgnore
    boolean isProxy();

    /**
     * The site-specific ontology URI for this class. Together with the name, it forms the full URI.
     * Eg. http://www.reinvention.be/ontology/
     */
    //note: data (URI and prefix) serialized in getUri and getCurie
    @JsonIgnore
    RdfOntology getOntology();

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

    /**
     * If this returns true, this should be (automatically) added to all publicly available members of our local public ontologies.
     */
    boolean isDefault();

    /**
     * Factory method to get a reference to the endpoint for this class.
     * The endpoint is used to lookup remote or local values for autocomplete boxes etc. of resources with this type.
     * Can return null to signal there is no such functionality and this class is for syntax/semantic-use only
     */
    @JsonIgnore
    RdfEndpoint getEndpoint();
}
