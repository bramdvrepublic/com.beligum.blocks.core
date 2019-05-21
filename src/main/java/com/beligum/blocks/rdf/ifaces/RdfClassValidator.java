package com.beligum.blocks.rdf.ifaces;

import com.beligum.blocks.exceptions.RdfValidationException;
import com.beligum.blocks.rdf.validation.ifaces.RdfClassInstance;
import com.beligum.blocks.rdf.validation.ifaces.RdfPropertyInstance;

/**
 * Created by bram on May 20, 2019
 */
public interface RdfClassValidator
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----PUBLIC METHODS-----
    /**
     * This will be called before persisting RDF class instances to the triple store
     *
     * @throws RdfValidationException
     */
    void validate(RdfClassInstance classInstance) throws RdfValidationException;

}
