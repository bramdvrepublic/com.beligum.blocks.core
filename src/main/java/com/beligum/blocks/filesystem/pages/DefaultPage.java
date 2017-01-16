package com.beligum.blocks.filesystem.pages;

import com.beligum.base.resources.MimeTypes;
import com.beligum.base.resources.ifaces.MimeType;
import com.beligum.base.resources.ifaces.ResourceRepository;
import com.beligum.base.resources.ifaces.ResourceRequest;
import com.beligum.blocks.filesystem.logger.RdfLogWriter;
import com.beligum.blocks.filesystem.logger.ifaces.LogWriter;
import com.beligum.blocks.filesystem.metadata.EBUCoreHdfsMetadataWriter;
import com.beligum.blocks.filesystem.metadata.ifaces.MetadataWriter;
import com.beligum.blocks.rdf.exporters.SesameExporter;
import com.beligum.blocks.rdf.ifaces.Exporter;
import com.beligum.blocks.rdf.ifaces.Format;
import com.beligum.blocks.rdf.ifaces.Importer;
import com.beligum.blocks.rdf.importers.SesameImporter;
import com.beligum.blocks.templating.blocks.HtmlAnalyzer;
import gen.com.beligum.blocks.core.maven;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;

/**
 * This intermediate class handles all implementation-specific settings (mainly the serialization standard settings),
 * except for the read-only/read-write details (see subclasses)
 *
 * Created by bram on 1/14/16.
 */
public abstract class DefaultPage extends AbstractPage
{
    //-----CONSTANTS-----
    private static final MimeType PAGE_PROXY_NORMALIZED_MIME_TYPE = MimeTypes.HTML;
    //Note: makes sense to prefix with the mvn artifact, so we know what we wrote it with
    private static final String PAGE_PROXY_NORMALIZED_FILE_NAME = maven.Entries.maven_artifactId.getValue() + "_normalized.html";

    // See this for why we write in N-Triples:
    // https://jena.apache.org/documentation/io/rdf-output.html#n-triples-and-n-quads
    // "These provide the formats that are fastest to write, and data of any size can be output."
    // "They maximise the interoperability with other systems and are useful for database dumps."
    private static final Format PAGE_PROXY_RDF_FORMAT = Format.NTRIPLES;
    private static final MimeType PAGE_PROXY_RDF_TYPE = PAGE_PROXY_RDF_FORMAT.getMimeType();
    // Note: makes sense to prefix with the mvn artifact, so we know what we wrote it with
    private static final String PAGE_PROXY_RDF_FILE_NAME = maven.Entries.maven_artifactId.getValue() + "_rdf.nt";

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    protected DefaultPage(ResourceRequest request, URI fileSystemBaseUri, FileContext fileContext) throws IOException
    {
        super(request, fileContext);

        //this.init(fileSystemBaseUri, fileContext);
    }
    protected DefaultPage(ResourceRepository repository, URI uri, Locale language, MimeType mimeType, boolean allowEternalCaching, URI fileSystemBaseUri, FileContext fileContext) throws IOException
    {
        super(repository, uri, language, mimeType, allowEternalCaching, fileContext);

        //this.init(fileSystemBaseUri, fileContext);
    }
//    protected DefaultPageImpl(Path relativeLocalFile, URI fileSystemBaseUri, FileContext fileContext) throws IOException
//    {
//        super(relativeLocalFile);
//
//        this.init(fileSystemBaseUri, fileContext);
//    }
//    protected void init(URI fileSystemBaseUri, FileContext fileContext) throws IOException
//    {
//        //Note: the root-relative is to remove the leading slash
//        URI resourceUri = fileSystemBaseUri.resolve(ROOT.relativize(this.getLocalStoragePath()));
//
//        //here, we effectively attach a save-implementation to the disk location URI
//        this.resourcePath = new HdfsResourcePath(fileContext, resourceUri);
//    }

    //-----PUBLIC METHODS-----
    @Override
    public boolean isImmutable()
    {
        //pages are not immutable, it's the whole point of our blocks system
        return false;
    }
    @Override
    public Importer createImporter(Format importFormat) throws IOException
    {
        return new SesameImporter(importFormat);
    }
    @Override
    public Exporter createExporter(Format exportFormat) throws IOException
    {
        return new SesameExporter(exportFormat);
    }
    @Override
    public HtmlAnalyzer createAnalyzer() throws IOException
    {
        return new HtmlAnalyzer(this);
    }
    @Override
    public LogWriter createLogWriter() throws IOException
    {
        return new RdfLogWriter(this);
    }
    @Override
    public MetadataWriter createMetadataWriter() throws IOException
    {
        return new EBUCoreHdfsMetadataWriter(this.getFileContext());
    }
    @Override
    public Path getNormalizedPageProxyPath()
    {
        return new Path(this.getProxyFolder(PAGE_PROXY_NORMALIZED_MIME_TYPE), PAGE_PROXY_NORMALIZED_FILE_NAME);
    }
    @Override
    public Path getRdfExportFile()
    {
        return new Path(this.getProxyFolder(PAGE_PROXY_RDF_TYPE), PAGE_PROXY_RDF_FILE_NAME);
    }
    @Override
    public Format getRdfExportFileFormat()
    {
        return PAGE_PROXY_RDF_FORMAT;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
