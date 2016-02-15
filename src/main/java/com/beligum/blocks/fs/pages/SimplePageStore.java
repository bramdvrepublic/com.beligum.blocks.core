package com.beligum.blocks.fs.pages;

import com.beligum.base.auth.models.Person;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.fs.HdfsPathInfo;
import com.beligum.blocks.fs.HdfsUtils;
import com.beligum.blocks.fs.LockFile;
import com.beligum.blocks.fs.hdfs.HdfsZipUtils;
import com.beligum.blocks.fs.ifaces.Constants;
import com.beligum.blocks.fs.ifaces.PathInfo;
import com.beligum.blocks.fs.metadata.ifaces.MetadataWriter;
import com.beligum.blocks.fs.pages.ifaces.Page;
import com.beligum.blocks.fs.pages.ifaces.PageStore;
import com.beligum.blocks.rdf.sources.HtmlSource;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.fs.permission.FsPermission;

import javax.ws.rs.core.UriBuilder;
import java.io.*;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
    public Page save(HtmlSource source, Person creator) throws IOException
    {
        Page retVal = null;

        //create this here, so we don't boot the filesystem on validation error
        URI localResourceUri = DefaultPageImpl.create(source.getBaseUri(), Settings.instance().getPagesStorePath());

        //now execute the FS changes
        FileContext fs = Settings.instance().getPageStoreFileSystem();

        PathInfo pathInfo = new HdfsPathInfo(fs, localResourceUri);
        //we need to use the abstract type here to have access to the package private setters
        AbstractPage page = new DefaultPageImpl(pathInfo);

        //will synchronize the metadata directory by creating/releasing a lock file
        try (LockFile lock = pathInfo.acquireLock()) {

            //prepare the HTML for saving
            source.prepareForSaving(true, true);

            //find translations on disk
            Set<URI> translations = source.getTranslations();
            Map<String, Locale> siteLanguages = Settings.instance().getLanguages();
            for (Map.Entry<String, Locale> l : siteLanguages.entrySet()) {
                if (!l.getValue().equals(source.getHtmlLocale())) {
                    UriBuilder translatedUri = UriBuilder.fromUri(source.getBaseUri());
                    Locale detectedLang = R.i18nFactory().getUrlLocale(source.getBaseUri(), translatedUri, l.getValue());
                    if (detectedLang != null) {
                        Logger.error(translatedUri.build());
                    }
                    else {
                        throw new IOException("No language detected in the page url; can't pull a translated url and can't proceed; " + localResourceUri);
                    }
                }
            }

            //we'll read everything into a string for performance and ease of use
            String sourceHtml;
            try (InputStream is = source.openNewInputStream()) {
                sourceHtml = IOUtils.toString(is);
            }

            //pre-calculate the hash based on the incoming string and compare it with the stored version to abort early if nothing changed
            Path hashFile = pathInfo.getMetaHashFile();
            boolean nothingChanged = false;
            String newHash = null;
            try (InputStream is = new ByteArrayInputStream(sourceHtml.getBytes())) {
                newHash = HdfsPathInfo.calcHashChecksumFor(is);
            }
            if (fs.util().exists(hashFile)) {
                try (FSDataInputStream is = fs.open(hashFile)) {
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
                fs.mkdir(pathInfo.getPath().getParent(), FsPermission.getDirDefault(), true);

                //we're overwriting; make an entry in the history folder
                //TODO maybe we want to make this asynchronous?
                if (fs.util().exists(pathInfo.getPath())) {
                    this.addHistoryEntry(fs, pathInfo);
                }

                //save the HASH of the file
                //TODO make this uniform with the watch code
                try (Writer writer = new BufferedWriter(new OutputStreamWriter(fs.create(hashFile, EnumSet.of(CreateFlag.CREATE, CreateFlag.OVERWRITE), Options.CreateOpts.createParent())))) {
                    writer.write(newHash);
                }

                //save the original page html
                try (Writer writer = new BufferedWriter(new OutputStreamWriter(fs.create(pathInfo.getPath(), EnumSet.of(CreateFlag.CREATE, CreateFlag.OVERWRITE), Options.CreateOpts.createParent())))) {
                    writer.write(sourceHtml);
                }

                //save the normalized page html
                Path normalizedHtml = page.getNormalizedPageProxyPath();
                try (Writer writer = new BufferedWriter(new OutputStreamWriter(fs.create(normalizedHtml, EnumSet.of(CreateFlag.CREATE, CreateFlag.OVERWRITE), Options.CreateOpts.createParent())))) {
                    writer.write(source.getNormalizedHtml());
                }

                //save the source for later use (eg. in indexer)
                page.setSource(source);

                //parse and store the RDF model in the page
                page.setRDFModel(page.createImporter().importDocument(source));

                //export the RDF model to the storage file (JSON-LD)
                try (OutputStream os = fs.create(page.getRdfExportFile(), EnumSet.of(CreateFlag.CREATE, CreateFlag.OVERWRITE), Options.CreateOpts.createParent())) {
                    //TODO enable this again!!
                    //page.createExporter().exportModel(page.getRDFModel(), os);
                }

                //save the page metadata (read it in if it exists)
                this.writeMetadata(fs, pathInfo, creator, page.createMetadataWriter());

                retVal = page;
            }
        }

        return retVal;
    }
    @Override
    public Page delete(URI uri, Person deleter) throws IOException
    {
        Page retVal = null;

        //create this here, so we don't boot the filesystem on validation error
        URI validUri = DefaultPageImpl.create(uri, Settings.instance().getPagesStorePath());

        //now execute the FS changes
        FileContext fs = Settings.instance().getPageStoreFileSystem();

        PathInfo pathInfo = new HdfsPathInfo(fs, validUri);
        //we need to use the abstract type here to have access to the package private setters
        AbstractPage page = new DefaultPageImpl(pathInfo);

        try (LockFile lock = pathInfo.acquireLock()) {

            //make sure the path dirs exist
            //Note: this also works for dirs, since they're special files inside the actual dir
            fs.mkdir(pathInfo.getPath().getParent(), FsPermission.getDirDefault(), true);

            //save the page metadata BEFORE we create the history entry (to make sure we save who deleted it)
            this.writeMetadata(fs, pathInfo, deleter, page.createMetadataWriter());

            //we're overwriting; make an entry in the history folder
            //TODO maybe we want to make this asynchronous?
            if (fs.util().exists(pathInfo.getPath())) {
                this.addHistoryEntry(fs, pathInfo);
            }

            //delete the original page html and leave the rest alone
            fs.delete(pathInfo.getPath(), false);

            //list everything under meta folder and delete it, except for the HISTORY folder
            RemoteIterator<FileStatus> metaEntries = fs.listStatus(pathInfo.getMetaFolder());
            while (metaEntries.hasNext()) {
                FileStatus child = metaEntries.next();
                if (!child.getPath().equals(pathInfo.getMetaHistoryFolder())) {
                    fs.delete(child.getPath(), true);
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
     * @param pathInfo
     * @return the successfully created history entry or null of something went wrong
     * @throws IOException
     */
    private PathInfo addHistoryEntry(FileContext fs, PathInfo pathInfo) throws IOException
    {
        // Note: it makes sense to use the now timestamp because we're about to create a snapshot of the situation _now_
        // we can't really rely on other timestamps because which one should we take?
        ZonedDateTime stamp = ZonedDateTime.now();

        // first, we'll create a snapshot of the meta folder to a sibling folder. We can't copy it to it's final destination
        // because that is a subfolder of the folder we're copying and we'll encounter odd recursion.
        Path snapshotMetaFolder = new Path(pathInfo.getMetaFolder().getParent(), pathInfo.getMetaFolder().getName() + Constants.TEMP_SNAPSHOT_SUFFIX);

        //this is the new history entry folder
        Path newHistoryEntryFolder = new Path(pathInfo.getMetaHistoryFolder(), Constants.FOLDER_TIMESTAMP_FORMAT.format(stamp));

        //the history entry destination (eg. the original file in the history entry folder)
        PathInfo historyEntry = new HdfsPathInfo(fs, new Path(newHistoryEntryFolder, pathInfo.getPath().getName()));

        boolean success = false;

        //copy the meta folder to the temp history entry folder
        if (!fs.util().copy(pathInfo.getMetaFolder(), snapshotMetaFolder)) {
            throw new IOException("Error while adding a history entry for " + pathInfo + ": couldn't create a snapshot of the meta folder; " + snapshotMetaFolder);
        }

        //we're not copying the history folder into the snapshot folder; that would be recursion, so delete it
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

        //zip it
        HdfsZipUtils.gzipTarFolder(fs, newHistoryEntryFolder, new Path(newHistoryEntryFolder.toUri().toString() + ".tgz"));

        //and remove the original
        fs.delete(newHistoryEntryFolder, true);

        success = true;

        //Note: no need to cleanup, the XA filesystem will do that for us

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
