package com.beligum.blocks.fs.pages;

import com.beligum.base.resources.ifaces.Resource;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.fs.HdfsResourcePath;
import com.beligum.blocks.fs.logger.RdfLogWriter;
import com.beligum.blocks.fs.logger.ifaces.LogWriter;
import com.beligum.blocks.fs.metadata.EBUCoreHdfsMetadataWriter;
import com.beligum.blocks.fs.metadata.ifaces.MetadataWriter;
import com.beligum.blocks.rdf.exporters.SesameExporter;
import com.beligum.blocks.rdf.ifaces.Exporter;
import com.beligum.blocks.rdf.ifaces.Format;
import com.beligum.blocks.rdf.ifaces.Importer;
import com.beligum.blocks.rdf.importers.SesameImporter;
import com.beligum.blocks.templating.blocks.HtmlAnalyzer;
import gen.com.beligum.blocks.core.maven;
import org.apache.hadoop.fs.Path;
import org.apache.tika.mime.MediaType;

import java.io.IOException;
import java.net.URI;

/**
 * This intermediate class handles all implementation-specific settings, except for the read-only/read-write details (see subclasses)
 *
 * Created by bram on 1/14/16.
 */
public abstract class DefaultPageImpl extends AbstractPage
{
    //-----CONSTANTS-----
    private MediaType PAGE_PROXY_NORMALIZED_MIME_TYPE = Resource.MimeType.HTML.getMimeType();
    //Note: makes sense to prefix with the mvn artifact, so we know what we wrote it with
    private String PAGE_PROXY_NORMALIZED_FILE_NAME = maven.Entries.maven_artifactId.getValue() + "_normalized." + Resource.MimeType.HTML.getExtension();

    // See this for why we write in N-Triples:
    // https://jena.apache.org/documentation/io/rdf-output.html#n-triples-and-n-quads
    // "These provide the formats that are fastest to write, and data of any size can be output."
    // "They maximise the interoperability with other systems and are useful for database dumps."
    private Format PAGE_PROXY_RDF_FORMAT = Format.NTRIPLES;
    private Resource.MimeType PAGE_PROXY_RDF_TYPE = PAGE_PROXY_RDF_FORMAT.getMimeType();
    // Note: makes sense to prefix with the mvn artifact, so we know what we wrote it with
    private String PAGE_PROXY_RDF_FILE_NAME = maven.Entries.maven_artifactId.getValue() + "_rdf." + PAGE_PROXY_RDF_TYPE.getExtension();

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    protected DefaultPageImpl(URI uri, URI fileSystemBaseUri) throws IOException
    {
        super(uri);

        //Note: the root-relative is to remove the leading slash
        URI resourceUri = fileSystemBaseUri.resolve(ROOT.relativize(this.relativeStoragePath));

        //here, we effectively attach a save-implementation to the disk location URI
        this.resourcePath = new HdfsResourcePath(StorageFactory.getPageStoreFileSystem(), resourceUri);
    }

    //-----PUBLIC METHODS-----
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
        return new HtmlAnalyzer(this.readOriginalHtml());
    }
    @Override
    public LogWriter createLogWriter() throws IOException
    {
        return new RdfLogWriter(this.resourcePath);
    }
    @Override
    public MetadataWriter createMetadataWriter() throws IOException
    {
        return new EBUCoreHdfsMetadataWriter(this.resourcePath.getFileContext());
    }
    @Override
    public Path getNormalizedPageProxyPath()
    {
        return new Path(this.resourcePath.getMetaProxyFolder(PAGE_PROXY_NORMALIZED_MIME_TYPE), PAGE_PROXY_NORMALIZED_FILE_NAME);
    }
    @Override
    public Path getRdfExportFile()
    {
        return new Path(this.resourcePath.getMetaProxyFolder(PAGE_PROXY_RDF_TYPE.getMimeType()), PAGE_PROXY_RDF_FILE_NAME);
    }
    @Override
    public Format getRdfExportFileFormat()
    {
        return PAGE_PROXY_RDF_FORMAT;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
