package com.beligum.blocks.fs.pages.ifaces;

import com.beligum.blocks.fs.ifaces.PathInfo;
import com.beligum.blocks.fs.metadata.ifaces.MetadataWriter;
import com.beligum.blocks.rdf.ifaces.Exporter;
import com.beligum.blocks.rdf.ifaces.Importer;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.net.URI;

/**
 * Created by bram on 1/14/16.
 */
public interface Page
{
    URI getUri();
    Importer createImporter() throws IOException;
    Exporter createExporter() throws IOException;
    MetadataWriter createMetadataWriter() throws IOException;
    Path getNormalizedPageProxyPath();
    Path getRdfExportFile();
//    Model getRDFModel();
//    HtmlSource getSource();
    PathInfo getPathInfo();
}
