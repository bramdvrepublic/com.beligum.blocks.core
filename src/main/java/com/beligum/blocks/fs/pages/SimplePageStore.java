package com.beligum.blocks.fs.pages;

import com.beligum.base.auth.models.Person;
import com.beligum.base.server.R;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.fs.HdfsResourcePath;
import com.beligum.blocks.fs.HdfsUtils;
import com.beligum.blocks.fs.LockFile;
import com.beligum.blocks.fs.hdfs.HdfsZipUtils;
import com.beligum.blocks.fs.ifaces.Constants;
import com.beligum.blocks.fs.ifaces.ResourcePath;
import com.beligum.blocks.fs.metadata.ifaces.MetadataWriter;
import com.beligum.blocks.fs.pages.ifaces.Page;
import com.beligum.blocks.fs.pages.ifaces.PageStore;
import com.beligum.blocks.rdf.ifaces.Format;
import com.beligum.blocks.rdf.sources.HtmlSource;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.fs.permission.FsPermission;
import org.openrdf.model.Model;

import java.io.*;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.EnumSet;

/**
 * Created by bram on 1/14/16.
 */
public class SimplePageStore implements PageStore
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private Settings settings;

    //-----CONSTRUCTORS-----
    public SimplePageStore()
    {
        this.settings = Settings.instance();
    }

    //-----PUBLIC METHODS-----
    /**
     * This will walk the file system and delete all (stale) lock files so we always start with
     * a clean file system.
     *
     * @throws IOException
     */
    @Override
    public void init() throws IOException
    {
        Path pagesRoot = new Path(settings.getPagesStorePath());
        FileContext fs = StorageFactory.getPageStoreFileSystem();
        if (fs.util().exists(pagesRoot)) {
            HdfsUtils.recursiveDeleteLockFiles(fs, pagesRoot);
        }
    }
    @Override
    public Page save(HtmlSource source, Person creator) throws IOException
    {
        Page retVal = null;

        //Translates the address of the source page to a page (and a save location on disk),
        //extracting all necessary information from the URL and normalizing the address in the mean time
        Page newPage = new ReadWritePage(source.getSourceAddress());

        //cache some vars
        ResourcePath resourcePath = newPage.getResourcePath();
        FileContext fileContext = resourcePath.getFileContext();

        //will synchronize the metadata directory by creating/releasing a lock file
        try (LockFile lock = resourcePath.acquireLock()) {

            //prepare the HTML for saving; this is the only place we can modify the source
            // because later on, the analyzer will have run
            source.prepareForSaving(fileContext);

            //we'll read everything into a string for performance and ease of use
            String sourceHtml;
            try (InputStream is = source.openNewInputStream()) {
                sourceHtml = IOUtils.toString(is);
            }

            //pre-calculate the hash based on the incoming string and compare it with the stored version to abort early if nothing changed
            Path hashFile = resourcePath.getMetaHashFile();
            boolean nothingChanged = false;
            String newHash = null;
            try (InputStream is = new ByteArrayInputStream(sourceHtml.getBytes())) {
                newHash = HdfsResourcePath.calcHashChecksumFor(is);
            }
            if (fileContext.util().exists(hashFile)) {
                try (FSDataInputStream is = fileContext.open(hashFile)) {
                    String existingHash = IOUtils.toString(is);
                    nothingChanged = existingHash!=null && existingHash.equals(newHash);
                }
            }

            //for now, we'll return null if nothing changed
            if (nothingChanged) {
                retVal = null;
            }
            else {
                //make sure the path dirs exist
                //Note: this also works for dirs, since they're special files inside the actual dir
                fileContext.mkdir(resourcePath.getLocalPath().getParent(), FsPermission.getDirDefault(), true);

                //we're overwriting; make an entry in the history folder
                //TODO maybe we want to make this asynchronous?
                if (fileContext.util().exists(resourcePath.getLocalPath())) {
                    this.addHistoryEntry(fileContext, resourcePath);
                }

                //save the HASH of the file
                //TODO make this uniform with the watch code
                try (Writer writer = new BufferedWriter(new OutputStreamWriter(fileContext.create(hashFile, EnumSet.of(CreateFlag.CREATE, CreateFlag.OVERWRITE), Options.CreateOpts.createParent())))) {
                    writer.write(newHash);
                }

                //save the original page html
                try (Writer writer = new BufferedWriter(new OutputStreamWriter(fileContext.create(resourcePath.getLocalPath(), EnumSet.of(CreateFlag.CREATE, CreateFlag.OVERWRITE), Options.CreateOpts.createParent())))) {
                    writer.write(sourceHtml);
                }

                //save the normalized page html
                Path normalizedHtml = newPage.getNormalizedPageProxyPath();
                try (Writer writer = new BufferedWriter(new OutputStreamWriter(fileContext.create(normalizedHtml, EnumSet.of(CreateFlag.CREATE, CreateFlag.OVERWRITE), Options.CreateOpts.createParent())))) {
                    writer.write(source.getNormalizedHtml());
                }

                //parse and generate the RDF model
                Model rdfModel = newPage.createImporter(Format.RDFA).importDocument(source);
                //export the RDF model to the storage file (note that this file will be re-read to save to the triple store)
                try (OutputStream os = fileContext.create(newPage.getRdfExportFile(), EnumSet.of(CreateFlag.CREATE, CreateFlag.OVERWRITE), Options.CreateOpts.createParent())) {
                    newPage.createExporter(newPage.getRdfExportFileFormat()).exportModel(rdfModel, os);
                }

                //save the page metadata (read it in if it exists)
                this.writeMetadata(fileContext, resourcePath, creator, newPage.createMetadataWriter());

                retVal = newPage;
            }
        }

        return retVal;
    }
    @Override
    public Page delete(URI publicAddress, Person deleter) throws IOException
    {
        Page retVal = null;

        Page page = new ReadWritePage(publicAddress);

        //cache some vars
        ResourcePath resourcePath = page.getResourcePath();
        FileContext fileContext = resourcePath.getFileContext();

        try (LockFile lock = resourcePath.acquireLock()) {

            //make sure the path dirs exist
            //Note: this also works for dirs, since they're special files inside the actual dir
            fileContext.mkdir(resourcePath.getLocalPath().getParent(), FsPermission.getDirDefault(), true);

            //save the page metadata BEFORE we create the history entry (to make sure we save who deleted it)
            this.writeMetadata(fileContext, resourcePath, deleter, page.createMetadataWriter());

            //we're overwriting; make an entry in the history folder
            //TODO maybe we want to make this asynchronous?
            if (fileContext.util().exists(resourcePath.getLocalPath())) {
                this.addHistoryEntry(fileContext, resourcePath);
            }

            //delete the original page html and leave the rest alone
            fileContext.delete(resourcePath.getLocalPath(), false);

            //list everything under meta folder and delete it, except for the HISTORY folder
            RemoteIterator<FileStatus> metaEntries = fileContext.listStatus(resourcePath.getMetaFolder());
            while (metaEntries.hasNext()) {
                FileStatus child = metaEntries.next();
                if (!child.getPath().equals(resourcePath.getMetaHistoryFolder())) {
                    fileContext.delete(child.getPath(), true);
                }
            }

            retVal = page;
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    /**
     * Very first simple try at creating a snapshot of the current pathInfo on disk, backed by the HDFS atomic rules.
     * Both the file and the meta directory are copied to a .snapshot sibling.
     * The wrapper around the resulting snapshot is returned.
     *
     * NOTE: this is not optimized in the light of:
     * - the new transactional-ness of the filesystem
     * - the fact we're gzipping the entire folder at the end
     *
     * @param fs
     * @param resourcePath
     * @return the successfully created history entry or null of something went wrong
     * @throws IOException
     */
    private ResourcePath addHistoryEntry(FileContext fs, ResourcePath resourcePath) throws IOException
    {
        // Note: it makes sense to use the now timestamp because we're about to create a snapshot of the situation _now_
        // we can't really rely on other timestamps because which one should we take?
        ZonedDateTime stamp = ZonedDateTime.now();

        // first, we'll create a snapshot of the meta folder to a sibling folder. We can't copy it to it's final destination
        // because that is a subfolder of the folder we're copying and we'll encounter odd recursion.
        Path snapshotMetaFolder = new Path(resourcePath.getMetaFolder().getParent(), resourcePath.getMetaFolder().getName() + Constants.TEMP_SNAPSHOT_SUFFIX);

        //this is the new history entry folder
        Path newHistoryEntryFolder = new Path(resourcePath.getMetaHistoryFolder(), Constants.FOLDER_TIMESTAMP_FORMAT.format(stamp));

        //the history entry destination (eg. the original file in the history entry folder)
        ResourcePath historyEntry = new HdfsResourcePath(fs, new Path(newHistoryEntryFolder, resourcePath.getLocalPath().getName()));

        boolean success = false;

        //copy the meta folder to the temp history entry folder
        if (!fs.util().copy(resourcePath.getMetaFolder(), snapshotMetaFolder)) {
            throw new IOException("Error while adding a history entry for " + resourcePath + ": couldn't create a snapshot of the meta folder; " + snapshotMetaFolder);
        }

        //we're not copying the history folder into the snapshot folder; that would be recursion, so delete it
        Path snapshotHistoryFolder = new Path(snapshotMetaFolder, Constants.META_SUBFOLDER_HISTORY);
        if (fs.util().exists(snapshotHistoryFolder)) {
            if (!fs.delete(snapshotHistoryFolder, true)) {
                throw new IOException("Error while adding a history entry for " + resourcePath + ": couldn't delete the history folder of the temp meta snapshot folder; " + snapshotMetaFolder);
            }
        }

        if (fs.util().exists(newHistoryEntryFolder)) {
            throw new IOException("Error while adding a history entry for " + resourcePath + ": history folder already existed; " + newHistoryEntryFolder);
        }
        else {
            fs.mkdir(newHistoryEntryFolder, FsPermission.getDirDefault(), true);
        }

        //if we get here without problems, we start copying the original file to it's history destination
        if (!fs.util().copy(resourcePath.getLocalPath(), historyEntry.getLocalPath())) {
            throw new IOException("Error while adding a history entry for " + resourcePath + ": couldn't copy the original file to it's final destination " + historyEntry.getLocalPath());
        }

        //move the snapshot in it's place
        fs.rename(snapshotMetaFolder, historyEntry.getMetaFolder());

        //zip it
        HdfsZipUtils.gzipTarFolder(fs, newHistoryEntryFolder, new Path(newHistoryEntryFolder.toUri().toString() + ".tgz"));

        //and remove the original
        fs.delete(newHistoryEntryFolder, true);

        success = true;

        //Note: no need to cleanup, the XA filesystem will do that for us

        return success ? historyEntry : null;
    }
    private void writeMetadata(FileContext fs, ResourcePath resourcePath, Person creator, MetadataWriter metadataWriter) throws IOException
    {
        metadataWriter.open(resourcePath);
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
