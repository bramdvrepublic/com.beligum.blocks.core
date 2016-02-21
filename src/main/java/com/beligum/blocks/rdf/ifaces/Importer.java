package com.beligum.blocks.rdf.ifaces;

import org.openrdf.model.Model;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Created by bram on 1/23/16.
 */
public interface Importer
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    Model importDocument(Source source) throws IOException;
    Model importDocument(InputStream inputStream, URI baseUri) throws IOException;

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
