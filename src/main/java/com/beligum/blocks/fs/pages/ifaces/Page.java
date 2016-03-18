package com.beligum.blocks.fs.pages.ifaces;

import com.beligum.blocks.fs.ifaces.ResourcePath;
import com.beligum.blocks.fs.metadata.ifaces.MetadataWriter;
import com.beligum.blocks.rdf.ifaces.Exporter;
import com.beligum.blocks.rdf.ifaces.Format;
import com.beligum.blocks.rdf.ifaces.Importer;
import com.beligum.blocks.rdf.sources.HtmlSource;
import com.beligum.blocks.templating.blocks.HtmlAnalyzer;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.net.URI;

/**
 * Created by bram on 1/14/16.
 */
public interface Page
{
    /**
     * Computes (and caches) the public, full (domain-prefixed) URL of this page, based on the relative path name of this page on the server.
     * @return
     * @throws IOException
     */
    URI buildAbsoluteAddress() throws IOException;

    /**
     * Computes (and caches) the public, relative (so without domain) URL of this page, based on the relative path name of this page on the server.
     * @return
     * @throws IOException
     */
    URI buildRelativeAddress() throws IOException;

    /**
     * Creates the semantic importer for this kind of page (currently an RDFa processor)
     * @return
     * @throws IOException
     */
    Importer createImporter(Format importFormat) throws IOException;

    /**
     * Creates the semantic exporter for this kind of page, to store to disk (currently a JSON-LD writer).
     * @return
     * @throws IOException
     */
    Exporter createExporter(Format exportFormat) throws IOException;

    /**
     * Creates a new analyzer, based on the currently stored original source for this page, reads it in and parses it.
     * Note: this is a very expensive operation, use with care.
     * @return the initialized and parsed analyzer to be used to extract meta info from the stored html.
     * @throws IOException
     */
    HtmlAnalyzer createAnalyzer() throws IOException;

    /**
     * Creates a new html (stream) source based on the stored original file on disk.
     * @return
     */
    HtmlSource readOriginalHtml() throws IOException;

    /**
     * Create the metadata writer for this page to writer to the "meta" file in the meta dot folder.
     * @return
     * @throws IOException
     */
    MetadataWriter createMetadataWriter() throws IOException;

    /**
     * Returns the path where the normalized html of this page is stored locally.
     * @return
     */
    Path getNormalizedPageProxyPath();

    /**
     * Returns the local loction where the output of the Exporter should be stored.
     * @return
     */
    Path getRdfExportFile();

    /**
     * Returns the type in which the RDF export is saved
     * @return
     */
    Format getRdfExportFileFormat();

    /**
     * Returns the path (and all other info) to the local resource file on disk.
     * @return
     */
    ResourcePath getResourcePath();
}
