package com.beligum.blocks.serializing;

import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.templating.HtmlTemplate;
import com.beligum.blocks.templating.TagTemplate;

import java.io.IOException;
import java.util.Locale;

/**
 * Created by bram on Aug 19, 2019
 */
public interface BlockSerializer
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----PUBLIC METHODS-----
    /**
     * This is the most general contract an importer class must provide to import data.
     * This method returns the serialized and normalized (!) HTML of a block with the supplied data, property and language, ready to be used in page templates.
     */
    CharSequence toHtml(TagTemplate blockType, RdfProperty property, Locale language, String value) throws IOException;
}
