/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.beligum.blocks.filesystem.pages;

import com.beligum.base.resources.MimeTypes;
import com.beligum.base.resources.ifaces.MimeType;
import com.beligum.base.resources.ifaces.ResourceRepository;
import com.beligum.base.resources.ifaces.ResourceRequest;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.filesystem.ifaces.ResourceMetadata;
import com.beligum.blocks.filesystem.logging.RdfLogWriter;
import com.beligum.blocks.filesystem.logging.ifaces.LogWriter;
import com.beligum.blocks.filesystem.metadata.EBUCoreHdfsMetadataWriter;
import com.beligum.blocks.filesystem.metadata.ifaces.MetadataWriter;
import com.beligum.blocks.filesystem.pages.ifaces.PageMetadata;
import com.beligum.blocks.rdf.exporters.SesameExporter;
import com.beligum.blocks.rdf.ifaces.Exporter;
import com.beligum.blocks.rdf.ifaces.Format;
import com.beligum.blocks.rdf.ifaces.Importer;
import com.beligum.blocks.rdf.importers.SesameImporter;
import com.beligum.blocks.templating.blocks.analyzer.HtmlAnalyzer;
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
    private static final String PAGE_PROXY_RDF_DEPS_FILE_NAME = maven.Entries.maven_artifactId.getValue() + "_rdf-deps.nt";

    //-----VARIABLES-----
    private PageMetadata cachedMetadata;
    private long cachedMetadataStamp;

    //-----CONSTRUCTORS-----
    protected DefaultPage(ResourceRequest request, FileContext fileContext) throws IOException
    {
        super(request, fileContext);
    }
    protected DefaultPage(ResourceRepository repository, URI uri, Locale language, MimeType mimeType, boolean allowEternalCaching, FileContext fileContext) throws IOException
    {
        super(repository, uri, language, mimeType, allowEternalCaching, fileContext);
    }

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
    public HtmlAnalyzer createAnalyzer(boolean readOriginal) throws IOException
    {
        return new HtmlAnalyzer(this, readOriginal);
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
    public ResourceMetadata getMetadata() throws IOException
    {
        if (Settings.instance().getEnablePageMetadataCaching()) {
            long stamp = this.getLastModifiedTime();
            if (this.cachedMetadata == null || stamp > this.cachedMetadataStamp) {
                this.cachedMetadata = new DefaultPageMetadata(this);
                this.cachedMetadataStamp = stamp;
            }
            return this.cachedMetadata;
        }
        else {
            return new DefaultPageMetadata(this);
        }
    }
    @Override
    public Path getNormalizedHtmlFile()
    {
        return new Path(this.getProxyFolder(PAGE_PROXY_NORMALIZED_MIME_TYPE), PAGE_PROXY_NORMALIZED_FILE_NAME);
    }
    @Override
    public Path getRdfExportFile()
    {
        return new Path(this.getProxyFolder(PAGE_PROXY_RDF_TYPE), PAGE_PROXY_RDF_FILE_NAME);
    }
    @Override
    public Path getRdfDependenciesExportFile()
    {
        return new Path(this.getProxyFolder(PAGE_PROXY_RDF_TYPE), PAGE_PROXY_RDF_DEPS_FILE_NAME);
    }
    @Override
    public Format getRdfExportFileFormat()
    {
        return PAGE_PROXY_RDF_FORMAT;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
