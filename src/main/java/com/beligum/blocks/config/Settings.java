package com.beligum.blocks.config;

import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.fs.hdfs.TransactionalRawLocalFS;
import com.beligum.blocks.fs.hdfs.TransactionalRawLocalFileSystem;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.AbstractFileSystem;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.FsConstants;
import org.apache.hadoop.fs.local.RawLocalFs;
import org.apache.jena.query.Dataset;
import org.apache.jena.tdb.TDBFactory;
import org.xadisk.bridge.proxies.interfaces.XAFileSystem;
import org.xadisk.bridge.proxies.interfaces.XAFileSystemProxy;
import org.xadisk.filesystem.standalone.StandaloneFileSystemConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * Created by bas on 08.10.14.
 */
public class Settings
{
    private static final String PAGES_HDFS_PROPERTIES_KEY = "blocks.core.pages.hdfs.properties.property";
    private static final String ELASTIC_SEARCH_PROPERTIES_KEY = "blocks.core.elastic-search.properties.property";

    private static final String DEFAULT_FILE_EXT = ".html";
    private static final String DEFAULT_LOCK_FILE_EXT = ".lock";

    private static final Class<? extends AbstractFileSystem> DEFAULT_TX_FILESYSTEM = TransactionalRawLocalFS.class;
    private static final String DEFAULT_TX_FILESYSTEM_SCHEME = TransactionalRawLocalFileSystem.SCHEME;

    private static final Class<? extends AbstractFileSystem> DEFAULT_PAGES_VIEW_FS = RawLocalFs.class;
    private static final String DEFAULT_PAGES_VIEW_FS_SCHEME = FsConstants.LOCAL_FS_URI.getScheme();

    private static final String DEFAULT_XADISK_INSTANCE_ID = "xa-1";
    private static final long DEFAULT_XADISK_BOOT_TIMEOUT = 60 * 1000; //1 minute

    private static Settings instance;
    /**
     * the languages this site can work with, ordered from most preferred languages, to less preferred
     */
    private LinkedHashMap<String, Locale> cachedLanguages;
    private Locale cachedDefaultLanguage;
    private URI cachedSiteDomain;
    private URI[] cachedSiteAliases;
    private URI defaultRdfSchema;
    private URI cachedPagesStorePath;
    private URI cachedPagesViewPath;
    private File cachedPagesStoreJournalDir;
    protected HashMap<String, String> cachedHdfsProperties = null;
    protected HashMap<String, String> cachedEsProperties = null;
    protected Object txManagerLock = new Object();
    private File cachedPagesMainIndexDir;
    private File cachedPagesTripleStoreDir;
    private Object rdfDatasetLock = new Object();

    private Settings()
    {
    }

    public static Settings instance()
    {
        if (Settings.instance == null) {
            Settings.instance = new Settings();
        }
        return Settings.instance;
    }

    public URI getSiteDomain()
    {
        if (this.cachedSiteDomain == null) {
            String schema = R.configuration().getString("blocks.core.domain.main");
            try {
                this.cachedSiteDomain = URI.create(schema);
            }
            catch (Exception e) {
                throw new RuntimeException("Site main domain in blocks config is not valid. This setting is vital, can't proceed.", e);
            }
        }

        return this.cachedSiteDomain;
    }
    /**
     * The aliases for the domain above (eg. like Apache ServerName vs. ServerAlias settings)
     * @return
     */
    public URI[] getSiteAliases()
    {
        if (this.cachedSiteAliases == null) {
            try {
                String[] aliases = R.configuration().getStringArray("blocks.core.domain.alias");
                if (aliases!=null && aliases.length>0) {
                    List<URI> tmpList = new ArrayList<>();
                    for (int i=0;i<aliases.length;i++) {
                        if (!StringUtils.isEmpty(aliases[i])) {
                            tmpList.add(URI.create(aliases[i]));
                        }
                    }
                    this.cachedSiteAliases = tmpList.toArray(new URI[tmpList.size()]);
                }
                else {
                    this.cachedSiteAliases = new URI[0];
                }
            }
            catch (Exception e) {
                Logger.error("Error while getting site domain alias in blocks config. Proceeding without aliases", e);
                this.cachedSiteAliases = new URI[0];
            }
        }

        return this.cachedSiteAliases;
    }
    /**
     * @return The languages this site can work with, ordered from most preferred getLanguage, to less preferred. If no such languages are specified in the configuration xml, an array with a default getLanguage is returned.
     */
    public Map<String, Locale> getLanguages()
    {
        if (cachedLanguages == null) {
            cachedLanguages = new LinkedHashMap<>();
            ArrayList<String> cachedLanguagesTemp = new ArrayList<String>(Arrays.asList(R.configuration().getStringArray("blocks.core.languages")));

            for (String l : cachedLanguagesTemp) {

                Locale locale = new Locale(l);
                if (this.cachedDefaultLanguage == null)
                    this.cachedDefaultLanguage = locale;
                //                String getLanguage = locale;
                cachedLanguages.put(locale.getLanguage(), locale);
            }
            if (cachedLanguages.size() == 0) {
                this.cachedDefaultLanguage = Locale.ENGLISH;
                cachedLanguages.put(cachedDefaultLanguage.getLanguage(), cachedDefaultLanguage);
            }
        }
        return cachedLanguages;
    }
    /**
     * @return The first languages in the languages-list, or the no-getLanguage-constant if no such list is present in the configuration-xml.
     */
    public Locale getDefaultLanguage()
    {
        if (cachedDefaultLanguage == null) {
            this.getLanguages();
        }
        return cachedDefaultLanguage;
    }
    public Locale getLocaleForLanguage(String language)
    {
        Locale retVal = this.getDefaultLanguage();
        if (this.cachedLanguages.containsKey(language)) {
            retVal = this.cachedLanguages.get(language);
        }
        return retVal;
    }
    public URI getPagesStorePath()
    {
        if (this.cachedPagesStorePath == null) {
            String path = R.configuration().getString("blocks.core.pages.store-path");
            //this is used to resolve files on, so make sure it's always a formal directory
            if (!path.endsWith("/")) {
                path += "/";
            }
            this.cachedPagesStorePath = URI.create(path);

            if (StringUtils.isEmpty(this.cachedPagesStorePath.getScheme())) {
                //make sure we have a schema
                this.cachedPagesStorePath = URI.create(DEFAULT_TX_FILESYSTEM_SCHEME + "://" + this.cachedPagesStorePath.toString());
                Logger.warn("The page store path doesn't have a schema, adding the HDFS '" + DEFAULT_TX_FILESYSTEM_SCHEME + "://' prefix to use the local transactional file system; " +
                            this.cachedPagesStorePath.toString());
            }
        }

        return this.cachedPagesStorePath;
    }
    public URI getPagesViewPath()
    {
        if (this.cachedPagesViewPath == null) {
            String path = R.configuration().getString("blocks.core.pages.view-path", null);
            if (path == null) {
                this.cachedPagesViewPath = URI.create(DEFAULT_PAGES_VIEW_FS_SCHEME + ":" + this.getPagesStorePath().getSchemeSpecificPart());
                Logger.info("No pages view store path configured, trying to build a local view path based on the pages store path; " + this.cachedPagesViewPath);
            }
            else {
                //this is used to resolve files on, so make sure it's always a formal directory
                if (!path.endsWith("/")) {
                    path += "/";
                }
                this.cachedPagesViewPath = URI.create(path);
            }

            if (StringUtils.isEmpty(this.cachedPagesViewPath.getScheme())) {
                //make sure we have a schema
                this.cachedPagesViewPath = URI.create(DEFAULT_PAGES_VIEW_FS_SCHEME + "://" + this.cachedPagesViewPath.toString());
                Logger.warn("The page store path doesn't have a schema, adding the HDFS '" + DEFAULT_PAGES_VIEW_FS_SCHEME + "://' prefix to use the local file system; " + this.cachedPagesViewPath.toString());
            }
        }

        return this.cachedPagesViewPath;
    }
    public File getPagesStoreJournalDir()
    {
        if (this.cachedPagesStoreJournalDir == null) {
            //Note: the journal dir resides on the local, naked file system, watch out you don't point to a dir in the distributed or transactional fs
            this.cachedPagesStoreJournalDir = new File(R.configuration().getString("blocks.core.pages.journal-dir"));
        }

        return this.cachedPagesStoreJournalDir;
    }
    public String getPagesStoreJournalId()
    {
        return R.configuration().getString("blocks.core.pages.journal-id", DEFAULT_XADISK_INSTANCE_ID);
    }
    public long getPagesStoreJournalBootTimeout()
    {
        return R.configuration().getLong("blocks.core.pages.journal-boot-timeout", DEFAULT_XADISK_BOOT_TIMEOUT);
    }
    public Map<String, String> getPagesHdfsProperties()
    {
        if (this.cachedHdfsProperties == null) {
            this.cachedHdfsProperties = new HashMap<String, String>();
            List<HierarchicalConfiguration> properties = R.configuration().configurationsAt(PAGES_HDFS_PROPERTIES_KEY);
            for (HierarchicalConfiguration property : properties) {
                String propertyKey = property.getString("name");
                String propertyValue = property.getString("value");
                this.cachedHdfsProperties.put(propertyKey, propertyValue);
            }
        }

        return this.cachedHdfsProperties;
    }
    public String getPagesFileExtension()
    {
        return R.configuration().getString("blocks.core.pages.file-ext", DEFAULT_FILE_EXT);
    }
    public String getPagesLockFileExtension()
    {
        return R.configuration().getString("blocks.core.pages.lock-file-ext", DEFAULT_LOCK_FILE_EXT);
    }
    public boolean hasElasticSearchConfigured()
    {
        return !StringUtils.isEmpty(Settings.instance().getElasticSearchClusterName());
    }
    public boolean getElasticSearchLaunchEmbedded()
    {
        return R.configuration().getBoolean("blocks.core.elastic-search.launch-embedded", true);
    }
    public String getElasticSearchClusterName()
    {
        return R.configuration().getString("blocks.core.elastic-search.cluster-name");
    }
    public String getElasticSearchHostName()
    {
        return R.configuration().getString("blocks.core.elastic-search.host", "localhost");
    }
    public Integer getElasticSearchPort()
    {
        return R.configuration().getInt("blocks.core.elastic-search.port", 9000);
    }
    public Map<String, String> getElasticSearchProperties()
    {
        if (this.cachedEsProperties == null) {
            this.cachedEsProperties = new HashMap<>();
            List<HierarchicalConfiguration> properties = R.configuration().configurationsAt(ELASTIC_SEARCH_PROPERTIES_KEY);
            for (HierarchicalConfiguration property : properties) {
                String propertyKey = property.getString("name");
                String propertyValue = property.getString("value");
                this.cachedEsProperties.put(propertyKey, propertyValue);
            }
        }

        return this.cachedEsProperties;
    }
    /**
     * @return this returns a NEW filesystem, that needs to be (auto) closed
     */
    public FileContext getPageStoreFileSystem() throws IOException
    {
        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.HDFS_PAGESTORE_FS_CONFIG)) {
            Configuration conf = new Configuration();
            URI pageStorePath = this.getPagesStorePath();
            if (StringUtils.isEmpty(pageStorePath.getScheme())) {
                //make sure we have a schema
                pageStorePath = URI.create(DEFAULT_TX_FILESYSTEM_SCHEME + "://" + pageStorePath.toString());
                Logger.warn("The page store path doesn't have a schema, adding the HDFS " + DEFAULT_TX_FILESYSTEM_SCHEME + "'://' prefix to use the local transactional file system; " +
                            pageStorePath.toString());
            }
            conf.set(CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY, pageStorePath.toString());

            if (pageStorePath.getScheme().equals(DEFAULT_TX_FILESYSTEM_SCHEME)) {
                // don't forget to register our custom FS so it can be found by HDFS
                // Note: below we have a chance to override this again with the conf
                conf.set("fs.AbstractFileSystem." + DEFAULT_TX_FILESYSTEM_SCHEME + ".impl", DEFAULT_TX_FILESYSTEM.getCanonicalName());
            }

            //note: if fs.defaultFS is set here, this might overwrite the path above
            Map<String, String> extraProperties = this.getPagesHdfsProperties();
            if (extraProperties != null) {
                for (Map.Entry<String, String> entry : extraProperties.entrySet()) {
                    if (entry.getKey().equals(CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY)) {
                        Logger.warn("Watch out, your HDFS settings overwrite the pages store path; " + entry.getValue());
                    }
                    conf.set(entry.getKey(), entry.getValue());
                }
            }

            R.cacheManager().getApplicationCache().put(CacheKeys.HDFS_PAGESTORE_FS_CONFIG, conf);

            //boot the XADisk instance too (probably still null here, good place to test them together)
            this.getPageStoreTransactionManager();
        }

        return FileContext.getFileContext((Configuration) R.cacheManager().getApplicationCache().get(CacheKeys.HDFS_PAGESTORE_FS_CONFIG));
    }
    /**
     * @return this returns a NEW filesystem, that needs to be (auto) closed
     */
    public FileContext getPageViewFileSystem() throws IOException
    {
        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.HDFS_PAGEVIEW_FS_CONFIG)) {
            Configuration conf = new Configuration();
            URI pageViewPath = this.getPagesViewPath();
            if (StringUtils.isEmpty(pageViewPath.getScheme())) {
                //make sure we have a schema
                pageViewPath = URI.create(DEFAULT_PAGES_VIEW_FS_SCHEME + "://" + pageViewPath.toString());
                Logger.warn("The page store path doesn't have a schema, adding the HDFS " + DEFAULT_PAGES_VIEW_FS_SCHEME + "'://' prefix to use the local file system; " +
                            pageViewPath.toString());
            }
            conf.set(CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY, pageViewPath.toString());

            if (pageViewPath.getScheme().equals(DEFAULT_PAGES_VIEW_FS_SCHEME)) {
                // don't forget to register our custom FS so it can be found by HDFS
                // Note: below we have a chance to override this again with the conf
                conf.set("fs.AbstractFileSystem." + DEFAULT_PAGES_VIEW_FS_SCHEME + ".impl", DEFAULT_PAGES_VIEW_FS.getCanonicalName());
            }

            //note: if fs.defaultFS is set here, this might overwrite the path above
            //Hmm, maybe this should be splitted in store/view properties, but let's keep it like this, for now
            Map<String, String> extraProperties = this.getPagesHdfsProperties();
            if (extraProperties != null) {
                for (Map.Entry<String, String> entry : extraProperties.entrySet()) {
                    if (entry.getKey().equals(CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY)) {
                        Logger.warn("Watch out, your HDFS settings overwrite the pages view path; " + entry.getValue());
                    }
                    conf.set(entry.getKey(), entry.getValue());
                }
            }

            R.cacheManager().getApplicationCache().put(CacheKeys.HDFS_PAGEVIEW_FS_CONFIG, conf);
        }

        return FileContext.getFileContext((Configuration) R.cacheManager().getApplicationCache().get(CacheKeys.HDFS_PAGEVIEW_FS_CONFIG));
    }
    public XAFileSystem getPageStoreTransactionManager() throws IOException
    {
        synchronized (this.txManagerLock) {
            if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.XADISK_FILE_SYSTEM)) {
                XAFileSystem xafs = XAFileSystemProxy.bootNativeXAFileSystem(new StandaloneFileSystemConfiguration(this.getPagesStoreJournalDir().getAbsolutePath(), this.getPagesStoreJournalId()));
                try {
                    xafs.waitForBootup(this.getPagesStoreJournalBootTimeout());
                    R.cacheManager().getApplicationCache().put(CacheKeys.XADISK_FILE_SYSTEM, xafs);
                }
                catch (InterruptedException e) {
                    throw new IOException("Error occurred whlie booting transactional XADisk file system (timeout=" + this.getPagesStoreJournalBootTimeout(), e);
                }
            }

            return (XAFileSystem) R.cacheManager().getApplicationCache().get(CacheKeys.XADISK_FILE_SYSTEM);
        }
    }
    public boolean rebootPageStoreTransactionManager()
    {
        synchronized (this.txManagerLock) {
            boolean retVal = false;

            if (R.cacheManager().getApplicationCache().containsKey(CacheKeys.XADISK_FILE_SYSTEM)) {
                XAFileSystem xafs = (XAFileSystem) R.cacheManager().getApplicationCache().get(CacheKeys.XADISK_FILE_SYSTEM);
                //setting it here will ensure it's null internally, even if the next shutdown fails
                R.cacheManager().getApplicationCache().remove(CacheKeys.XADISK_FILE_SYSTEM);
                try {
                    xafs.shutdown();

                    //uniform reboot
                    this.getPageStoreTransactionManager();

                    retVal = true;
                }
                catch (IOException e) {
                    Logger.error("Exception caught while rebooting a transactional XADisk file system", e);
                }
            }

            return retVal;
        }
    }
    public File getPageMainIndexFolder()
    {
        if (this.cachedPagesMainIndexDir == null) {
            //Note: the journal dir resides on the local, naked file system, watch out you don't point to a dir in the distributed or transactional fs
            this.cachedPagesMainIndexDir = new File(R.configuration().getString("blocks.core.pages.main-index.dir"));
        }

        return this.cachedPagesMainIndexDir;
    }
    public File getPageTripleStoreFolder()
    {
        if (this.cachedPagesTripleStoreDir == null) {
            //Note: the journal dir resides on the local, naked file system, watch out you don't point to a dir in the distributed or transactional fs
            this.cachedPagesTripleStoreDir = new File(R.configuration().getString("blocks.core.pages.triple-store.dir"));
        }

        return this.cachedPagesTripleStoreDir;
    }
    public Dataset getRDFDataset()
    {
        synchronized (this.rdfDatasetLock) {
            if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.RDF_DATASET)) {
                Dataset dataset = TDBFactory.createDataset(Settings.instance().getPageTripleStoreFolder().getAbsolutePath());
                R.cacheManager().getApplicationCache().put(CacheKeys.RDF_DATASET, dataset);
            }

            return (Dataset) R.cacheManager().getApplicationCache().get(CacheKeys.RDF_DATASET);
        }
    }

    //TODO revise these below:
    public URI getDefaultRdfSchema()
    {
        if (this.defaultRdfSchema == null) {
            String schema = R.configuration().getString("blocks.core.rdf.schema.url");
            try {
                this.defaultRdfSchema = URI.create(schema);
            }
            catch (Exception e) {
                throw new RuntimeException("Wrong default RDF blocks.core.rdf.schema.url configured; " + schema, e);
            }
        }
        return this.defaultRdfSchema;
    }
    public String getDefaultRdfPrefix()
    {
        return R.configuration().getString("blocks.core.rdf.schema.prefix");
    }
}
