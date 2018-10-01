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

import com.beligum.base.models.Person;
import com.beligum.base.resources.ifaces.MimeType;
import com.beligum.base.resources.ifaces.ResourceRepository;
import com.beligum.base.resources.ifaces.Source;
import com.beligum.base.server.R;
import com.beligum.blocks.config.RdfFactory;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.endpoints.ifaces.RdfQueryEndpoint;
import com.beligum.blocks.filesystem.hdfs.HdfsZipUtils;
import com.beligum.blocks.filesystem.ifaces.BlocksResource;
import com.beligum.blocks.filesystem.ifaces.ResourceMetadata;
import com.beligum.blocks.filesystem.logger.PageLogEntry;
import com.beligum.blocks.filesystem.logger.ifaces.LogWriter;
import com.beligum.blocks.filesystem.metadata.ifaces.MetadataWriter;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.rdf.ifaces.Format;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ontology.factories.Terms;
import com.beligum.blocks.rdf.sources.PageSource;
import com.beligum.blocks.utils.RdfTools;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.fs.permission.FsPermission;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import java.io.*;
import java.net.URI;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Locale;

/**
 * Created by bram on 5/2/16.
 */
public class ReadWritePage extends DefaultPage
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    protected ReadWritePage(ResourceRepository repository, Source source) throws IOException
    {
        super(repository, source.getUri(), source.getLanguage(), source.getMimeType(), false, StorageFactory.getPageStoreFileSystem());
    }
    protected ReadWritePage(ResourceRepository repository, URI uri, Locale language, MimeType mimeType, boolean allowEternalCaching, FileContext fileContext) throws IOException
    {
        super(repository, uri, language, mimeType, allowEternalCaching, fileContext);
    }

    //-----PUBLIC METHODS-----
    @Override
    public boolean isReadOnly()
    {
        return false;
    }
    public void createParent() throws IOException
    {
        //make sure the path dirs exist
        //Note: this also works for dirs, since they're special files inside the actual dir
        this.getFileContext().mkdir(this.getLocalStoragePath().getParent(), FsPermission.getDirDefault(), true);
    }
    public void write(PageSource source) throws IOException
    {
        //save the original page html
        try (InputStream is = source.newInputStream();
             OutputStream os = this.getFileContext().create(this.getLocalStoragePath(),
                                                            EnumSet.of(CreateFlag.CREATE, CreateFlag.OVERWRITE),
                                                            Options.CreateOpts.createParent())) {
            IOUtils.copy(is, os);
        }
    }
    public void updateNormalizedProxy(PageSource source) throws IOException
    {
        //save the normalized page html
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(this.getFileContext().create(this.getNormalizedHtmlFile(),
                                                                                                    EnumSet.of(CreateFlag.CREATE, CreateFlag.OVERWRITE),
                                                                                                    Options.CreateOpts.createParent())))) {
            writer.write(source.getNormalizedHtml());
        }
    }
    public void updateRdfProxy(PageSource source) throws IOException
    {
        //parse, generate and save the RDF model from the RDFa in the HTML page
        Model rdfModel;
        try (InputStream is = source.newInputStream()) {
            rdfModel = this.createImporter(Format.RDFA).importDocument(this.getPublicAbsoluteAddress(), is);
            try (OutputStream os = this.getFileContext().create(this.getRdfExportFile(), EnumSet.of(CreateFlag.CREATE, CreateFlag.OVERWRITE), Options.CreateOpts.createParent())) {
                this.createExporter(this.getRdfExportFileFormat()).exportModel(rdfModel, os);
            }
        }

        //this model will hold all copies of dependency triples on external ontologies
        Model rdfDepsModel = new LinkedHashModel();
        ValueFactory valueFactory = SimpleValueFactory.getInstance();
        for (Statement stmt : rdfModel) {
            Value value = stmt.getObject();
            //detect if the value is an external resource
            if (value instanceof IRI) {
                URI predicateCurie = RdfTools.fullToCurie(URI.create(stmt.getPredicate().toString()));
                //this means the predicate is locally known
                if (predicateCurie != null) {
                    RdfProperty predicate = (RdfProperty) RdfFactory.getClassForResourceType(predicateCurie);
                    //should always be ok because of the check above
                    if (predicate != null) {
                        RdfClass dataType = predicate.getDataType();
                        RdfQueryEndpoint endpoint = dataType.getEndpoint();
                        //this means the predicate's datatype has an external endpoint, so it's a valid external resource
                        if (endpoint != null && endpoint.isExternal()) {
                            URI localResourceId = URI.create(value.stringValue());
                            Model externalRdfModel = endpoint.getExternalRdfModel(dataType, localResourceId, source.getLanguage());
                            //if we have a valid model, add it to the dependency list, together with a "equals" statement
                            if (!externalRdfModel.isEmpty()) {
                                URI externalResourceId = endpoint.getExternalResourceId(localResourceId, source.getLanguage());
                                //this is more or less the glue between the external ontology and the local one
                                rdfDepsModel.add(valueFactory.createIRI(localResourceId.toString()),
                                                 //this approach is discussable (instead of using the OWL ontology directly),
                                                 // but the general approach is to use the local ontology as much as possible,
                                                 // so let's be consequent in our decisions...
                                                 valueFactory.createIRI(Terms.sameAs.getFullName().toString()),
                                                 valueFactory.createIRI(externalResourceId.toString()));

                                rdfDepsModel.addAll(externalRdfModel);
                            }
                        }
                    }
                }
            }
        }

        //Note: it makes sense to always sync the deps file with the state of the model,
        // so we'll erase everything and re-create the file.
        //Note: if the deps model is empty, we still create an empty file to indicate this,
        // so we can rely on it's presence to detect the need to redo it duing reindexing
        Path rdfDepsFile = this.getRdfDependenciesExportFile();
        try (OutputStream os = this.getFileContext().create(rdfDepsFile, EnumSet.of(CreateFlag.CREATE, CreateFlag.OVERWRITE), Options.CreateOpts.createParent())) {
            this.createExporter(this.getRdfExportFileFormat()).exportModel(rdfDepsModel, os);
        }
    }
    public void delete() throws IOException
    {
        //delete the original page html and leave the rest (the dot folder) alone
        this.getFileContext().delete(this.getLocalStoragePath(), false);

        //list everything under the dot folder and delete it, except for the HISTORY folder
        RemoteIterator<FileStatus> metaEntries = this.getFileContext().listStatus(this.getDotFolder());
        while (metaEntries.hasNext()) {
            FileStatus child = metaEntries.next();
            if (!child.getPath().equals(this.getHistoryFolder())) {
                this.getFileContext().delete(child.getPath(), true);
            }
        }
    }
    /**
     * Very first simple try at creating a snapshot of the current pathInfo on disk, backed by the HDFS atomic rules.
     * Both the file and the meta directory are copied to a .snapshot sibling.
     * The wrapper around the resulting snapshot is returned.
     * <p>
     * NOTE: this is not optimized in the light of:
     * - the new transactional-ness of the filesystem
     * - the fact we're gzipping the entire folder at the end
     *
     * @return the successfully created history entry or null of something went wrong
     * @throws IOException
     */
    public Path writeHistoryEntry(Page oldPage) throws IOException
    {
        //this is the new history entry folder
        Path newHistoryEntryFolder = new Path(this.getHistoryFolder(), BlocksResource.FOLDER_TIMESTAMP_FORMAT.format(Instant.ofEpochMilli(this.creationStamp)));

        //copy the meta folder to the temp history entry folder:
        // first, we'll instance a snapshot of the meta folder to a sibling folder. We can't copy it directly to it's final destination
        // because that is a subfolder of the folder we're copying and we'll encounter odd recursion.
        Path snapshotMetaFolder = new Path(this.getDotFolder().getParent(), this.getDotFolder().getName() + BlocksResource.TEMP_SNAPSHOT_SUFFIX);
        if (!this.getFileContext().util().copy(oldPage.getDotFolder(), snapshotMetaFolder)) {
            throw new IOException("Error while adding a history entry to " + oldPage + ": couldn't instance a snapshot of the meta folder; " + snapshotMetaFolder);
        }

        //we're not copying the history folder into the snapshot folder; that would be recursion, so delete it
        Path snapshotHistoryFolder = new Path(snapshotMetaFolder, BlocksResource.META_SUBFOLDER_HISTORY);
        if (this.getFileContext().util().exists(snapshotHistoryFolder)) {
            if (!this.getFileContext().delete(snapshotHistoryFolder, true)) {
                throw new IOException("Error while adding a history entry to " + this + ": couldn't delete the history folder of the temp meta snapshot folder; " + snapshotMetaFolder);
            }
        }

        if (this.getFileContext().util().exists(newHistoryEntryFolder)) {
            throw new IOException("Error while adding a history entry to " + this + ": history folder already existed (this should be impossible because of the locking mechanism); " +
                                  newHistoryEntryFolder);
        }
        else {
            this.getFileContext().mkdir(newHistoryEntryFolder, FsPermission.getDirDefault(), true);
        }

        //if we get here without problems, we start copying the original file to it's history destination
        Path newHistoryEntryOriginal = new Path(newHistoryEntryFolder, oldPage.getLocalStoragePath().getName());
        if (!this.getFileContext().util().copy(oldPage.getLocalStoragePath(), newHistoryEntryOriginal)) {
            throw new IOException("Error while adding a history entry to " + this + ": couldn't copy the original file to it's final destination " + newHistoryEntryOriginal);
        }

        //move the snapshot in it's place
        this.getFileContext().rename(snapshotMetaFolder, new Path(newHistoryEntryFolder, oldPage.getDotFolder().getName()));

        //zip it
        Path newHistoryEntryZipFile = new Path(newHistoryEntryFolder.toUri().toString() + ".tgz");
        HdfsZipUtils.gzipTarFolder(this.getFileContext(), newHistoryEntryFolder, newHistoryEntryZipFile);

        //and remove the original
        this.getFileContext().delete(newHistoryEntryFolder, true);

        //Note: no need to cleanup, the XA filesystem will do that for us

        return newHistoryEntryZipFile;
    }
    public void writeLogEntry(Person creator, PageLogEntry.Action action) throws IOException
    {
        try (LogWriter logWriter = this.createLogWriter()) {
            logWriter.writeLogEntry(new PageLogEntry(Instant.ofEpochMilli(this.creationStamp), creator, this, action));
        }
    }
    public void writeMetadata(Person creator) throws IOException
    {
        try (MetadataWriter metadataWriter = this.createMetadataWriter()) {
            metadataWriter.open(this);
            //update or fill the ebucore structure with all possible metadata
            metadataWriter.updateSchemaData();
            //update the version of this software that writes the metadata
            metadataWriter.updateSoftwareData(R.configuration().getCurrentProjectProperties());
            metadataWriter.updateFileData();
            metadataWriter.updateCreator(creator);
            metadataWriter.updateTimestamps();
            metadataWriter.write();
            metadataWriter.close();
        }
    }
    @Override
    public ReadWritePage getReadWriteVariant()
    {
        return this;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
