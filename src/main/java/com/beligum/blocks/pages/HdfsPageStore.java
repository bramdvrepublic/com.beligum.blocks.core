package com.beligum.blocks.pages;

import com.beligum.base.auth.models.Person;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.fs.EBUCoreHdfsMetadataWriter;
import com.beligum.blocks.fs.HdfsPathInfo;
import com.beligum.blocks.fs.HdfsUtils;
import com.beligum.blocks.fs.LockFile;
import com.beligum.blocks.fs.ifaces.Constants;
import com.beligum.blocks.fs.ifaces.HdfsMetadataWriter;
import com.beligum.blocks.fs.ifaces.PathInfo;
import com.beligum.blocks.pages.ifaces.Page;
import com.beligum.blocks.pages.ifaces.PageStore;
import com.beligum.blocks.rdf.exporters.JenaExporter;
import com.beligum.blocks.rdf.ifaces.Exporter;
import com.beligum.blocks.rdf.ifaces.Importer;
import com.beligum.blocks.rdf.ifaces.Source;
import com.beligum.blocks.rdf.importers.SesameImporter;
import com.hp.hpl.jena.rdf.model.Model;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.joda.time.DateTime;

import java.io.*;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by bram on 1/14/16.
 */
public class HdfsPageStore implements PageStore
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private Settings settings;

    //-----CONSTRUCTORS-----
    public HdfsPageStore()
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
        try (FileSystem fs = this.getFileSystem()) {
            if (fs.exists(pagesRoot)) {
                HdfsUtils.recursiveDeleteLockFiles(fs, pagesRoot);
            }
        }
    }
    @Override
    public void save(Source source, Person creator) throws IOException
    {
        Page page = new PageImpl(source.getBaseUri());

        //now execute the FS changes
        try (FileSystem fs = this.getFileSystem()) {

            PathInfo<Path> pathInfo = new HdfsPathInfo(fs, page.getSaveFile());

            try (LockFile lock = pathInfo.acquireLock()) {

                //make sure the path dirs exist
                //Note: this also works for dirs, since they're special files inside the actual dir
                fs.mkdirs(pathInfo.getPath().getParent());

                //we're overwriting; make an entry in the history folder
                //TODO maybe we want to make this asynchronous?
                if (fs.exists(pathInfo.getPath())) {
                    this.createVersion(fs, pathInfo);
                }

                //we'll read everything into a string for ease of use
                String sourceHtml;
                try (InputStream is = source.openNewInputStream()) {
                    sourceHtml = org.apache.commons.io.IOUtils.toString(is);
                }

                //save the original page html
                try (Writer writer = new BufferedWriter(new OutputStreamWriter(fs.create(pathInfo.getPath())))) {
                    writer.write(sourceHtml);
                }

                //save the normalized page html
                Path normalizedHtml = new Path(pathInfo.getMetaProxyFolder(PAGE_PROXY_NORMALIZED_MIME_TYPE), PAGE_PROXY_NORMALIZED_FILE_NAME);
                try (Writer writer = new BufferedWriter(new OutputStreamWriter(fs.create(normalizedHtml)))) {
                    writer.write(new PageHtmlParser().parse(sourceHtml, source.getBaseUri(), true));
                }

                //parse to jsonld and save it
                Path jsonld = new Path(pathInfo.getMetaProxyFolder(PAGE_PROXY_RDF_JSONLD_MIME_TYPE), PAGE_PROXY_RDF_JSONLD_FILE_NAME);
                Model model = new SesameImporter().importDocument(source, Importer.Format.RDFA);
                try (OutputStream os = fs.create(jsonld)) {
                    new JenaExporter().exportModel(model, Exporter.Format.JSONLD, os);
                    //JsonNode jsonLD = Json.read(os.toString(), JsonNode.class);
                }

                //save the HASH of the file
                //TODO make this uniform with the watch code
                try (Writer writer = new BufferedWriter(new OutputStreamWriter(fs.create(pathInfo.getMetaHashFile())))) {
                    writer.write(pathInfo.calcHashChecksum());
                }

                //save the page metadata (read it in if it exists)
                this.writeMetadata(fs, pathInfo, creator, new EBUCoreHdfsMetadataWriter());
            }
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private void createVersion(FileSystem fs, PathInfo<Path> pathInfo) throws IOException
    {
        // Note: it makes sense to use the now timestamp because we're about to create a snapshot of the situation _now_
        // we can't really rely on other timestamps because which one should we take?
        DateTime stamp = DateTime.now();

        Path historyFolder = pathInfo.getMetaHistoryFolder();

        // first, we'll create a snapshot of the meta folder to a sibling folder. We can't copy it to it's final destination
        // because that is a subfolder of the folder we're copying.
        Path tempMetaFolderCopy = new Path(pathInfo.getMetaFolder().getParent(), pathInfo.getMetaFolder().getName() + Constants.TEMP_META_FOLDER_SNAPSHOT_SUFFIX);

        //this is the root folder for the snapshot
        Path newHistoryEntryFolder = new Path(historyFolder, Constants.FOLDER_TIMESTAMP_FORMAT.print(stamp));

        //the two version destinations
        Path historyOriginal = new Path(newHistoryEntryFolder, pathInfo.getPath().getName());
        Path historyMetaFolder = new Path(newHistoryEntryFolder, pathInfo.getMetaFolder().getName());

        boolean versioningSuccess = false;
        try {
            FileUtil.copy(fs, pathInfo.getMetaFolder(), fs, tempMetaFolderCopy, false, fs.getConf());

            //we're not copying the history folder into the snapshot folder; that would be recursion
            Path tempMetaFolderCopyHistory = new Path(tempMetaFolderCopy, Constants.META_SUBFOLDER_HISTORY);
            fs.delete(tempMetaFolderCopyHistory, true);

            if (fs.exists(newHistoryEntryFolder)) {
                throw new IOException("Error while creating the history folder because it already existed; " + newHistoryEntryFolder);
            }
            else if (!fs.mkdirs(newHistoryEntryFolder)) {
                throw new IOException("Error while creating the history folder; " + newHistoryEntryFolder);
            }

            FileUtil.copy(fs, pathInfo.getPath(), fs, historyOriginal, false, fs.getConf());
            fs.rename(tempMetaFolderCopy, historyMetaFolder);

            versioningSuccess = true;
        }
        finally {
            //cleanup on error
            if (!versioningSuccess) {
                fs.delete(tempMetaFolderCopy, true);
                fs.delete(newHistoryEntryFolder, true);
                fs.delete(historyOriginal, true);
                fs.delete(historyMetaFolder, true);

                throw new IOException("Error happened while versioning (tried to clean up as good as possible) "+pathInfo.getPath()+" to "+newHistoryEntryFolder);
            }
        }
    }
    private void writeMetadata(FileSystem fs, PathInfo<Path> pathInfo, Person creator, HdfsMetadataWriter metadataWriter) throws IOException
    {
        metadataWriter.init(fs);
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
    /**
     * @return this returns a NEW filesystem, that needs to be (auto) closed
     */
    private FileSystem getFileSystem() throws IOException
    {
        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.HDFS_PAGE_FS_CONFIG)) {
            Configuration conf = new Configuration();
            URI pageStorePath = Settings.instance().getPagesStorePath();
            if (StringUtils.isEmpty(pageStorePath.getScheme())) {
                //make sure we have a com.beligum.blocks.schema.schema
                pageStorePath = URI.create("file://" + pageStorePath.toString());
                Logger.warn("The page store path doesn't have a com.beligum.blocks.schema.schema, adding the HDFS 'file://' prefix to use the local file system; " + pageStorePath.toString());
            }
            conf.set(CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY, pageStorePath.toString());

            //note: if fs.defaultFS is set here, this might overwirte the path above
            HashMap<String, String> extraProperties = Settings.instance().getElasticSearchProperties();
            if (extraProperties != null) {
                for (Map.Entry<String, String> entry : extraProperties.entrySet()) {
                    if (entry.getKey().equals(CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY)) {
                        Logger.warn("Watch out, your HDFS settings overwrite the pages store path; " + entry.getValue());
                    }
                    conf.set(entry.getKey(), entry.getValue());
                }
            }

            R.cacheManager().getApplicationCache().put(CacheKeys.HDFS_PAGE_FS_CONFIG, conf);
        }

        return FileSystem.get((Configuration) R.cacheManager().getApplicationCache().get(CacheKeys.HDFS_PAGE_FS_CONFIG));
    }
}
