package com.beligum.blocks.rdf.ifaces;

import com.hp.hpl.jena.rdf.model.Model;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by bram on 1/23/16.
 */
public interface Exporter
{
    //-----CONSTANTS-----
    enum Format
    {
        JSONLD;
    }

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    void exportModel(Model model, OutputStream outputStream) throws IOException;

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
