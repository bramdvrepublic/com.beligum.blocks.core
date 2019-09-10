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

package com.beligum.blocks.filesystem.pages.ifaces;

import com.beligum.blocks.filesystem.ifaces.BlocksResource;
import com.beligum.blocks.filesystem.logging.ifaces.LogWriter;
import com.beligum.blocks.filesystem.metadata.ifaces.MetadataWriter;
import com.beligum.blocks.filesystem.pages.ReadWritePage;
import com.beligum.blocks.rdf.ifaces.Exporter;
import com.beligum.blocks.rdf.ifaces.Format;
import com.beligum.blocks.rdf.ifaces.Importer;
import com.beligum.blocks.templating.analyzer.HtmlAnalyzer;
import org.apache.hadoop.fs.Path;
import org.eclipse.rdf4j.model.Model;

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
     * This philosophy also dictates the domain is stripped from the address and thus the returned URI is relative.
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
     * returns the absolute (domain-prefixed) URL of the resource (the RDF subject) this page describes.
     * Note: for now, this is a convenience method around the page analyzer and that analyzer is loaded every time again, so beware of performance issues!
     */
    URI getAbsoluteResourceAddress() throws IOException;

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
     * Creates a new analyzer - based on the information coming from newInputStream() - reads it in and parses it.
     * Note: this is a very expensive operation, use with care.
     * @return the initialized and parsed analyzer to be used to extract meta info from the stored html.
     * @throws IOException
     */
    HtmlAnalyzer createAnalyzer() throws IOException;

    /**
     * Same as above, but forces to read the original, unmodified file instead of the default newInputStream()
     * (eg. which reads the normalized stream for ReadOnlyPages instead of the full original html)
     */
    HtmlAnalyzer createAnalyzer(boolean readOriginal) throws IOException;

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
    Path getNormalizedHtmlFile();

    /**
     * Returns the local location where the output of the Exporter should be stored.
     * @return
     */
    Path getRdfExportFile();

    /**
     * Returns the local location where the output of the dependencies on external/foreign ontologies should be stored.
     * @return
     */
    Path getRdfDependenciesExportFile();

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
     * Reads the RDF dependencies model from disk to a memory graph.
     * Note: nothing is cached, this is just a convenience method.
     */
    Model readRdfDependenciesModel() throws IOException;

    /**
     * Iterates the registered site languages and checks if we have translations stored of this page for each language
     */
    Map<Locale, Page> getTranslations() throws IOException;

    /**
     * Same method as above, but allows us to re-use an existing analyzer
     */
    Map<Locale, Page> getTranslations(HtmlAnalyzer htmlAnalyzer) throws IOException;

    /**
     * Returns all sub-resource URIs of this page
     */
    Iterable<URI> getSubResources() throws IOException;

    /**
     * Same method as above, but allows us to re-use an existing analyzer
     */
    Iterable<URI> getSubResources(HtmlAnalyzer htmlAnalyzer) throws IOException;

    /**
     * Creates a read/write variant of this page if needed (just returns itself if it's a read/write page)
     */
    ReadWritePage getReadWriteVariant() throws IOException;
}
