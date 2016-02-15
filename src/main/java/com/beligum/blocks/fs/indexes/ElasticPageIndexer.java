package com.beligum.blocks.fs.indexes;

import com.beligum.base.utils.json.Json;
import com.beligum.blocks.fs.indexes.ifaces.PageIndexer;
import com.beligum.blocks.fs.pages.ifaces.Page;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by bram on 1/26/16.
 *
 * Some interesting reads:
 *
 *   https://groups.google.com/forum/#!msg/elasticsearch/hPu1e7TrL40/K8ORXOoWWe4J
 *
 *
 */
public class ElasticPageIndexer implements PageIndexer
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    public void indexPage(Page page) throws IOException
    {
        // read it back in and parse it because it's the link between this (where we have HDFS access)
        // and the page indexer (where we work with generic json objects)
        JsonNode jsonLd = null;
        try (InputStream is = page.getPathInfo().getFileContext().open(page.getRdfExportFile())) {
            jsonLd = Json.read(is, JsonNode.class);
        }

        //TODO: do something with it ;-)
        //Logger.info(Json.getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(jsonLd));
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
