package com.beligum.blocks.pages;

import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.fs.HdfsPathInfo;
import com.beligum.blocks.fs.HdfsUtils;
import com.beligum.blocks.fs.LockFile;
import com.beligum.blocks.fs.ifaces.Constants;
import com.beligum.blocks.fs.ifaces.PathInfo;
import com.beligum.blocks.pages.ifaces.Page;
import com.beligum.blocks.pages.ifaces.PageStore;
import com.beligum.blocks.schema.ebucore.v1_6.jaxb.*;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.joda.time.DateTime;

import javax.xml.bind.JAXB;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.lang.String;
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
    public void save(URI uri, String content) throws IOException
    {
        Page page = new PageImpl(uri);

        //now execute the FS changes
        try (FileSystem fs = this.getFileSystem()) {

            PathInfo<Path> pathInfo = new HdfsPathInfo(fs, page.getSaveFile());

            try (LockFile lock = pathInfo.acquireLock()) {

                DateTime stamp = DateTime.now();

                //make sure the path dirs exist
                //Note: this also works for dirs, since they're special files inside the actual dir
                fs.mkdirs(pathInfo.getPath().getParent());

                //we're overwriting; make an entry in the history folder
                if (fs.exists(pathInfo.getPath())) {
                    Path historyFolder = pathInfo.getHistoryFolder();

                    // first, we'll create a snapshot of the meta folder to a sibling folder. We can't copy it to it's final destination
                    // because that is a subfolder of the folder we're copying.
                    Path tempMetaFolderCopy = new Path(pathInfo.getMetaFolder().getParent(), pathInfo.getMetaFolder().getName()+Constants.METADATA_TEMP_META_FOLDER_SNAPSHOT_SUFFIX);
                    FileUtil.copy(fs, pathInfo.getMetaFolder(), fs, tempMetaFolderCopy, false, fs.getConf());

                    // Note: it makes sense to use the now timestamp because we're about to create a snapshot of the situation _now_
                    // we can't really rely on other timestamps because which one should we take?
                    Path newHistoryEntryFolder = new Path(historyFolder, Constants.FOLDER_TIMESTAMP_FORMAT.print(stamp));
                    if (fs.exists(newHistoryEntryFolder)) {
                        throw new IOException("Error while creating the history folder because it already existed; "+newHistoryEntryFolder);
                    }
                    else if (!fs.mkdirs(newHistoryEntryFolder)) {
                        throw new IOException("Error while creating the history folder; "+newHistoryEntryFolder);
                    }

                    Path historyOriginal = new Path(newHistoryEntryFolder, pathInfo.getPath().getName());
                    Path historyMetaFolder = new Path(newHistoryEntryFolder, pathInfo.getMetaFolder().getName());
                    FileUtil.copy(fs, pathInfo.getPath(), fs, historyOriginal, false, fs.getConf());
                    fs.rename(tempMetaFolderCopy, historyMetaFolder);
                }

                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fs.create(pathInfo.getPath())))) {
                    writer.write(content);

                    final ObjectFactory ebuCoreFactory = new ObjectFactory();
                    final EbuCoreMainType ebuCore = ebuCoreFactory.createEbuCoreMainType();
                    final CoreMetadataType ebuCoreMetaData = ebuCoreFactory.createCoreMetadataType();
                    ebuCore.setCoreMetadata(ebuCoreMetaData);

                    final PartType editorialMetadataPart = ebuCoreFactory.createPartType();
                    editorialMetadataPart.setPartName("EditorialMetadata");
                    final TitleType titleType = ebuCoreFactory.createTitleType();
                    titleType.setTypeLabel("ProgramTitle");
                    final ElementType titleElementType = ebuCoreFactory.createElementType();
                    titleElementType.setLang("fr");
                    titleElementType.setValue("Titre du pogramme");
                    titleType.getTitle().add(titleElementType);
                    editorialMetadataPart.getTitle().add(titleType);
                    ebuCoreMetaData.getPart().add(editorialMetadataPart);

                    StringWriter sw = new StringWriter();
                    JAXB.marshal(ebuCore, sw);
                    Logger.info(sw.getBuffer().toString());


                }
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
