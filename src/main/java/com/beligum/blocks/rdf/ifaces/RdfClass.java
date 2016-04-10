package com.beligum.blocks.rdf.ifaces;

import com.beligum.blocks.endpoints.ifaces.RdfQueryEndpoint;
import com.beligum.blocks.fs.index.entries.resources.ResourceIndexEntry;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.net.URI;
import java.util.Set;

/**
 * This is more or less the OO representation of the RDFS::Class
 *
 * Created by bram on 2/26/16.
 */
public interface RdfClass extends RdfResource
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
    //note: data (URI and prefix) serialized in getFullName and getCurieName
    @JsonIgnore
    RdfVocabulary getVocabulary();

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
     * Optional (can be null) list of other ontology URIs that describe the same concept of the class described by this class.
     * Eg. http://dbpedia.org/page/Water_well
     */
    URI[] getIsSameAs();

    /**
     * Factory method to get a reference to the endpoint for this class.
     * The endpoint is used to lookup remote or local values for autocomplete boxes etc. of resources with this type.
     * Can return null to signal there is no such functionality and this class is for syntax/semantic-use only
     */
    @JsonIgnore
    RdfQueryEndpoint getEndpoint();

    /**
     * A collection of all properties of this class. This list is returned by the RDF endpoint when the properties for a page of type 'this' is requested.
     * When this method returns null, all RdfProperties known to this server are returned. When an empty list is returned, this class doesn't have any properties.
     */
    @JsonIgnore
    Set<RdfProperty> getProperties();

    /**
     * A subclass of ResourceIndexEntry to use when building the java-object when the RDF triples come back from the triple store.
     * If this is null, a generic SimpleResourceIndexEntry is used.
     */
    @JsonIgnore
    Class<? extends ResourceIndexEntry> getResourceIndexClass();

    /**
     * Setter for the value above. Note that this was pulled out of the constructor and made public to allow us to set it in a separate mapping.
     * This was because there was a cyclic static initialization dependency while creating the objects: terms relied on classes which relied on terms (to initialize this set).
     */
    @JsonIgnore
    void setProperties(Set<RdfProperty> properties);
}
