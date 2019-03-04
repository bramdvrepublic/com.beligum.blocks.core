package com.beligum.blocks.rdf.ifaces;

import java.net.URI;

public interface RdfNamespace
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----PUBLIC METHODS-----
    /**
     * The full namespace URI of this vocabulary
     */
    URI getUri();

    /**
     * The abbreviated namespace prefix (without the colon) to instance a CURIE
     * Note that W3C has an official list of predefined prefixes that are always there, together with some popular prefixes:
     * https://www.w3.org/2011/rdfa-context/rdfa-1.1
     */
    String getPrefix();
}
