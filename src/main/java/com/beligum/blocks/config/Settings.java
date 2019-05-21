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

import com.beligum.base.filesystem.MessagesFileEntry;
import com.beligum.base.security.PermissionRole;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.base.utils.json.Json;
import com.beligum.base.utils.toolkit.ReflectionFunctions;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.filesystem.hdfs.HdfsImplDef;
import com.beligum.blocks.filesystem.hdfs.impl.FileSystems;
import com.beligum.blocks.filesystem.hdfs.xattr.XAttrMapper;
import com.beligum.blocks.filesystem.hdfs.xattr.XAttrResolverFactory;
import com.beligum.blocks.rdf.RdfFactory;
import com.beligum.blocks.rdf.RdfNamespaceImpl;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfNamespace;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ifaces.RdfResource;
import com.beligum.blocks.rdf.ontologies.Main;
import com.beligum.blocks.rdf.ontologies.RDFS;
import com.beligum.blocks.security.AclImpl;
import com.beligum.blocks.security.ifaces.Acl;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.lang3.StringUtils;

import javax.transaction.TransactionManager;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.*;

/**
 * Created by bas on 08.10.14.
 */
public class Settings
{
    //sync this with the reserved page keywords
    public static final String RESOURCE_ENDPOINT = "/resource/";
    public static final String DEFAULT_MAIN_ONTOLOGY_ENDPOINT = "/ontology/";
    public static final String DEFAULT_MAIN_ONTOLOGY_PREFIX = "local";
    public static final String DEFAULT_META_ONTOLOGY_ENDPOINT = DEFAULT_MAIN_ONTOLOGY_ENDPOINT + "meta/";
    public static final String DEFAULT_META_ONTOLOGY_PREFIX = "meta";
    public static final String DEFAULT_LOG_ONTOLOGY_ENDPOINT = DEFAULT_MAIN_ONTOLOGY_ENDPOINT + "log/";
    public static final String DEFAULT_LOG_ONTOLOGY_PREFIX = "log";
    public static final String DEFAULT_BLOCKS_ONTOLOGY_ENDPOINT = DEFAULT_MAIN_ONTOLOGY_ENDPOINT + "blocks/";
    public static final String DEFAULT_BLOCKS_ONTOLOGY_PREFIX = "blocks";

    //centralized constant for the default entity type when nothing is set
    public static final RdfClass DEFAULT_CLASS = Main.Page;

    private static final String COMMON_PREFIX = "blocks.core";
    private static final String PAGES_PREFIX = COMMON_PREFIX + ".pages";
    private static final String RDF_PREFIX = COMMON_PREFIX + ".rdf";
    private static final String RDF_ONTOLOGIES_KEY = RDF_PREFIX + ".ontologies.ontology";
    private static final String RDF_MAIN_ONTOLOGY_NAME = "main";
    private static final String RDF_META_ONTOLOGY_NAME = "meta";
    private static final String RDF_LOG_ONTOLOGY_NAME = "log";
    private static final String RDF_BLOCKS_ONTOLOGY_NAME = "blocks";

    private static final String SECURITY_PREFIX = COMMON_PREFIX + ".security";
    private static final String SECURITY_ACLS_PREFIX = SECURITY_PREFIX + ".acls";

    private static final String TRANSACTIONS_PROPERTIES_KEY = COMMON_PREFIX + ".transactions.properties.property";
    private static final String TRANSACTION_MANAGER_KEY = COMMON_PREFIX + ".transactions.transaction-manager";
    private static final String TRANSACTION_TIMEOUT_KEY = COMMON_PREFIX + ".transactions.timeout";

    //one minute; this is the same values as the default of bitronix.tm.timer.defaultTransactionTimeout
    private static final int DEFAULT_TRANSACTION_TIMEOUT_MILLIS = 60 * 1000;//1 minute
    private static final Map<String, String> DEFAULT_TRANSACTION_MANAGER_PROPS = ImmutableMap.<String, String>builder().build();

    private static final String CONTEXT_DEFAULT_PAGES_DIR = "pages";
    //this constant wil be used from the blocks-media module, but is defined here to group the subdirs together a little bit...
    public static final String CONTEXT_DEFAULT_MEDIA_DIR = "media";

    private static final String PAGES_HDFS_PROPERTIES_KEY = PAGES_PREFIX + ".hdfs.properties.property";
    private static final String PAGES_DEFAULT_FILE_EXT = ".html";
    private static final String PAGES_DEFAULT_LOCK_FILE_EXT = ".lock";
    private static final String PAGES_DEFAULT_INDEX_FOLDER = "index";
    private static final String PAGES_DEFAULT_JOURNAL_FOLDER = "journal";
    private static final String PAGES_DEFAULT_TRIPLESTORE_FOLDER = "triplestore";
    public static final String PAGES_DEFAULT_TRANSACTIONS_FOLDER = "tx";
    private static final String PAGES_DEFAULT_DATA_FOLDER = "data";

    private static final String SECURITY_ACLS_KEY = SECURITY_ACLS_PREFIX + ".acl";

    private static final String DEFAULT_XADISK_INSTANCE_ID = "xa-1";
    private static final long DEFAULT_XADISK_BOOT_TIMEOUT = 60 * 1000; //1 minute

    private static Settings instance;
    private Boolean cachedDeleteLocksOnStartup;
    private RdfNamespace cachedRdfMainOntologyNamespace;
    private RdfNamespace cachedRdfMetaOntologyNamespace;
    private RdfNamespace cachedRdfLogOntologyNamespace;
    private RdfNamespace cachedRdfBlocksOntologyNamespace;
    private RdfProperty cachedRdfLabelProperty;
    private URI cachedPagesStorePath;
    private URI cachedPagesViewPath;
    private URI cachedPagesStoreJournalDir;
    private URI cachedPagesMainIndexDir;
    private URI cachedPagesTripleStoreDir;
    private Class<? extends TransactionManager> cachedTransactionManagerClass;
    private HashMap<String, String> cachedTransactionsProperties = null;
    private HashMap<String, String> cachedHdfsProperties = null;
    private String cachedPagesFileExtension;
    private String cachedPagesLockFileExtension;
    private Map<Integer, Acl> cachedAcls;
    private String cachedAclsJson;
    private Map<String, RdfNamespace> cachedRdfOntologiesMapping;

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
        return R.configuration().getMaxIndex(COMMON_PREFIX) >= 0;
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
            this.cachedDeleteLocksOnStartup = R.configuration().getBoolean(COMMON_PREFIX + ".resources.delete-locks-on-startup", true);
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

        return R.cacheManager().getApplicationCache().get(CacheKeys.BLOCKS_XATTR_RESOLVER_FACTORY);
    }
    public URI getPagesRootPath()
    {
        return R.configuration().getContextConfig().resolveLocalRoot(CONTEXT_DEFAULT_PAGES_DIR, true);
    }
    public String getPagesFileSystemScheme()
    {
        return R.configuration().getString(PAGES_PREFIX + ".filesystem", null);
    }
    public String getPagesReadOnlyFileSystemScheme()
    {
        return R.configuration().getString(PAGES_PREFIX + ".filesystem-ro", null);
    }
    public URI getPagesStoreUri()
    {
        if (this.cachedPagesStorePath == null) {
            String dir = R.configuration().getString(PAGES_PREFIX + ".store-path");

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
            String dir = R.configuration().getString(PAGES_PREFIX + ".view-path", null);

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
            String dir = R.configuration().getString(PAGES_PREFIX + ".journal-dir", null);

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
            String dir = R.configuration().getString(PAGES_PREFIX + ".main-index.dir", null);

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
            String dir = R.configuration().getString(PAGES_PREFIX + ".triple-store.dir", null);

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
        return R.configuration().getString(PAGES_PREFIX + ".journal-id", DEFAULT_XADISK_INSTANCE_ID);
    }
    public long getPagesStoreJournalBootTimeout()
    {
        return R.configuration().getLong(PAGES_PREFIX + ".journal-boot-timeout", DEFAULT_XADISK_BOOT_TIMEOUT);
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
            this.cachedPagesFileExtension = R.configuration().getString(PAGES_PREFIX + ".file-ext", PAGES_DEFAULT_FILE_EXT);
        }

        return this.cachedPagesFileExtension;
    }
    /**
     * @return returns the file extension of the local page resource lock file INCLUDING the dot
     */
    public String getPagesLockFileExtension()
    {
        if (this.cachedPagesLockFileExtension == null) {
            this.cachedPagesLockFileExtension = R.configuration().getString(PAGES_PREFIX + ".lock-file-ext", PAGES_DEFAULT_LOCK_FILE_EXT);
        }

        return this.cachedPagesLockFileExtension;
    }
    /**
     * Flag that indicates if we should redirect to the default locale of the site on creating a language-less new page.
     * If true, a redirect to the /def-lang/... is forced, otherwise, the language of the browser is first tried to see
     * if it's supported by this site's languages.
     */
    public boolean getForceRedirectToDefaultLocale()
    {
        return R.configuration().getBoolean(PAGES_PREFIX + ".force-default-locale", false);
    }
    public boolean getEnablePageMetadataCaching()
    {
        return R.configuration().getBoolean(PAGES_PREFIX + ".enable-metadata-caching", true);
    }
    public boolean getEnablePageLeaveEditConfirmation()
    {
        return R.configuration().getBoolean(PAGES_PREFIX + ".enable-leave-edit-confirmation", false);
    }
    public RdfNamespace getRdfMainOntologyNamespace()
    {
        if (this.cachedRdfMainOntologyNamespace == null) {
            this.cachedRdfMainOntologyNamespace = this.initOntology(RDF_MAIN_ONTOLOGY_NAME,
                                                                    R.configuration().getSiteDomain().resolve(DEFAULT_MAIN_ONTOLOGY_ENDPOINT),
                                                                    DEFAULT_MAIN_ONTOLOGY_PREFIX);
        }

        return this.cachedRdfMainOntologyNamespace;
    }
    public RdfNamespace getRdfMetaOntologyNamespace()
    {
        if (this.cachedRdfMetaOntologyNamespace == null) {
            this.cachedRdfMetaOntologyNamespace = this.initOntology(RDF_META_ONTOLOGY_NAME,
                                                                    R.configuration().getSiteDomain().resolve(DEFAULT_META_ONTOLOGY_ENDPOINT),
                                                                    DEFAULT_META_ONTOLOGY_PREFIX);
        }

        return this.cachedRdfMetaOntologyNamespace;
    }
    public RdfNamespace getRdfLogOntologyNamespace()
    {
        if (this.cachedRdfLogOntologyNamespace == null) {
            this.cachedRdfLogOntologyNamespace = this.initOntology(RDF_LOG_ONTOLOGY_NAME,
                                                                   R.configuration().getSiteDomain().resolve(DEFAULT_LOG_ONTOLOGY_ENDPOINT),
                                                                   DEFAULT_LOG_ONTOLOGY_PREFIX);
        }

        return this.cachedRdfLogOntologyNamespace;
    }
    public RdfNamespace getRdfBlocksOntologyNamespace()
    {
        if (this.cachedRdfBlocksOntologyNamespace == null) {
            this.cachedRdfBlocksOntologyNamespace = this.initOntology(RDF_BLOCKS_ONTOLOGY_NAME,
                                                                      R.configuration().getSiteDomain().resolve(DEFAULT_BLOCKS_ONTOLOGY_ENDPOINT),
                                                                      DEFAULT_BLOCKS_ONTOLOGY_PREFIX);
        }

        return this.cachedRdfBlocksOntologyNamespace;
    }
    public RdfProperty getRdfLabelProperty()
    {
        if (this.cachedRdfLabelProperty == null) {
            String propertyName = R.configuration().getString(RDF_PREFIX + ".label-property", null);
            if (propertyName != null) {
                try {
                    RdfResource property = RdfFactory.lookup(propertyName);
                    if (property instanceof RdfProperty) {
                        this.cachedRdfLabelProperty = (RdfProperty) property;
                    }
                    else {
                        Logger.error("A default RDF label property was configured, but it doesn't seem to be a property; " + propertyName);
                    }
                }
                catch (Exception e) {
                    Logger.error("Error happened while looking up the default RDF label property, " + propertyName, e);
                }
            }

            if (this.cachedRdfLabelProperty == null) {
                this.cachedRdfLabelProperty = RDFS.label;
            }
        }

        return this.cachedRdfLabelProperty;
    }
    /**
     * When "rdf create sync" is active, we sync the offered list of blocks (when creating a new block)
     * to the rdf ontology of the (current) page type.
     * Note that the default class is an exception: we allow it to have all blocks.
     */
    public boolean getEnableRdfCreateSync()
    {
        return R.configuration().getBoolean(RDF_PREFIX + ".enable-create-sync", true);
    }
    /**
     * If strict mode is active, no blocks that only consist of non-rdf properties can be added to pages that are not of default type.
     * Note that this is an extra flag on top of "rdf create sync" mode and is only checked
     * when that flag is active.
     */
    public boolean getEnableRdfCreateSyncStrict()
    {
        return R.configuration().getBoolean(RDF_PREFIX + ".enable-create-sync-strict", false);
    }
    public boolean getEnableRdfValidation()
    {
        return R.configuration().getBoolean(RDF_PREFIX + ".enable-validation", true);
    }
    public boolean getEnableRestrictedDefaultRead()
    {
        return R.configuration().getBoolean(SECURITY_ACLS_PREFIX + ".enable-restricted-default-read", false);
    }
    public Integer getDefaultLevelRead()
    {
        return R.configuration().getInteger(SECURITY_ACLS_PREFIX + ".default-level-read", null);
    }
    public Integer getDefaultLevelUpdate()
    {
        return R.configuration().getInteger(SECURITY_ACLS_PREFIX + ".default-level-update", null);
    }
    public Integer getDefaultLevelDelete()
    {
        return R.configuration().getInteger(SECURITY_ACLS_PREFIX + ".default-level-delete", null);
    }
    public Integer getDefaultLevelManage()
    {
        return R.configuration().getInteger(SECURITY_ACLS_PREFIX + ".default-level-manage", null);
    }
    public boolean getDisableAcls()
    {
        return R.configuration().getBoolean(SECURITY_ACLS_PREFIX + ".disable", false);
    }
    public Map<Integer, Acl> getAcls()
    {
        if (this.cachedAcls == null) {
            this.cachedAcls = new LinkedHashMap<>();

            List<HierarchicalConfiguration> acls = R.configuration().configurationsAt(SECURITY_ACLS_KEY);
            for (HierarchicalConfiguration aclConfig : acls) {
                Integer level = aclConfig.getInteger("level", null);
                String labelStr = aclConfig.getString("label", null);

                if (level != null && !StringUtils.isEmpty(labelStr)) {
                    MessagesFileEntry labelMsg = R.resourceManager().getTemplateEngine().resolvePropertyKey(labelStr);
                    Acl acl = labelMsg == null ? new AclImpl(level, labelStr) : new AclImpl(level, labelMsg);
                    this.cachedAcls.put(level, acl);
                }
                else {
                    throw new RuntimeException("Encountered an ACL without a level or label; this is not allowed");
                }
            }

            //if no explicit ACLs are configured, we'll sync them with the security roles
            if (this.cachedAcls.isEmpty()) {
                for (PermissionRole r : R.configuration().getSecurityConfig().getRoles(true)) {
                    this.cachedAcls.put(r.getLevel(), new AclImpl(r));
                }
            }
        }

        return this.cachedAcls;
    }
    public String getAclsJson()
    {
        if (this.cachedAclsJson == null) {
            this.cachedAclsJson = Json.getSettingsMap(this.getAcls().values());
        }

        return this.cachedAclsJson;
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
    private Map<String, RdfNamespace> getOntologyNamespaces()
    {
        if (this.cachedRdfOntologiesMapping == null) {
            this.cachedRdfOntologiesMapping = new LinkedHashMap<>();
            List<HierarchicalConfiguration> ontologies = R.configuration().configurationsAt(RDF_ONTOLOGIES_KEY);
            for (HierarchicalConfiguration ontology : ontologies) {
                this.cachedRdfOntologiesMapping.put(ontology.getString("name"),
                                                    new RdfNamespaceImpl(ontology.getString("uri"),
                                                                         ontology.getString("prefix")));
            }
        }

        return this.cachedRdfOntologiesMapping;
    }
    private RdfNamespace initOntology(String ontologyName, URI defaultUri, String defaultPrefix)
    {
        RdfNamespace retVal = this.getOntologyNamespaces().get(ontologyName);
        if (retVal == null) {
            // initialization will be done uniformally below
            retVal = new RdfNamespaceImpl();
        }

        if (retVal.getUri() == null) {
            retVal = new RdfNamespaceImpl(defaultUri, retVal.getPrefix());
            Logger.info("Using a default ontology URL for " + ontologyName + ", constructed from the main base URL using a standardized endpoint; " + retVal.getUri());
        }
        if (StringUtils.isEmpty(retVal.getPrefix())) {
            retVal = new RdfNamespaceImpl(retVal.getUri(), defaultPrefix);
            Logger.info("Using a default ontology prefix for " + ontologyName + "; " + retVal.getPrefix());
        }

        return retVal;
    }
}
