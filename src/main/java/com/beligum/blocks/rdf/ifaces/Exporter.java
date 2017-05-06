package com.beligum.blocks.rdf.ifaces;

import org.eclipse.rdf4j.model.Model;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by bram on 1/23/16.
 */
public interface Exporter
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    void exportModel(Model model, OutputStream outputStream) throws IOException;

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
