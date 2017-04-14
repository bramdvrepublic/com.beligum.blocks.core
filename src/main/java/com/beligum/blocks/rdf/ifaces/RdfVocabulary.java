package com.beligum.blocks.rdf.ifaces;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.net.URI;
import java.util.Map;
import java.util.Set;

/**
 * Created by bram on 2/28/16.
 */
public interface RdfVocabulary
{
    /**
     * The full namespace URI of this vocabulary
     */
    URI getNamespace();

    /**
     * Resolves the supplied suffix against the full namespace of this vocabulary
     */
    @JsonIgnore
    URI resolve(String suffix);

    /**
     * The abbreviated namespace prefix (without the colon) to instance a CURIE
     * Note that W3C has an official list of predefined prefixes that are always there, together with some popular prefixes:
     * https://www.w3.org/2011/rdfa-context/rdfa-1.1
     */
    String getPrefix();

    /**
     * Resolves the supplied suffix against the curie namespace of this vocabulary
     */
    @JsonIgnore
    URI resolveCurie(String suffix);

    /**
     * Returns all typed members (classes, properties and dataTypes) in this vocabulary
     */
    //avoids infinite recursion
    @JsonIgnore
    Map<URI, RdfResource> getAllTypes();

    /**
     * Returns all classes in this vocabulary
     */
    //avoids infinite recursion
    @JsonIgnore
    Map<URI, RdfClass> getAllClasses();

    /**
     * Returns all classes in this vocabulary that are selectable from the client-side page-type-dropdown
     */
    //avoids infinite recursion
    @JsonIgnore
    Map<URI, RdfClass> getPublicClasses();

    /**
     * Returns all properties in this vocabulary
     */
    //avoids infinite recursion
    @JsonIgnore
    Map<URI, RdfDataType> getAllDataTypes();

    /**
     * Returns all properties in this vocabulary that are accessible from the client-side UI
     */
    //avoids infinite recursion
    @JsonIgnore
    Map<URI, RdfDataType> getPublicDataTypes();

    /**
     * Returns all properties in this vocabulary
     */
    //avoids infinite recursion
    @JsonIgnore
    Map<URI, RdfProperty> getAllProperties();

    /**
     * Returns all properties in this vocabulary that are accessible from the client-side UI
     */
    //avoids infinite recursion
    @JsonIgnore
    Map<URI, RdfProperty> getPublicProperties();

    /**
     * Returns all literals in this vocabulary
     */
    //avoids infinite recursion
    @JsonIgnore
    Set<RdfLiteral> getAllLiterals();

    /**
     * Returns all literals in this vocabulary that are accessible from the client-side UI
     */
    //avoids infinite recursion
    @JsonIgnore
    Set<RdfLiteral> getPublicLiterals();

    /**
     * Call this method to add a class to the vocabulary (probably only during static initialization)
     */
    void addClass(RdfClass rdfClass);

    /**
     * Call this method to add a property to the vocabulary (probably only during static initialization)
     */
    void addProperty(RdfProperty rdfProperty);

    /**
     * Call this method to add a dataType to the vocabulary (probably only during static initialization)
     */
    void addDataType(RdfDataType rdfDataType);

    /**
     * Call this method to add a literal to the vocabulary (probably only during static initialization)
     */
    void addLiteral(RdfLiteral rdfLiteral);
}
