package com.beligum.blocks.rdf.validation.ifaces;

import com.beligum.blocks.exceptions.RdfValidationException;

/**
 * This is basically a pre-parsed sesame RDF model where all statements about relevant and known ontologies
 * have been converted to a hierarchical class model for easy validation and multiplicity checking.
 *
 * Created by bram on May 21, 2019
 */
public interface RdfModel
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----PUBLIC METHODS-----

    /**
     * Returns all instances of RdfClass in this model
     */
    Iterable<RdfClassInstance> getInstances();

    /**
     * Recursively validate all members of this model
     */
    void validate() throws RdfValidationException;

}
