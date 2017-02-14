package com.beligum.blocks.filesystem.pages.ifaces;

import com.beligum.blocks.filesystem.ifaces.BlocksResource;
import com.beligum.blocks.filesystem.logger.ifaces.LogWriter;
import com.beligum.blocks.filesystem.metadata.ifaces.MetadataWriter;
import com.beligum.blocks.rdf.ifaces.Exporter;
import com.beligum.blocks.rdf.ifaces.Format;
import com.beligum.blocks.rdf.ifaces.Importer;
import com.beligum.blocks.templating.blocks.analyzer.HtmlAnalyzer;
import org.apache.hadoop.fs.Path;
import org.openrdf.model.Model;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Map;

/**
 * This is a wrapper for the general concept of a HTML page in the blocks system.
 * Note that compared to the concept of a Resource (from the base system), this interface doesn't say anything
 * about the local storage details of this page. See getResource() for those details.
 *
 * Created by bram on 1/14/16.
 */
public interface Page extends BlocksResource
{
    /**
     * Returns the most naked form of this page; eg. stripped from all language params, it's file storage extension (or directory dummy filename, etc).
     * This address is to be used to index this page (together with it's language, but separately).
     * This allows us to easily perform redirects eg. when a requested page doesn't exist for the requested path in the requested language, but does with another language.
     * For example, if the page /en/this/page exists, but the user requests /nl/this/page, we can easily lookup the second page using it's canonical address /this/page
     */
    URI getCanonicalAddress();

    /**
     * returns the public, full (domain-prefixed) URL of this page, based on the relative path name of this page on the server.
     */
    URI getPublicAbsoluteAddress();

    /**
     * returns the public, relative (so without domain) URL of this page, based on the relative path name of this page on the server.
     */
    URI getPublicRelativeAddress();

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
     * Create the log writer for this page to write to the "LOG" file in the meta dot folder.
     * @return
     * @throws IOException
     */
    LogWriter createLogWriter() throws IOException;

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
     * Returns the local location where the output of the Exporter should be stored.
     * @return
     */
    Path getRdfExportFile();

    /**
     * Returns the type in which the RDF export is saved
     * @return
     */
    Format getRdfExportFileFormat();

    /**
     * Reads the RDF model from disk to a memory graph.
     * Note: nothing is cached, this is just a convenience method.
     */
    Model readRdfModel() throws IOException;

    /**
     * Iterates the registered site languages and checks if we have translations stored of this page for each language
     */
    Map<Locale, Page> getTranslations() throws IOException;

}