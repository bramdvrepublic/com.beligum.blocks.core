package com.beligum.blocks.rdf.ifaces;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Created by Bram on 29/06/17.
 * <p>
 * Support for default values in widgets.
 *
 * @see com.beligum.blocks.rdf.ontology.RdfPropertyImpl#RdfPropertyImpl
 */
public interface RdfInitialValue
{
    /**
     * Get the default value
     *
     * @return the default rdf value
     */
    String getValue() throws URISyntaxException, IOException;
}