package com.beligum.blocks.rdf.ifaces;

import com.beligum.blocks.filesystem.pages.PageModel;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * RDF mapper (currently only to JSON) that translates a Sesame RDF model to another format
 */
public interface RdfMapper
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----PUBLIC METHODS-----
    /**
     * Converts the RDF model of a page to JSON.
     * The implementation decides how this is done.
     */
    JsonNode toJson(Page page) throws IOException;

}
