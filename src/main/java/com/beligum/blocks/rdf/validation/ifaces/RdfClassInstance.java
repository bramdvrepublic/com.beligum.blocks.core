package com.beligum.blocks.rdf.validation.ifaces;

import com.beligum.blocks.exceptions.RdfValidationException;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;

import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * This contains information about the particular instance of an RDF class
 *
 * Created by bram on May 21, 2019
 */
public interface RdfClassInstance
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----PUBLIC METHODS-----
    /**
     * The main resource this instance contains information about
     */
    URI getSubject();

    /**
     * The type of this instance
     */
    RdfClass getType();

    /**
     * The list of property instance of this instance
     */
    Iterable<RdfPropertyInstance> getProperties();

    /**
     * Validates this class instance
     */
    void validate() throws RdfValidationException;
}
