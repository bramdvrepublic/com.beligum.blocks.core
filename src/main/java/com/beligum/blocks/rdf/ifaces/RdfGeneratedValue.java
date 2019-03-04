package com.beligum.blocks.rdf.ifaces;

import java.io.IOException;

/**
 * Created by Bram on 29/06/17.
 * <p>
 * Support for default values in widgets.
 *
 * @see com.beligum.blocks.rdf.ontology.RdfPropertyImpl#RdfPropertyImpl
 */
public interface RdfGeneratedValue
{
    /**
     * Get the generated
     *
     * @return the generated rdf value
     * @see com.beligum.blocks.rdf.ontology.RdfPropertyImpl#getGeneratedValue
     */
    String getValue(String parameter) throws IOException;
}