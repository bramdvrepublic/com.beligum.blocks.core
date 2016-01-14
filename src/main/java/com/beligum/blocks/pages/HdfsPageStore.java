package com.beligum.blocks.pages;

import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.pages.ifaces.Page;
import com.beligum.blocks.pages.ifaces.PageStore;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by bram on 1/14/16.
 */
public class HdfsPageStore implements PageStore
{
    //-----CONSTANTS-----
    private static final long DEFAULT_BACK_OFF = 100;
    private static final long DEFAULT_TIMEOUT = 5000;

    //-----VARIABLES-----
    private Settings settings;

    //-----CONSTRUCTORS-----
    public HdfsPageStore()
    {
        this.settings = Settings.instance();
    }

    //-----PUBLIC METHODS-----
    @Override
    public void save(URI uri, String content) throws IOException
    {
        Page page = new PageImpl(uri);

        //now execute the FS changes
        try (FileSystem fs = this.getFileSystem()) {

            //convert it to a hdfs path
            Path hdfsPath = new Path(page.getURI());

            Path lockFile = this.acquireLock(fs, page, DEFAULT_BACK_OFF, DEFAULT_TIMEOUT);

            //make sure the path dirs exist
            //Note: this also works for dirs, since they're special files inside the actual dir
            fs.mkdirs(hdfsPath.getParent());

            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fs.create(hdfsPath)))) {
                writer.write(content);
            }

            this.releaseLock(fs, lockFile);
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
                //make sure we have a schema
                pageStorePath = URI.create("file://"+pageStorePath.toString());
                Logger.warn("The page store path doesn't have a schema, adding the HDFS 'file://' prefix to use the local file system; " + pageStorePath.toString());
            }
            conf.set(CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY, pageStorePath.toString());

            //note: if fs.defaultFS is set here, this might overwirte the path above
            HashMap<String, String> extraProperties = Settings.instance().getElasticSearchProperties();
            if (extraProperties!=null) {
                for (Map.Entry<String, String> entry : extraProperties.entrySet()) {
                    if (entry.getKey().equals(CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY)) {
                        Logger.warn("Watch out, your HDFS settings overwrite the pages store path; "+entry.getValue());
                    }
                    conf.set(entry.getKey(), entry.getValue());
                }
            }

            R.cacheManager().getApplicationCache().put(CacheKeys.HDFS_PAGE_FS_CONFIG, conf);
        }

        return FileSystem.get((Configuration) R.cacheManager().getApplicationCache().get(CacheKeys.HDFS_PAGE_FS_CONFIG));
    }
    /**
     * Pretty simple locking mechanism, probably full of holes, but a first try to create something simple to set up (eg. no Zookeeper)
     * Note: this should work pretty ok, because creation/deletion of files MUST be atomic in HDFS;
     * see https://hadoop.apache.org/docs/stable/hadoop-project-dist/hadoop-common/filesystem/introduction.html
     * @param fs the file system
     * @param page the page
     * @param backOff the time to back off after each try (in ms)
     * @param timeout the time after which an exception is thrown (in ms)
     * @return the lock file
     */
    private Path acquireLock(FileSystem fs, Page page, long backOff, long timeout) throws IOException
    {
        long timer = 0;

        Path lock = new Path(page.getLockFile().toUri());
        while (fs.exists(lock)) {
            try {
                Thread.currentThread().wait(backOff);
            }
            catch (InterruptedException e) {
                throw new IOException("Error happened while waiting on file lock; "+lock, e);
            }
            timer += backOff;

            if (timer>=timeout) {
                throw new IOException("Unable to get lock on file; timeout of ("+timeout+" ms exceeded); "+lock);
            }
        }

        //note: not possible another process 'gets between' the loop above and this, because this will throw an exception if the file already exists.
        if (!fs.createNewFile(lock)) {
            throw new IOException("Unable to create lock file because of an error or because (in the mean time) it already existed; "+lock);
        }

        return lock;
    }
    private void releaseLock(FileSystem fs, Path lockFile) throws IOException
    {
        if (!fs.exists(lockFile)) {
            throw new IOException("Trying to release a lock file that doesn't exist; somethings wrong...; "+lockFile);
        }

        if (!fs.delete(lockFile, false)) {
            throw new IOException("Error happened while releasing a lock file; "+lockFile);
        }
    }
}
