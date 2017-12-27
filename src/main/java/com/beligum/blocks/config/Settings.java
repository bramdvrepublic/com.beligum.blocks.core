/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.beligum.blocks.config;

import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.base.utils.toolkit.ReflectionFunctions;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.filesystem.hdfs.HdfsImplDef;
import com.beligum.blocks.filesystem.hdfs.impl.FileSystems;
import com.beligum.blocks.filesystem.hdfs.xattr.XAttrMapper;
import com.beligum.blocks.filesystem.hdfs.xattr.XAttrResolverFactory;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.lang3.StringUtils;

import javax.transaction.TransactionManager;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by bas on 08.10.14.
 */
public class Settings
{
    public static final String RESOURCE_ENDPOINT = "/resource/";

    private static final String TRANSACTIONS_PROPERTIES_KEY = "blocks.core.transactions.properties.property";
    private static final String TRANSACTION_MANAGER_KEY = "blocks.core.transactions.transaction-manager";
    private static final String TRANSACTION_TIMEOUT_KEY = "blocks.core.transactions.timeout";
    //one minute; this is the same values as the default of bitronix.tm.timer.defaultTransactionTimeout
    private static final int DEFAULT_TRANSACTION_TIMEOUT_MILLIS = 60 * 1000;//1 minute
    private static final Map<String, String> DEFAULT_TRANSACTION_MANAGER_PROPS = ImmutableMap.<String, String>builder().build();

    private static final String CONTEXT_DEFAULT_PAGES_DIR = "pages";
    //this constant wil be used from the blocks-media module, but is defined here to group the subdirs together a little bit...
    public static final String CONTEXT_DEFAULT_MEDIA_DIR = "media";

    private static final String PAGES_HDFS_PROPERTIES_KEY = "blocks.core.pages.hdfs.properties.property";
    private static final String PAGES_DEFAULT_FILE_EXT = ".html";
    private static final String PAGES_DEFAULT_LOCK_FILE_EXT = ".lock";
    private static final String PAGES_DEFAULT_INDEX_FOLDER = "index";
    private static final String PAGES_DEFAULT_JOURNAL_FOLDER = "journal";
    private static final String PAGES_DEFAULT_TRIPLESTORE_FOLDER = "triplestore";
    public static final String PAGES_DEFAULT_TRANSACTIONS_FOLDER = "tx";
    private static final String PAGES_DEFAULT_DATA_FOLDER = "data";

    private static final String DEFAULT_XADISK_INSTANCE_ID = "xa-1";
    private static final long DEFAULT_XADISK_BOOT_TIMEOUT = 60 * 1000; //1 minute

    private static Settings instance;
    private Boolean cachedDeleteLocksOnStartup;
    private URI cachedRdfOntologyUri;
    private boolean triedRdfOntologyUri;
    private URI cachedPagesStorePath;
    private URI cachedPagesViewPath;
    private URI cachedPagesStoreJournalDir;
    private URI cachedPagesMainIndexDir;
    private URI cachedPagesTripleStoreDir;
    private Class<? extends TransactionManager> cachedTransactionManagerClass;
    protected HashMap<String, String> cachedTransactionsProperties = null;
    protected HashMap<String, String> cachedHdfsProperties = null;
    private String cachedPagesFileExtension;
    private String cachedPagesLockFileExtension;

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

    public boolean hasBlocksConfig()
    {
        return R.configuration().getMaxIndex("blocks") >= 0;
    }
    public boolean hasBlocksCoreConfig()
    {
        return R.configuration().getMaxIndex("blocks.core") >= 0;
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
    public Class<? extends TransactionManager> getTransactionManagerClass()
    {
        if (this.cachedTransactionManagerClass == null) {
            String classname = R.configuration().getString(TRANSACTION_MANAGER_KEY, null);
            try {
                this.cachedTransactionManagerClass = (Class<? extends TransactionManager>) Class.forName(classname);
            }
            catch (ClassNotFoundException e) {
                throw new RuntimeException("Unable to instantiate configured transaction manager for class " + classname, e);
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

            //Don't default values that have been set explicitly
            for (Map.Entry<String, String> e : DEFAULT_TRANSACTION_MANAGER_PROPS.entrySet()) {
                if (!this.cachedTransactionsProperties.containsKey(e.getKey())) {
                    this.cachedTransactionsProperties.put(e.getKey(), e.getValue());
                }
            }
        }

        return this.cachedTransactionsProperties;
    }
    /**
     * Returns the default timeout value in milliseconds for all transactions
     */
    public int getTransactionTimeoutMillis()
    {
        return R.configuration().getInt(TRANSACTION_TIMEOUT_KEY, DEFAULT_TRANSACTION_TIMEOUT_MILLIS);
    }
    public boolean getDeleteLocksOnStartup()
    {
        if (this.cachedDeleteLocksOnStartup == null) {
            this.cachedDeleteLocksOnStartup = R.configuration().getBoolean("blocks.core.resources.delete-locks-on-startup", true);
        }

        return this.cachedDeleteLocksOnStartup;
    }
    public XAttrResolverFactory getXAttrResolverFactory()
    {
        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.BLOCKS_XATTR_RESOLVER_FACTORY)) {
            XAttrResolverFactory xAttrResolverFactory = new XAttrResolverFactory();
            Set<Class<? extends XAttrMapper>> allMappers = ReflectionFunctions.searchAllClassesImplementing(XAttrMapper.class, true);
            for (Class<? extends XAttrMapper> m : allMappers) {
                try {
                    xAttrResolverFactory.register(m.newInstance());
                }
                catch (Exception e) {
                    Logger.error("Error while instantiating XAttrMapper class, ignoring this mapper; this shouldn't happen; " + m, e);
                }
            }

            R.cacheManager().getApplicationCache().put(CacheKeys.BLOCKS_XATTR_RESOLVER_FACTORY, xAttrResolverFactory);
        }

        return (XAttrResolverFactory) R.cacheManager().getApplicationCache().get(CacheKeys.BLOCKS_XATTR_RESOLVER_FACTORY);
    }
    public URI getPagesRootPath()
    {
        return R.configuration().getContextConfig().resolveLocalRoot(CONTEXT_DEFAULT_PAGES_DIR, true);
    }
    public String getPagesFileSystemScheme()
    {
        return R.configuration().getString("blocks.core.pages.filesystem", null);
    }
    public String getPagesReadOnlyFileSystemScheme()
    {
        return R.configuration().getString("blocks.core.pages.filesystem-ro", null);
    }
    public URI getPagesStoreUri()
    {
        if (this.cachedPagesStorePath == null) {
            String dir = R.configuration().getString("blocks.core.pages.store-path");

            if (!StringUtils.isEmpty(dir)) {
                Logger.warn("Using custom pages data dir; " + dir);

                //this is used to resolve files on, so make sure it's always a formal directory
                if (!dir.endsWith("/")) {
                    dir += "/";
                }

                this.cachedPagesStorePath = URI.create(dir);
            }
            else {
                //note that in this case we strip off the scheme because we want to trigger the condition below (to fill it will the default store scheme)
                this.cachedPagesStorePath = UriBuilder.fromUri(this.getPagesRootPath().resolve(PAGES_DEFAULT_DATA_FOLDER + "/")).scheme(null).build();
            }

            if (StringUtils.isEmpty(this.cachedPagesStorePath.getScheme())) {
                this.cachedPagesStorePath = this.buildDefaultPagesUri(this.cachedPagesStorePath, false);
                Logger.info("The pages store URI wasn't set explicitly or doesn't have a schema, auto-fixing it; " + this.cachedPagesStorePath.toString());
            }
        }

        return this.cachedPagesStorePath;
    }
    public URI getPagesViewUri()
    {
        if (this.cachedPagesViewPath == null) {
            String dir = R.configuration().getString("blocks.core.pages.view-path", null);

            if (!StringUtils.isEmpty(dir)) {
                Logger.warn("Using custom pages view path; " + dir);

                //this is used to resolve files on, so make sure it's always a formal directory
                if (!dir.endsWith("/")) {
                    dir += "/";
                }

                this.cachedPagesViewPath = URI.create(dir);
            }

            //make sure we have a schema
            if (this.cachedPagesViewPath == null || StringUtils.isEmpty(this.cachedPagesViewPath.getScheme())) {
                this.cachedPagesViewPath = this.buildDefaultPagesUri(this.getPagesStoreUri(), true);
                Logger.info("The pages view URI wasn't set explicitly or doesn't have a schema, auto-fixing it; " + this.cachedPagesViewPath.toString());
            }
        }

        return this.cachedPagesViewPath;
    }
    public URI getPagesStoreJournalDir()
    {
        if (this.cachedPagesStoreJournalDir == null) {
            String dir = R.configuration().getString("blocks.core.pages.journal-dir", null);

            //Note: the journal dir resides on the local, naked file system, watch out you don't point to a dir in the distributed or transactional fs
            if (dir != null) {
                Logger.warn("Using custom pages journal dir; " + dir);

                //this is used to resolve files on, so make sure it's always a formal directory
                if (!dir.endsWith("/")) {
                    dir += "/";
                }

                this.cachedPagesStoreJournalDir = URI.create(dir);
            }
            else {
                this.cachedPagesStoreJournalDir = this.getPagesRootPath().resolve(PAGES_DEFAULT_JOURNAL_FOLDER + "/");
            }
        }

        return this.cachedPagesStoreJournalDir;
    }
    public URI getPageMainIndexFolder()
    {
        if (this.cachedPagesMainIndexDir == null) {
            String dir = R.configuration().getString("blocks.core.pages.main-index.dir", null);

            //Note: the journal dir resides on the local, naked file system, watch out you don't point to a dir in the distributed or transactional fs
            if (dir != null) {
                Logger.warn("Using custom pages index dir; " + dir);

                //this is used to resolve files on, so make sure it's always a formal directory
                if (!dir.endsWith("/")) {
                    dir += "/";
                }

                this.cachedPagesMainIndexDir = URI.create(dir);
            }
            else {
                this.cachedPagesMainIndexDir = this.getPagesRootPath().resolve(PAGES_DEFAULT_INDEX_FOLDER + "/");
            }
        }

        return this.cachedPagesMainIndexDir;
    }
    public URI getPageTripleStoreFolder()
    {
        if (this.cachedPagesTripleStoreDir == null) {
            String dir = R.configuration().getString("blocks.core.pages.triple-store.dir", null);

            if (dir != null) {
                Logger.warn("Using custom pages triple store dir; " + dir);

                //this is used to resolve files on, so make sure it's always a formal directory
                if (!dir.endsWith("/")) {
                    dir += "/";
                }

                this.cachedPagesTripleStoreDir = URI.create(dir);
            }
            else {
                this.cachedPagesTripleStoreDir = this.getPagesRootPath().resolve(PAGES_DEFAULT_TRIPLESTORE_FOLDER + "/");
            }
        }

        return this.cachedPagesTripleStoreDir;
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
            this.cachedHdfsProperties = new HashMap<>();
            List<HierarchicalConfiguration> properties = R.configuration().configurationsAt(PAGES_HDFS_PROPERTIES_KEY);
            for (HierarchicalConfiguration property : properties) {
                this.cachedHdfsProperties.put(property.getString("name"), property.getString("value"));
            }
        }

        return this.cachedHdfsProperties;
    }
    /**
     * @return returns the file extension of the local page resources INCLUDING the dot
     */
    public String getPagesFileExtension()
    {
        if (this.cachedPagesFileExtension == null) {
            this.cachedPagesFileExtension = R.configuration().getString("blocks.core.pages.file-ext", PAGES_DEFAULT_FILE_EXT);
        }

        return this.cachedPagesFileExtension;
    }
    /**
     * @return returns the file extension of the local page resource lock file INCLUDING the dot
     */
    public String getPagesLockFileExtension()
    {
        if (this.cachedPagesLockFileExtension == null) {
            this.cachedPagesLockFileExtension = R.configuration().getString("blocks.core.pages.lock-file-ext", PAGES_DEFAULT_LOCK_FILE_EXT);
        }

        return this.cachedPagesLockFileExtension;
    }
    public URI getRdfOntologyUri()
    {
        if (!this.triedRdfOntologyUri) {
            String uri = R.configuration().getString("blocks.core.rdf.ontology.uri");
            if (!StringUtils.isEmpty(uri)) {
                try {
                    this.cachedRdfOntologyUri = URI.create(uri);
                }
                catch (Exception e) {
                    throw new RuntimeException("Error while parsing RDF ontology URI; " + uri, e);
                }
            }

            this.triedRdfOntologyUri = true;
        }
        return this.cachedRdfOntologyUri;
    }
    public String getRdfOntologyPrefix()
    {
        return R.configuration().getString("blocks.core.rdf.ontology.prefix");
    }

    //-----PRIVATE METHODS-----
    private URI buildDefaultPagesUri(URI tempUri, boolean readOnly)
    {
        //first, we try to select as good as possible, but if we don't have a specific read-only fs,
        //we try to revert to the general default (read/write) fs.
        String scheme = readOnly ? this.getPagesReadOnlyFileSystemScheme() : this.getPagesFileSystemScheme();
        if (readOnly && StringUtils.isEmpty(scheme)) {
            scheme = this.getPagesFileSystemScheme();
        }

        HdfsImplDef confFs = FileSystems.forScheme(scheme);
        if (confFs == null) {
            confFs = readOnly ? StorageFactory.DEFAULT_PAGES_VIEW_FILESYSTEM : StorageFactory.DEFAULT_PAGES_TX_FILESYSTEM;
            //same as before: if we don't have a configured default for read-only, revert to the read-write version
            if (readOnly && confFs == null) {
                confFs = StorageFactory.DEFAULT_PAGES_TX_FILESYSTEM;
            }
        }

        return confFs == null ? null : UriBuilder.fromUri(tempUri).scheme(confFs.getScheme()).build();
    }
}
