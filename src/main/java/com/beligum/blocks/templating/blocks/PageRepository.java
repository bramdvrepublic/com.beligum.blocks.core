package com.beligum.blocks.templating.blocks;

import com.beligum.base.models.Person;
import com.beligum.base.resources.RegisteredMimeType;
import com.beligum.base.resources.ResourceRepositoryPrefix;
import com.beligum.base.resources.ifaces.Resource;
import com.beligum.base.resources.ifaces.ResourceRepository;
import com.beligum.base.resources.ifaces.ResourceRequest;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.fs.LockFile;
import com.beligum.blocks.fs.hdfs.HdfsZipUtils;
import com.beligum.blocks.fs.ifaces.BlocksResource;
import com.beligum.blocks.fs.logger.PageLogEntry;
import com.beligum.blocks.fs.logger.ifaces.LogWriter;
import com.beligum.blocks.fs.metadata.ifaces.MetadataWriter;
import com.beligum.blocks.fs.pages.ReadOnlyPage;
import com.beligum.blocks.fs.pages.ReadWritePage;
import com.beligum.blocks.fs.pages.ifaces.Page;
import com.beligum.blocks.rdf.ifaces.Format;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.fs.permission.FsPermission;
import org.openrdf.model.Model;

import java.io.*;
import java.net.URI;
import java.time.Instant;
import java.util.EnumSet;

/**
 * Created by bram on 12/27/16.
 */
public class PageRepository implements ResourceRepository
{
    //-----CONSTANTS-----
    public static final String PUBLIC_PATH_PREFIX = "/";

    //-----VARIABLES-----
    private Settings settings;
    private FileContext writeContext;

    //-----CONSTRUCTORS-----
    public PageRepository() throws IOException
    {
        this.settings = Settings.instance();
        this.writeContext = StorageFactory.getPageStoreFileSystem();
    }

    //-----PUBLIC METHODS-----
    @Override
    public ResourceRepositoryPrefix[] getPrefixes()
    {
        return new ResourceRepositoryPrefix[] { new ResourceRepositoryPrefix(this, URI.create(PUBLIC_PATH_PREFIX), RegisteredMimeType.HTML) };
    }
    @Override
    public boolean isImmutable()
    {
        //Our HTML files are never static, it's the whole point of the blocks system
        return false;
    }
    @Override
    public boolean isReadOnly()
    {
        //both save() and delete() are implemented, so we fully support writing away HTML files
        return false;
    }
    @Override
    public Resource get(ResourceRequest resourceRequest)
    {
        Resource retVal = null;

        try {
            Page page = new ReadOnlyPage(resourceRequest, this);

            //Here, we decide on the normalized html path to see if the page exists or not
            // (note that we actually could check on the original and re-generate the normalized if necessary, but we decided to take the safe road)
            //Also note that out interface demands us to return null if the resourceRequest can't be resolved, so this check is necessary!
            if (page.getFileContext().util().exists(page.getNormalizedPageProxyPath())) {
                retVal = page;
            }
        }
        catch (IOException e) {
            Logger.error("Error while resolving html resource for " + resourceRequest.getUri(), e);
        }

        //DISABLED because this is a security risk!
        //        //if we found nothing, we just pass it on to the super classpath resolver
        //        //note: this is necessary because this resolver is used with a very short "/" prefix and will match a lot
        //        if (retVal == null) {
        //            retVal = super.resolve(resourceRequest);
        //        }

        return retVal;
    }
//    @Override
//    public PageIterator getAll(boolean readOnly, String relativeStartFolder, FullPathGlobFilter filter, int depth) throws IOException
//    {
//        URI rootPath = readOnly ? Settings.instance().getPagesViewPath() : Settings.instance().getPagesStorePath();
//        URI startFolder = rootPath;
//        if (!StringUtils.isEmpty(relativeStartFolder)) {
//            //make sure it doesn't remove leading paths
//            while (relativeStartFolder.startsWith("/")) {
//                relativeStartFolder = relativeStartFolder.substring(1);
//            }
//
//            startFolder = startFolder.resolve(relativeStartFolder);
//        }
//
//        FileContext fileContext = readOnly ? StorageFactory.getPageViewFileSystem() : StorageFactory.getPageStoreFileSystem();
//        return new PageIterator(fileContext, new Path(rootPath), new Path(startFolder), readOnly, filter, depth);
//    }
    @Override
    public Resource save(Resource resource, Person editor) throws IOException, UnsupportedOperationException
    {
        Page retVal = null;

        Page resourcePage = resource.unwrap(Page.class);

        //Translates the address of the source page to a page (and a save location on disk),
        //extracting all necessary information from the URL and normalizing the address in the mean time
        Page newPage = new ReadWritePage(resourcePage);

        //cache some vars
        BlocksResource blocksResource = newPage.getResourcePath();
        FileContext fileContext = blocksResource.getFileContext();

        //will synchronize the metadata directory by creating/releasing a lock file
        try (LockFile lock = blocksResource.acquireLock()) {

            // Note: it makes sense to use the now timestamp because we're about to create a snapshot of the situation _now_
            // we can't really rely on other timestamps because which one should we take?
            Instant stamp = Instant.now();

            //prepare the HTML for saving; this is the only place we can modify the source
            // because later on, the analyzer will have run
            source.prepareForSaving(fileContext);

            //we'll read everything into a string for performance and ease of use
            String sourceHtml;
            try (InputStream is = source.newInputStream()) {
                sourceHtml = IOUtils.toString(is);
            }

            //pre-calculate the hash based on the incoming string and compare it with the stored version to abort early if nothing changed
            Path hashFile = blocksResource.getHashFile();
            boolean nothingChanged = false;
            String newHash = null;
            try (InputStream is = new ByteArrayInputStream(sourceHtml.getBytes())) {
                newHash = HdfsResourcePath.calcHashChecksumFor(is);
            }
            if (fileContext.util().exists(hashFile)) {
                try (FSDataInputStream is = fileContext.open(hashFile)) {
                    String existingHash = IOUtils.toString(is);
                    nothingChanged = existingHash != null && existingHash.equals(newHash);
                }
            }

            //for now, we'll return null if nothing changed
            //update: changed because sometimes we want to re-index the page (eg. in an updated manner), so just return the old page now
            if (nothingChanged) {
                retVal = newPage;
            }
            else {

                //make sure the path dirs exist
                //Note: this also works for dirs, since they're special files inside the actual dir
                fileContext.mkdir(blocksResource.getLocalStoragePath().getParent(), FsPermission.getDirDefault(), true);

                //we're overwriting; make an entry in the history folder
                //TODO maybe we want to make this asynchronous?
                boolean existed = fileContext.util().exists(blocksResource.getLocalStoragePath());
                if (existed) {
                    this.addHistoryEntry(fileContext, blocksResource, stamp);
                }

                //save the HASH of the file
                //TODO make this uniform with the watch code
                try (Writer writer = new BufferedWriter(new OutputStreamWriter(fileContext.create(hashFile, EnumSet.of(CreateFlag.CREATE, CreateFlag.OVERWRITE), Options.CreateOpts.createParent())))) {
                    writer.write(newHash);
                }

                //save the original page html
                try (Writer writer = new BufferedWriter(
                                new OutputStreamWriter(fileContext.create(blocksResource.getLocalStoragePath(), EnumSet.of(CreateFlag.CREATE, CreateFlag.OVERWRITE), Options.CreateOpts.createParent())))) {
                    writer.write(sourceHtml);
                }

                //save the normalized page html
                Path normalizedHtml = newPage.getNormalizedPageProxyPath();
                try (Writer writer = new BufferedWriter(
                                new OutputStreamWriter(fileContext.create(normalizedHtml, EnumSet.of(CreateFlag.CREATE, CreateFlag.OVERWRITE), Options.CreateOpts.createParent())))) {
                    writer.write(source.getNormalizedHtml());
                }

                //parse and generate the RDF model
                Model rdfModel = newPage.createImporter(Format.RDFA).importDocument(source);
                //export the RDF model to the storage file (note that this file will be re-read to save to the triple store)
                try (OutputStream os = fileContext.create(newPage.getRdfExportFile(), EnumSet.of(CreateFlag.CREATE, CreateFlag.OVERWRITE), Options.CreateOpts.createParent())) {
                    newPage.createExporter(newPage.getRdfExportFileFormat()).exportModel(rdfModel, os);
                }

                this.writeLogEntry(newPage, creator, stamp, existed ? PageLogEntry.Action.UPDATE : PageLogEntry.Action.CREATE);

                //save the page metadata (read it in if it exists)
                //Note: disabled and more or less replaced by the writeLogEntry() above because it was too error prone on crashes
                //this.writeMetadata(fileContext, resourcePath, creator, newPage.createMetadataWriter());

                retVal = newPage;
            }
        }

        return retVal;
    }
    @Override
    public Resource delete(Resource resource, Person editor) throws IOException, UnsupportedOperationException
    {
        Page retVal = null;

        Page page = new ReadWritePage(publicAddress);

        //cache some vars
        BlocksResource blocksResource = page.getResourcePath();
        FileContext fileContext = blocksResource.getFileContext();

        try (LockFile lock = blocksResource.acquireLock()) {

            Instant stamp = Instant.now();

            //make sure the path dirs exist
            //Note: this also works for dirs, since they're special files inside the actual dir
            fileContext.mkdir(blocksResource.getLocalStoragePath().getParent(), FsPermission.getDirDefault(), true);

            //save the page metadata BEFORE we create the history entry (to make sure we save who deleted it)
            this.writeMetadata(fileContext, blocksResource, deleter, page.createMetadataWriter());

            //we're overwriting; make an entry in the history folder
            //TODO maybe we want to make this asynchronous?
            if (fileContext.util().exists(blocksResource.getLocalStoragePath())) {
                this.addHistoryEntry(fileContext, blocksResource, stamp);
            }

            //delete the original page html and leave the rest alone
            fileContext.delete(blocksResource.getLocalStoragePath(), false);

            this.writeLogEntry(page, deleter, stamp, PageLogEntry.Action.DELETE);

            //list everything under meta folder and delete it, except for the HISTORY folder
            RemoteIterator<FileStatus> metaEntries = fileContext.listStatus(blocksResource.getDotFolder());
            while (metaEntries.hasNext()) {
                FileStatus child = metaEntries.next();
                if (!child.getPath().equals(blocksResource.getHistoryFolder())) {
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
     * <p>
     * NOTE: this is not optimized in the light of:
     * - the new transactional-ness of the filesystem
     * - the fact we're gzipping the entire folder at the end
     *
     * @param fs
     * @param blocksResource
     * @return the successfully created history entry or null of something went wrong
     * @throws IOException
     */
    private BlocksResource addHistoryEntry(FileContext fs, BlocksResource blocksResource, Instant stamp) throws IOException
    {
        // first, we'll create a snapshot of the meta folder to a sibling folder. We can't copy it to it's final destination
        // because that is a subfolder of the folder we're copying and we'll encounter odd recursion.
        Path snapshotMetaFolder = new Path(blocksResource.getDotFolder().getParent(), blocksResource.getDotFolder().getName() + Constants.TEMP_SNAPSHOT_SUFFIX);

        //this is the new history entry folder
        Path newHistoryEntryFolder = new Path(blocksResource.getHistoryFolder(), Constants.FOLDER_TIMESTAMP_FORMAT.format(stamp));

        //the history entry destination (eg. the original file in the history entry folder)
        BlocksResource historyEntry = new HdfsResourcePath(fs, new Path(newHistoryEntryFolder, blocksResource.getLocalStoragePath().getName()));

        boolean success = false;

        //copy the meta folder to the temp history entry folder
        if (!fs.util().copy(blocksResource.getDotFolder(), snapshotMetaFolder)) {
            throw new IOException("Error while adding a history entry for " + blocksResource + ": couldn't create a snapshot of the meta folder; " + snapshotMetaFolder);
        }

        //we're not copying the history folder into the snapshot folder; that would be recursion, so delete it
        Path snapshotHistoryFolder = new Path(snapshotMetaFolder, Constants.META_SUBFOLDER_HISTORY);
        if (fs.util().exists(snapshotHistoryFolder)) {
            if (!fs.delete(snapshotHistoryFolder, true)) {
                throw new IOException("Error while adding a history entry for " + blocksResource + ": couldn't delete the history folder of the temp meta snapshot folder; " + snapshotMetaFolder);
            }
        }

        if (fs.util().exists(newHistoryEntryFolder)) {
            throw new IOException("Error while adding a history entry for " + blocksResource + ": history folder already existed; " + newHistoryEntryFolder);
        }
        else {
            fs.mkdir(newHistoryEntryFolder, FsPermission.getDirDefault(), true);
        }

        //if we get here without problems, we start copying the original file to it's history destination
        if (!fs.util().copy(blocksResource.getLocalStoragePath(), historyEntry.getLocalStoragePath())) {
            throw new IOException("Error while adding a history entry for " + blocksResource + ": couldn't copy the original file to it's final destination " + historyEntry.getLocalStoragePath());
        }

        //move the snapshot in it's place
        fs.rename(snapshotMetaFolder, historyEntry.getDotFolder());

        //zip it
        HdfsZipUtils.gzipTarFolder(fs, newHistoryEntryFolder, new Path(newHistoryEntryFolder.toUri().toString() + ".tgz"));

        //and remove the original
        fs.delete(newHistoryEntryFolder, true);

        success = true;

        //Note: no need to cleanup, the XA filesystem will do that for us

        return success ? historyEntry : null;
    }
    private void writeLogEntry(Page page, Person creator, Instant stamp, PageLogEntry.Action action) throws IOException
    {
        try (LogWriter logWriter = page.createLogWriter()) {
            logWriter.writeLogEntry(new PageLogEntry(stamp, creator, page, action));
        }
    }
    private void writeMetadata(FileContext fs, BlocksResource blocksResource, Person creator, MetadataWriter metadataWriter) throws IOException
    {
        metadataWriter.open(blocksResource);
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
