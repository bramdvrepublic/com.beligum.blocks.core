package com.beligum.blocks.importer;

import com.beligum.blocks.rdf.ifaces.RdfProperty;

import java.io.IOException;
import java.util.Locale;

/**
 * Created by bram on Aug 19, 2019
 */
public interface BlocksImporter
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----PUBLIC METHODS-----
    /**
     * This is the most general contract an importer class must provide to import data.
     * This method returns the serialized HTML of a block with the supplied data, property and language, ready to be used in page templates.
     */
    String serialize(RdfProperty property, Locale language, String value) throws IOException;
}
