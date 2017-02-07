package com.beligum.blocks.filesystem.pages;

import com.beligum.base.models.Person;
import com.beligum.base.resources.ifaces.ResourceRepository;
import com.beligum.base.resources.ifaces.Source;
import com.beligum.base.server.R;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.filesystem.hdfs.HdfsZipUtils;
import com.beligum.blocks.filesystem.ifaces.BlocksResource;
import com.beligum.blocks.filesystem.logger.PageLogEntry;
import com.beligum.blocks.filesystem.logger.ifaces.LogWriter;
import com.beligum.blocks.filesystem.metadata.ifaces.MetadataWriter;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.rdf.ifaces.Format;
import com.beligum.blocks.rdf.sources.PageSource;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.fs.permission.FsPermission;
import org.openrdf.model.Model;

import java.io.*;
import java.time.Instant;
import java.util.EnumSet;

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
        super(repository, source.getUri(), source.getLanguage(), source.getMimeType(), false, Settings.instance().getPagesStorePath(), StorageFactory.getPageStoreFileSystem());
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

        //save the normalized page html
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(this.getFileContext().create(this.getNormalizedPageProxyPath(),
                                                                                                    EnumSet.of(CreateFlag.CREATE, CreateFlag.OVERWRITE),
                                                                                                    Options.CreateOpts.createParent())))) {
            writer.write(source.getNormalizedHtml());
        }

        //parse, generate and save the RDF model from the RDFa in the HTML page
        Model rdfModel = this.createImporter(Format.RDFA).importDocument(source);
        try (OutputStream os = this.getFileContext().create(this.getRdfExportFile(), EnumSet.of(CreateFlag.CREATE, CreateFlag.OVERWRITE), Options.CreateOpts.createParent())) {
            this.createExporter(this.getRdfExportFileFormat()).exportModel(rdfModel, os);
        }

        //if all went well, we can update the hash file
        this.writeHash(source.getHash());
    }
    public void delete() throws IOException
    {
        //delete the original page html and leave the rest alone
        this.getFileContext().delete(this.getLocalStoragePath(), false);

        //list everything under meta folder and delete it, except for the HISTORY folder
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
        // first, we'll create a snapshot of the meta folder to a sibling folder. We can't copy it directly to it's final destination
        // because that is a subfolder of the folder we're copying and we'll encounter odd recursion.
        Path snapshotMetaFolder = new Path(this.getDotFolder().getParent(), this.getDotFolder().getName() + BlocksResource.TEMP_SNAPSHOT_SUFFIX);
        if (!this.getFileContext().util().copy(oldPage.getDotFolder(), snapshotMetaFolder)) {
            throw new IOException("Error while adding a history entry to " + oldPage + ": couldn't create a snapshot of the meta folder; " + snapshotMetaFolder);
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
        this.getFileContext().rename(snapshotMetaFolder, new Path(newHistoryEntryFolder, oldPage.getMetadataFolder().getName()));

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

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
