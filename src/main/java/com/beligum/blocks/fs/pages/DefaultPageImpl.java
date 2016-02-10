package com.beligum.blocks.fs.pages;

import com.beligum.base.templating.ifaces.Resource;
import com.beligum.blocks.fs.ifaces.PathInfo;
import com.beligum.blocks.fs.metadata.EBUCoreHdfsMetadataWriter;
import com.beligum.blocks.fs.metadata.ifaces.MetadataWriter;
import com.beligum.blocks.rdf.exporters.JenaExporter;
import com.beligum.blocks.rdf.ifaces.Exporter;
import com.beligum.blocks.rdf.ifaces.Importer;
import com.beligum.blocks.rdf.importers.SesameImporter;
import gen.com.beligum.blocks.core.maven;
import org.apache.hadoop.fs.Path;
import org.apache.tika.mime.MediaType;

import java.io.IOException;

/**
 * Created by bram on 1/14/16.
 */
public class DefaultPageImpl extends AbstractPage
{
    //-----CONSTANTS-----
    MediaType PAGE_PROXY_NORMALIZED_MIME_TYPE = Resource.MimeType.HTML.getMimeType();
    //Note: makes sense to prefix with the mvn artifact, so we know what we wrote it with
    String PAGE_PROXY_NORMALIZED_FILE_NAME = maven.Entries.maven_artifactId.getValue() + "_normalized." + Resource.MimeType.HTML.getExtension();

    MediaType PAGE_PROXY_RDF_JSONLD_MIME_TYPE = Resource.MimeType.JSONLD.getMimeType();
    // Note: makes sense to prefix with the mvn artifact, so we know what we wrote it with
    // also note that the reason it's suffixed 'rdf' is that JSONLD is a Resource Description Framework serialization format:
    // https://en.wikipedia.org/wiki/Linked_data
    String PAGE_PROXY_RDF_JSONLD_FILE_NAME = maven.Entries.maven_artifactId.getValue() + "_rdf." + Resource.MimeType.JSONLD.getExtension();

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public DefaultPageImpl(PathInfo pathInfo)
    {
        super(pathInfo);
    }

    //-----PUBLIC METHODS-----
    @Override
    public Importer createImporter() throws IOException
    {
        return new SesameImporter(Importer.Format.RDFA);
    }
    @Override
    public Exporter createExporter() throws IOException
    {
        return new JenaExporter(Exporter.Format.JSONLD);
    }
    @Override
    public MetadataWriter createMetadataWriter() throws IOException
    {
        return new EBUCoreHdfsMetadataWriter(this.pathInfo.getFileContext());
    }
    //    @Override
    public Path getNormalizedPageProxyPath()
    {
        return new Path(this.pathInfo.getMetaProxyFolder(PAGE_PROXY_NORMALIZED_MIME_TYPE), PAGE_PROXY_NORMALIZED_FILE_NAME);
    }
    @Override
    public Path getExportFile()
    {
        return new Path(this.pathInfo.getMetaProxyFolder(PAGE_PROXY_RDF_JSONLD_MIME_TYPE), PAGE_PROXY_RDF_JSONLD_FILE_NAME);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
