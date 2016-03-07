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
     * The name of the static singleton class variable that holds an instance of this vocabulary
     */
    String INSTANCE_FIELD_NAME = "INSTANCE";

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
     * Returns all classes in this vocabulary that are accessible from the client-side UI
     */
    //avoids infinite recursion
    @JsonIgnore
    Set<RdfClass> getPublicClasses();

    /**
     * Returns all properties in this vocabulary that are accessible from the client-side UI
     */
    //avoids infinite recursion
    @JsonIgnore
    Set<RdfDataType> getPublicDataTypes();

    /**
     * Returns all properties in this vocabulary that are accessible from the client-side UI
     */
    //avoids infinite recursion
    @JsonIgnore
    Set<RdfProperty> getPublicProperties();

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
