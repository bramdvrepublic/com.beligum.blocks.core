package com.beligum.blocks.rdf.ifaces;

import com.hp.hpl.jena.rdf.model.Model;

import java.io.IOException;

/**
 * Created by bram on 1/23/16.
 */
public interface Importer
{
    //-----CONSTANTS-----
    enum Format
    {
        RDFA
    }

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    Model importDocument(Source source) throws IOException;

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
