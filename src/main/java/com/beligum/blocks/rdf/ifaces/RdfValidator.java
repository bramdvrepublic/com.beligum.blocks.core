package com.beligum.blocks.rdf.ifaces;

import com.beligum.blocks.exceptions.RdfValidationException;

/**
 * Created by bram on May 20, 2019
 */
public interface RdfValidator
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----PUBLIC METHODS-----
    /**
     * This will be called before persisting RDF ontology members to the triple store
     *
     * @throws RdfValidationException
     */
    void validate(RdfOntologyMember rdfOntologyMember) throws RdfValidationException;

}
