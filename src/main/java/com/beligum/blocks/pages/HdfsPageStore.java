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
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.joda.time.DateTime;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
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
    public void save(URI uri, String content, Person creator) throws IOException
    {
        Page page = new PageImpl(uri);

        //now execute the FS changes
        try (FileSystem fs = this.getFileSystem()) {

            PathInfo<Path> pathInfo = new HdfsPathInfo(fs, page.getSaveFile());

            try (LockFile lock = pathInfo.acquireLock()) {

                // Note: it makes sense to use the now timestamp because we're about to create a snapshot of the situation _now_
                // we can't really rely on other timestamps because which one should we take?
                DateTime stamp = DateTime.now();

                //make sure the path dirs exist
                //Note: this also works for dirs, since they're special files inside the actual dir
                fs.mkdirs(pathInfo.getPath().getParent());

                //we're overwriting; make an entry in the history folder
                if (fs.exists(pathInfo.getPath())) {
                    Path historyFolder = pathInfo.getMetaHistoryFolder();

                    // first, we'll create a snapshot of the meta folder to a sibling folder. We can't copy it to it's final destination
                    // because that is a subfolder of the folder we're copying.
                    Path tempMetaFolderCopy = new Path(pathInfo.getMetaFolder().getParent(), pathInfo.getMetaFolder().getName()+Constants.TEMP_META_FOLDER_SNAPSHOT_SUFFIX);

                    //this is the root folder for the snapshot
                    Path newHistoryEntryFolder = new Path(historyFolder, Constants.FOLDER_TIMESTAMP_FORMAT.print(stamp));

                    //the two version destinations
                    Path historyOriginal = new Path(newHistoryEntryFolder, pathInfo.getPath().getName());
                    Path historyMetaFolder = new Path(newHistoryEntryFolder, pathInfo.getMetaFolder().getName());

                    boolean versioningSuccess = false;
                    try {
                        FileUtil.copy(fs, pathInfo.getMetaFolder(), fs, tempMetaFolderCopy, false, fs.getConf());

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

                //save the page html
                try (Writer writer = new BufferedWriter(new OutputStreamWriter(fs.create(pathInfo.getPath())))) {
                    writer.write(content);
                }

                //save the page metadata (read it in if it exists)
                HdfsMetadataWriter metadataWriter = new EBUCoreHdfsMetadataWriter();
                metadataWriter.init(fs);
                metadataWriter.open(pathInfo);
                //update or fill the ebucore structure with all possible metadata
                metadataWriter.updateCreator(creator);
                metadataWriter.updateTimestamps();
                metadataWriter.write();
                metadataWriter.close();
            }
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
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
