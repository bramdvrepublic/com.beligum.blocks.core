package com.beligum.blocks.fs.pages.ifaces;

import com.beligum.blocks.fs.ifaces.ResourcePath;
import com.beligum.blocks.fs.logger.ifaces.LogWriter;
import com.beligum.blocks.fs.metadata.ifaces.MetadataWriter;
import com.beligum.blocks.rdf.ifaces.Exporter;
import com.beligum.blocks.rdf.ifaces.Format;
import com.beligum.blocks.rdf.ifaces.Importer;
import com.beligum.blocks.rdf.sources.HtmlSource;
import com.beligum.blocks.templating.blocks.HtmlAnalyzer;
import org.apache.hadoop.fs.Path;
import org.openrdf.model.Model;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;

/**
 * Created by bram on 1/14/16.
 */
public interface Page
{
    /**
     * Returns the most naked form of this page; eg. stripped from all language params, it's file storage extension (or directory dummy filename, etc).
     * This address is to be used to index this page (together with it's language, but separately).
     * This allows us to easily perform redirects eg. when a requested page doesn't exist for the requested path in the requested language, but does with another language.
     * For example, if the page /en/this/page exists, but the user requests /nl/this/page, we can easily lookup the second page using it's canonical address /this/page
     */
    URI getCanonicalAddress();

    /**
     * Returns the language of this page, as extracted (and stripped) during creation. May be null, if no such value was present during creation (eg. /this/page)
     * or the used language was unsupported with the current site language settings (eg. /xx/this/page)
     */
    Locale getLanguage();

    /**
     * returns the public, full (domain-prefixed) URL of this page, based on the relative path name of this page on the server.
     */
    URI getPublicAbsoluteAddress() throws IOException;

    /**
     * returns the public, relative (so without domain) URL of this page, based on the relative path name of this page on the server.
     */
    URI getPublicRelativeAddress() throws IOException;

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
     */
    HtmlSource readOriginalHtml() throws IOException;

    /**
     * Creates a new html (stream) source based on the rendered normalized html on disk.
     */
    HtmlSource readNormalizedHtml() throws IOException;

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
     * Returns the path (and all other info) to the local resource file on disk.
     * @return
     */
    ResourcePath getResourcePath();

    /**
     * Reads the RDF model from disk to a memory graph.
     * Note: nothing is cached, this is just a convenience method.
     */
    Model readRdfModel() throws IOException;
}
