package com.beligum.blocks.rdf.validation.ifaces;

import com.beligum.blocks.rdf.ifaces.RdfProperty;
import org.eclipse.rdf4j.model.Value;

/**
 * This contains information about the particular instance of an RDF property
 *
 * Created by bram on May 21, 2019
 */
public interface RdfPropertyInstance
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----PUBLIC METHODS-----
    /**
     * The parent instance context of this property instance
     */
    RdfClassInstance getContext();

    /**
     * The type of this instance
     */
    RdfProperty getType();

    /**
     * The value of this property instance
     */
    Value getValue();

    /**
     * Validates this class instance
     */
    void validate();

}
