package com.beligum.blocks.rdf.ifaces;

import org.eclipse.rdf4j.model.Model;

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
    Model importDocument(URI baseUri, InputStream inputStream) throws IOException;

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
