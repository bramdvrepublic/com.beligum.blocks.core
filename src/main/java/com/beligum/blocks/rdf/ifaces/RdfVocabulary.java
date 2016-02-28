package com.beligum.blocks.rdf.ifaces;

import java.net.URI;

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
     */
    String getPrefix();
}
