package com.beligum.blocks.fs.pages;

import com.beligum.base.auth.models.Person;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.base.utils.json.Json;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.fs.HdfsPathInfo;
import com.beligum.blocks.fs.HdfsUtils;
import com.beligum.blocks.fs.LockFile;
import com.beligum.blocks.fs.ifaces.Constants;
import com.beligum.blocks.fs.ifaces.PathInfo;
import com.beligum.blocks.fs.metadata.ifaces.MetadataWriter;
import com.beligum.blocks.fs.pages.ifaces.Page;
import com.beligum.blocks.fs.pages.ifaces.PageStore;
import com.beligum.blocks.rdf.ifaces.Source;
import com.fasterxml.jackson.databind.JsonNode;
import com.hp.hpl.jena.rdf.model.Model;
import org.apache.hadoop.fs.CreateFlag;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.joda.time.DateTime;

import java.io.*;
import java.net.URI;
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
        FileContext fs = Settings.instance().getPageStoreFileSystem();
        if (fs.util().exists(pagesRoot)) {
            HdfsUtils.recursiveDeleteLockFiles(fs, pagesRoot);
        }
    }
    @Override
    public Page save(Source source, Person creator) throws IOException
    {
        Page retVal = null;

        //create this here, so we don't boot the filesystem on validation error
        URI validUri = DefaultPageImpl.create(source.getBaseUri());

        //TODO: BIG ONE, make this transactional with a good rollback (history entry might be a good starting point)
        //now execute the FS changes
        FileContext fs = Settings.instance().getPageStoreFileSystem();

        PathInfo pathInfo = new HdfsPathInfo(fs, validUri);
        //we need to use the abstract type here to have access to the package private setters
        AbstractPage page = new DefaultPageImpl(pathInfo);

        try (LockFile lock = pathInfo.acquireLock()) {

            //make sure the path dirs exist
            //Note: this also works for dirs, since they're special files inside the actual dir
            fs.mkdir(pathInfo.getPath().getParent(), FsPermission.getDirDefault(), true);

            //we're overwriting; make an entry in the history folder
            //TODO maybe we want to make this asynchronous?
            PathInfo historyEntry = null;
            if (fs.util().exists(pathInfo.getPath())) {
                historyEntry = this.addHistoryEntry(fs, pathInfo);
            }

            //we'll read everything into a string for ease of use
            String sourceHtml;
            try (InputStream is = source.openNewInputStream()) {
                sourceHtml = org.apache.commons.io.IOUtils.toString(is);
            }

            //save the original page html
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(fs.create(pathInfo.getPath(), EnumSet.of(CreateFlag.CREATE, CreateFlag.OVERWRITE))))) {
                writer.write(sourceHtml);
            }

            //save the normalized page html
            Path normalizedHtml = page.getNormalizedPageProxyPath();
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(fs.create(normalizedHtml, EnumSet.of(CreateFlag.CREATE, CreateFlag.OVERWRITE))))) {
                writer.write(new PageHtmlParser().parse(sourceHtml, source.getBaseUri(), true));
            }

            //parse to jsonld and save it
            Path jsonldFile = page.getJsonLDProxyPath();
            Model model = page.createImporter().importDocument(source);
            try (OutputStream os = fs.create(jsonldFile, EnumSet.of(CreateFlag.CREATE, CreateFlag.OVERWRITE))) {
                page.createExporter().exportModel(model, os);
            }

            // read it back in and parse it because it's the link between this (where we have HDFS access)
            // and the page indexer (where we work with generic json objects)
            try (InputStream is = fs.open(jsonldFile)) {
                page.setJsonLDNode(Json.read(is, JsonNode.class));
            }

            //save the HASH of the file
            //TODO make this uniform with the watch code
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(fs.create(pathInfo.getMetaHashFile(), EnumSet.of(CreateFlag.CREATE, CreateFlag.OVERWRITE))))) {
                writer.write(pathInfo.calcHashChecksum());
            }

            //save the page metadata (read it in if it exists)
            this.writeMetadata(fs, pathInfo, creator, page.createMetadataWriter());

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
     * @param fs
     * @param pathInfo
     * @return the successfully created history entry or null of something went wrong
     * @throws IOException
     */
    private PathInfo addHistoryEntry(FileContext fs, PathInfo pathInfo) throws IOException
    {
        // Note: it makes sense to use the now timestamp because we're about to create a snapshot of the situation _now_
        // we can't really rely on other timestamps because which one should we take?
        DateTime stamp = DateTime.now();

        // first, we'll create a snapshot of the meta folder to a sibling folder. We can't copy it to it's final destination
        // because that is a subfolder of the folder we're copying and we'll encounter odd recursion.
        Path snapshotMetaFolder = new Path(pathInfo.getMetaFolder().getParent(), pathInfo.getMetaFolder().getName() + Constants.TEMP_SNAPSHOT_SUFFIX);

        //this is the new history entry folder
        Path newHistoryEntryFolder = new Path(pathInfo.getMetaHistoryFolder(), Constants.FOLDER_TIMESTAMP_FORMAT.print(stamp));

        //the history entry destination
        PathInfo historyEntry = new HdfsPathInfo(fs, new Path(newHistoryEntryFolder, pathInfo.getPath().getName()));

        boolean success = false;
        try {
            if (!fs.util().copy(pathInfo.getMetaFolder(), snapshotMetaFolder)) {
                throw new IOException("Error while adding a history entry for " + pathInfo + ": couldn't create a snapshot of the meta folder; " + snapshotMetaFolder);
            }

            //we're not copying the history folder into the snapshot folder; that would be recursion
            Path snapshotHistoryFolder = new Path(snapshotMetaFolder, Constants.META_SUBFOLDER_HISTORY);
            if (fs.util().exists(snapshotHistoryFolder)) {
                if (!fs.delete(snapshotHistoryFolder, true)) {
                    throw new IOException("Error while adding a history entry for " + pathInfo + ": couldn't delete the history folder of the temp meta snapshot folder; " + snapshotMetaFolder);
                }
            }

            if (fs.util().exists(newHistoryEntryFolder)) {
                throw new IOException("Error while adding a history entry for " + pathInfo + ": history folder already existed; " + newHistoryEntryFolder);
            }
            else {
                fs.mkdir(newHistoryEntryFolder, FsPermission.getDirDefault(), true);
            }

            //if we get here without problems, we start copying the original file to it's history destination
            if (!fs.util().copy(pathInfo.getPath(), historyEntry.getPath())) {
                throw new IOException("Error while adding a history entry for " + pathInfo + ": couldn't copy the original file to it's final destination " + historyEntry.getPath());
            }

            //move the snapshot in it's place
            fs.rename(snapshotMetaFolder, historyEntry.getMetaFolder());

            success = true;
        }
        finally {
            //cleanup on error
            if (!success) {
                //Note: this isn't fail-safe, but a good try. (all we can do is log the error if for some reason we couldn't do a clean rollback)
                try {
                    fs.delete(snapshotMetaFolder, true);
                }
                catch (IOException e) {
                    Logger.error("Error while cleaning up the temp snapshot meta folder of a failed history attempt for " + pathInfo.getPath() + "; " + snapshotMetaFolder, e);
                }

                try {
                    fs.delete(newHistoryEntryFolder, true);
                }
                catch (IOException e) {
                    Logger.error("Error while cleaning up the history entry folder of a failed history attempt for " + pathInfo.getPath() + "; " + newHistoryEntryFolder, e);
                }
            }
        }

        return success ? historyEntry : null;
    }
    private void writeMetadata(FileContext fs, PathInfo pathInfo, Person creator, MetadataWriter metadataWriter) throws IOException
    {
        metadataWriter.open(pathInfo);
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
