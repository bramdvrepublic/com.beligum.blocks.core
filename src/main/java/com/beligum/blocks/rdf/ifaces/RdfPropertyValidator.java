package com.beligum.blocks.rdf.ifaces;

import com.beligum.blocks.exceptions.RdfValidationException;
import com.beligum.blocks.rdf.validation.ifaces.RdfPropertyInstance;

/**
 * Created by bram on May 20, 2019
 */
public interface RdfPropertyValidator
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----PUBLIC METHODS-----
    /**
     * This will be called before persisting RDF properties to the triple store
     *
     * @throws RdfValidationException
     */
    void validate(RdfPropertyInstance propertyInstance) throws RdfValidationException;

}
