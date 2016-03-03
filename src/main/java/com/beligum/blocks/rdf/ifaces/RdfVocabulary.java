package com.beligum.blocks.rdf.ifaces;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.net.URI;
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
     * The abbreviated namespace prefix (without the colon) to create a CURIE
     * Note that W3C has an official list of predefined prefixes that are always there, together with some popular prefixes:
     * https://www.w3.org/2011/rdfa-context/rdfa-1.1
     */
    String getPrefix();

    /**
     * Returns all classes in this vocabulary
     */
    //avoids infinite recursion
    @JsonIgnore
    Set<RdfClass> getClasses();

    /**
     * Returns all properties in this vocabulary
     */
    //avoids infinite recursion
    @JsonIgnore
    Set<RdfDataType> getDataTypes();

    /**
     * Returns all properties in this vocabulary
     */
    //avoids infinite recursion
    @JsonIgnore
    Set<RdfProperty> getProperties();

    /**
     * Returns all literals in this vocabulary
     */
    //avoids infinite recursion
    @JsonIgnore
    Set<RdfLiteral> getLiterals();

    /**
     * Call this method to add a class to the vocabulary (probably only during static initialization)
     */
    void add(RdfResource rdfClass);

}
