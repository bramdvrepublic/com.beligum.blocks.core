package com.beligum.blocks.rdf.ifaces;

import java.io.InputStream;
import java.net.URI;

/**
 * Created by bram on 1/23/16.
 */
public interface Source extends AutoCloseable
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    URI getUri();
    URI getBaseUri();
    InputStream getInputStream();

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
