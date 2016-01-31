package com.beligum.blocks.fs.pages.ifaces;

import com.beligum.blocks.fs.metadata.ifaces.MetadataWriter;
import com.beligum.blocks.rdf.ifaces.Exporter;
import com.beligum.blocks.rdf.ifaces.Importer;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.hadoop.fs.Path;

import java.io.IOException;

/**
 * Created by bram on 1/14/16.
 */
public interface Page
{
    Importer createImporter() throws IOException;
    Exporter createExporter() throws IOException;
    MetadataWriter createMetadataWriter() throws IOException;

    Path getNormalizedPageProxyPath();
    Path getJsonLDProxyPath();

    JsonNode getJsonLDNode();
}
