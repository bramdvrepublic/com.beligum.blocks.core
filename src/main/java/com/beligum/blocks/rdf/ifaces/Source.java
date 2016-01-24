package com.beligum.blocks.rdf.ifaces;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Created by bram on 1/23/16.
 */
public interface Source
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    URI getBaseUri();
    InputStream openNewInputStream() throws IOException;

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
