package com.beligum.blocks.config;

import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.lang3.StringUtils;

import javax.transaction.TransactionManager;
import java.io.File;
import java.net.URI;
import java.util.*;

/**
 * Created by bas on 08.10.14.
 */
public class Settings
{
    private static final String TRANSACTIONS_PROPERTIES_KEY = "blocks.core.transactions.properties.property";
    private static final String PAGES_HDFS_PROPERTIES_KEY = "blocks.core.pages.hdfs.properties.property";
    private static final String ELASTIC_SEARCH_PROPERTIES_KEY = "blocks.core.elastic-search.properties.property";

    private static final String DEFAULT_FILE_EXT = ".html";
    private static final String DEFAULT_LOCK_FILE_EXT = ".lock";

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
    private Class<? extends TransactionManager> cachedTransactionManagerClass;
    protected HashMap<String, String> cachedTransactionsProperties = null;
    private File cachedPagesStoreJournalDir;
    protected HashMap<String, String> cachedHdfsProperties = null;
    protected HashMap<String, String> cachedEsProperties = null;
    private File cachedPagesMainIndexDir;
    private File cachedPagesTripleStoreDir;

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
    /**
     * Flag that indicates if we should redirect to the default locale of the site on creating a language-less new page.
     * If true, a redirect to the /def-lang/... is forced, otherwise, the language of the browser is first tried to see
     * if it's supported by this site's languages.
     */
    public boolean getForceRedirectToDefaultLocale()
    {
        return R.configuration().getBoolean("blocks.core.pages.force-default-locale", false);
    }
    public Locale getLocaleForLanguage(String language)
    {
        Locale retVal = this.getDefaultLanguage();
        if (this.cachedLanguages.containsKey(language)) {
            retVal = this.cachedLanguages.get(language);
        }
        return retVal;
    }
    public Class<? extends TransactionManager> getTransactionManagerClass()
    {
        if (this.cachedTransactionManagerClass == null) {
            String classname = R.configuration().getString("blocks.core.transactions.transaction-manager", "com.atomikos.icatch.jta.UserTransactionManager");
            try {
                this.cachedTransactionManagerClass = (Class<? extends TransactionManager>) Class.forName(classname);
            }
            catch (ClassNotFoundException e) {
                throw new RuntimeException("Unable to instantiate configured transaction manager for class "+classname, e);
            }
        }

        return this.cachedTransactionManagerClass;
    }
    public Map<String, String> getTransactionsProperties()
    {
        if (this.cachedTransactionsProperties == null) {
            this.cachedTransactionsProperties = new HashMap<>();
            List<HierarchicalConfiguration> properties = R.configuration().configurationsAt(TRANSACTIONS_PROPERTIES_KEY);
            for (HierarchicalConfiguration property : properties) {
                String propertyKey = property.getString("name");
                String propertyValue = property.getString("value");
                this.cachedTransactionsProperties.put(propertyKey, propertyValue);
            }
        }

        return this.cachedTransactionsProperties;
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
                this.cachedPagesStorePath = URI.create(StorageFactory.DEFAULT_TX_FILESYSTEM_SCHEME + "://" + this.cachedPagesStorePath.toString());
                Logger.warn("The page store path doesn't have a schema, adding the HDFS '" + StorageFactory.DEFAULT_TX_FILESYSTEM_SCHEME + "://' prefix to use the local transactional file system; " +
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
                this.cachedPagesViewPath = URI.create(StorageFactory.DEFAULT_PAGES_VIEW_FS_SCHEME + ":" + this.getPagesStorePath().getSchemeSpecificPart());
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
                this.cachedPagesViewPath = URI.create(StorageFactory.DEFAULT_PAGES_VIEW_FS_SCHEME + "://" + this.cachedPagesViewPath.toString());
                Logger.warn("The page store path doesn't have a schema, adding the HDFS '" + StorageFactory.DEFAULT_PAGES_VIEW_FS_SCHEME + "://' prefix to use the local file system; " + this.cachedPagesViewPath.toString());
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
    /**
     * @return returns the file extension of the local page resources INCLUDING the dot
     */
    public String getPagesFileExtension()
    {
        return R.configuration().getString("blocks.core.pages.file-ext", DEFAULT_FILE_EXT);
    }
    /**
     * @return returns the file extension of the local page resource lock file INCLUDING the dot
     */
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
