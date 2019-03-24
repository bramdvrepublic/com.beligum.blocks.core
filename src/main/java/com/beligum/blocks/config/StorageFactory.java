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

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.ResourceRegistrar;
import ch.qos.logback.classic.Level;
import com.beligum.base.cache.Cache;
import com.beligum.base.cache.CacheBiFunction;
import com.beligum.base.cache.CacheFunction;
import com.beligum.base.cache.CacheKey;
import com.beligum.base.resources.ifaces.AutoLock;
import com.beligum.base.server.R;
import com.beligum.base.server.ifaces.RequestCloseable;
import com.beligum.base.utils.Logger;
import com.beligum.base.utils.toolkit.NetworkFunctions;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.filesystem.hdfs.HdfsImplDef;
import com.beligum.blocks.filesystem.hdfs.HdfsUtils;
import com.beligum.blocks.filesystem.hdfs.TX;
import com.beligum.blocks.filesystem.hdfs.bitronix.CustomBitronixResourceProducer;
import com.beligum.blocks.filesystem.hdfs.bitronix.SimpleXAResourceProducer;
import com.beligum.blocks.filesystem.hdfs.impl.FileSystems;
import com.beligum.blocks.filesystem.ifaces.XAttrFS;
import com.beligum.blocks.filesystem.index.LucenePageIndexer;
import com.beligum.blocks.filesystem.index.SesamePageIndexer;
import com.beligum.blocks.filesystem.index.ifaces.Indexer;
import com.beligum.blocks.filesystem.index.ifaces.PageIndexConnection;
import com.beligum.blocks.filesystem.index.ifaces.PageIndexer;
import com.beligum.blocks.filesystem.index.solr.SolrPageIndexer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.UnsupportedFileSystemException;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.slf4j.LoggerFactory;
import org.xadisk.bridge.proxies.interfaces.XAFileSystem;
import org.xadisk.bridge.proxies.interfaces.XAFileSystemProxy;
import org.xadisk.bridge.proxies.interfaces.XASession;
import org.xadisk.filesystem.standalone.StandaloneFileSystemConfiguration;

import javax.transaction.TransactionManager;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.beligum.base.server.R.cacheManager;

/**
 * Created by bram on 2/16/16.
 */
public class StorageFactory
{
    //-----CONSTANTS-----
    public static final HdfsImplDef DEFAULT_PAGES_TX_FILESYSTEM = FileSystems.LOCAL_TX_CHROOT;
    public static final HdfsImplDef DEFAULT_PAGES_VIEW_FILESYSTEM = FileSystems.LOCAL_RO_CHROOT;

    //-----VARIABLES-----
    private static final Object currentThreadTxLock = new Object();

    /**
     * An inheritable thread local is needed if we want to support executor services launched from
     * an asynchronous thread, so all child-threads spawned from the main async thread will
     * share the same transaction.
     * Note: I assume the access to the contents of this variable is thread safe, right?
     * --> the access yes, but we still need to synchronize the glue code, see below.
     */
    private static final ThreadLocal<TX> currentThreadTx = new InheritableThreadLocal();

    private static final Lock lock = new Lock();

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    public static FileContext createFileContext(Configuration configuration) throws UnsupportedFileSystemException
    {
        FileContext fileContext = FileContext.getFileContext(configuration);

        if (fileContext.getDefaultFileSystem() instanceof XAttrFS) {
            ((XAttrFS) fileContext.getDefaultFileSystem()).register(Settings.instance().getXAttrResolverFactory().create(fileContext));
        }

        return fileContext;
    }
    public static PageIndexer getJsonIndexer() throws IOException
    {
        return cacheManager().getApplicationCache().getAndPutIfAbsent(CacheKeys.JSON_PAGE_INDEXER, new CacheFunction<CacheKey, PageIndexer>()
        {
            @Override
            public PageIndexer apply(CacheKey cacheKey) throws IOException
            {
                //TODO decide if we change to Solr or implement a switch
//                PageIndexer indexer = new LucenePageIndexer(lock);
                PageIndexer indexer = new SolrPageIndexer(lock);

                getIndexerRegistry().add(indexer);

                return indexer;
            }
        });
    }
    public static PageIndexConnection getJsonQueryConnection() throws IOException
    {
        //Note that we don't supply a transaction to a query connection since we assume querying is read-only
        return getJsonIndexer().connect(null);
    }
    public static PageIndexer getSparqlIndexer() throws IOException
    {
        return cacheManager().getApplicationCache().getAndPutIfAbsent(CacheKeys.SPARQL_PAGE_INDEXER, new CacheFunction<CacheKey, PageIndexer>()
        {
            @Override
            public PageIndexer apply(CacheKey cacheKey) throws IOException
            {
                PageIndexer indexer = new SesamePageIndexer(lock);
                getIndexerRegistry().add(indexer);

                return indexer;
            }
        });
    }
    public static PageIndexConnection getSparqlQueryConnection() throws IOException
    {
        //Note that we don't supply a transaction to a query connection since we assume querying is read-only
        return getSparqlIndexer().connect(null);
    }
    public static TransactionManager getTransactionManager() throws IOException
    {
        return cacheManager().getApplicationCache().getAndPutIfAbsent(CacheKeys.TRANSACTION_MANAGER, new CacheFunction<CacheKey, TransactionManager>()
        {
            @Override
            public TransactionManager apply(CacheKey cacheKey) throws IOException
            {
                try {
                    Map<String, String> extraProperties = Settings.instance().getTransactionsProperties();
                    if (extraProperties != null) {
                        for (Map.Entry<String, String> entry : extraProperties.entrySet()) {
                            System.setProperty(entry.getKey(), entry.getValue());
                        }
                    }

                    //atomikos
                    //TransactionManager transactionManager = Settings.instance().getTransactionManagerClass().newInstance();

                    //bitronix
                    //This doesn't work because Hibernate picks up Bitronix during startup and this throws a 'cannot change the configuration while the transaction manager is running' exception...
                    //workaround is to instance a file called "bitronix-default-config.properties" in the resources folder that holds the "bitronix.tm.timer.defaultTransactionTimeout = xxx" property
                    //or to set the JVM system properties.
                    //DONE: this is implemented now, see BlocksSystemPropertyFactory
                    //TransactionManagerServices.getConfiguration().setDefaultTransactionTimeout(Settings.instance().getTransactionTimeoutSeconds());

                    TransactionManager transactionManager = TransactionManagerServices.getTransactionManager();

                    //Register our custom producer
                    //Note that the unregister method is called from SimpleXAResourceProducer.close(), hope that's ok
                    ResourceRegistrar.register(new SimpleXAResourceProducer());

                    //Note that this values doesn't seem to propagate to the sub-transactions of the resources.
                    //The value of Bitronix' DefaultTransactionTimeout does, eg. for XADisk via this way:
                    //enlist() -> setTimeoutDate() -> start() -> XAResource.setTransactionTimeout() -> XASession.setTransactionTimeout()
                    // --> during enlist() (actually start()), a thread context is initialized that loads the default tx timeout value and so it gets passed along all the way down
                    //see bitronix.tm.BitronixTransactionManager.begin() (rule 126)
                    //and org.xadisk.filesystem.workers.TransactionTimeoutDetector.doWorkOnce() (rule 33)
                    //Fix above so we can reactivate the setting
                    //Note: also note there's an important setting in the constructor of com.beligum.blocks.fs.hdfs.bitronix.XAResourceProducer (setApplyTransactionTimeout()) that affects a lot...
                    //DONE: this is implemented now, see BlocksSystemPropertyFactory
                    //transactionManager.setTransactionTimeout(Settings.instance().getTransactionTimeoutSeconds());

                    //tweak the log level if we're using atomikos (old code)
                    if (transactionManager.getClass().getCanonicalName().contains("atomikos")) {
                        ch.qos.logback.classic.Logger atomikosLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("com.atomikos");
                        if (atomikosLogger != null) {
                            //Atomikos info level is way too verbose; shut it down and switch to (default) WARN
                            if (R.configuration().getLogConfig().getLogLevel().equals(Level.INFO)) {
                                //atomikosLogger.setLevel(Level.WARN);
                            }
                        }
                        else {
                            throw new IOException("Error while configuring Atomikos logger; couldn't find the logger");
                        }
                    }

                    return transactionManager;
                }
                catch (Exception e) {
                    throw new IOException("Exception caught while booting up the transaction manager", e);
                }
            }
        });
    }
    public static CustomBitronixResourceProducer getBitronixResourceProducer() throws IOException
    {
        //make sure there's a transaction manager first..
        getTransactionManager();

        return (CustomBitronixResourceProducer) ResourceRegistrar.get(SimpleXAResourceProducer.UNIQUE_NAME);
    }
    public static TX getCurrentRequestTx() throws IOException
    {
        TX retVal = null;

        Cache<CacheKey, Object> txCache = R.cacheManager().getRequestCache();

        if (txCache != null) {
            retVal = cacheManager().getRequestCache().getAndPutIfAbsent(CacheKeys.REQUEST_TRANSACTION, new CacheFunction<CacheKey, TX>()
            {
                @Override
                public TX apply(CacheKey cacheKey) throws IOException
                {
                    try {
                        TX tx = new TX(getTransactionManager());

                        //make sure the TX gets closed at the end of the request
                        R.requestContext().registerClosable(new RequestCloseable()
                        {
                            @Override
                            public void close(RequestEvent event) throws Exception
                            {
                                releaseCurrentRequestTx(NetworkFunctions.isFailedRequestEvent(event));
                            }
                        });

                        return tx;
                    }
                    catch (Exception e) {
                        throw new IOException("Exception caught while booting up a request transaction; " + R.requestContext().getJaxRsRequest().getUriInfo().getRequestUri(), e);
                    }
                }
            });
        }

        return retVal;
    }
    public static boolean hasCurrentRequestTx() throws IOException
    {
        boolean retVal = false;

        Cache<CacheKey, Object> txCache = R.cacheManager().getRequestCache();

        if (txCache != null) {
            try (AutoLock lock = txCache.acquireReadLock(CacheKeys.REQUEST_TRANSACTION)) {
                retVal = txCache.containsKey(CacheKeys.REQUEST_TRANSACTION);
            }
        }

        return retVal;
    }
    public static TX getCurrentScopeTx() throws IOException
    {
        return R.requestContext().isActive() ? getCurrentRequestTx() : getCurrentThreadTx();
    }
    public static void releaseCurrentRequestTx(boolean forceRollback) throws IOException
    {
        Cache<CacheKey, Object> txCache = R.cacheManager().getRequestCache();

        //happens when the server hasn't started up yet or we're not in a request context
        if (txCache != null) {

            txCache.removeIfPresent(CacheKeys.REQUEST_TRANSACTION, new CacheBiFunction<CacheKey, TX, Void>()
            {
                @Override
                public Void apply(CacheKey cacheKey, TX value) throws IOException
                {
                    //don't release anything if no tx is active
                    if (value != null) {
                        try {
                            value.close(forceRollback);
                        }
                        catch (Throwable e) {
                            //don't wait for the next reboot before trying to revert to a clean state; try it now
                            //note that the reboot method is implemented so that it doesn't throw (another) exception, so we can rely on it's return value quite safely
                            if (!StorageFactory.rebootPageStoreTransactionManager()) {
                                throw new IOException(
                                                "Exception caught while processing a file system transaction and the reboot because of a faulty rollback failed too; this is VERY bad and I don't really know what to do. You should investigate this!",
                                                e);
                            }
                            else {
                                //we can't just swallow the exception; something's wrong and we should report it to the user
                                throw new IOException(
                                                "I was unable to commit a file system transaction and even the resulting rollback failed, but I did manage to reboot the filesystem. I'm adding the exception below;",
                                                e);
                            }
                        }
                    }

                    return null;
                }
            });
        }
    }
    /**
     * Creates a manual transaction, attached to the calling thread that needs to be closed manually using releaseCurrentThreadTx().
     */
    public static TX createCurrentThreadTx(TX.Listener listener, long timeoutMillis) throws IOException
    {
        synchronized (currentThreadTxLock) {
            TX retVal = currentThreadTx.get();

            if (retVal == null) {
                try {
                    currentThreadTx.set(retVal = new TX(getTransactionManager(), listener, timeoutMillis));
                }
                catch (Exception e) {
                    throw new IOException("Exception caught while booting up a thread transaction", e);
                }
            }
            else {
                throw new IOException("Can't create thread transaction because there's an active one already; you should release that one first.");
            }

            return retVal;
        }
    }
    public static TX createCurrentThreadTx() throws IOException
    {
        return createCurrentThreadTx(null, 0);
    }
    public static TX getCurrentThreadTx() throws IOException
    {
        return currentThreadTx.get();
    }
    public static boolean hasCurrentThreadTx() throws IOException
    {
        return currentThreadTx.get() != null;
    }
    public static void releaseCurrentThreadTx(boolean forceRollback) throws IOException
    {
        synchronized (currentThreadTxLock) {
            TX transaction = currentThreadTx.get();

            if (transaction != null) {
                try {
                    transaction.close(forceRollback);
                }
                catch (Throwable e) {
                    //we can't just swallow the exception; something's wrong and we should report it to the user
                    throw new IOException("I was unable to close a thread transaction. I'm adding the exception below;", e);
                }
                finally {
                    currentThreadTx.remove();
                }
            }
            else {
                throw new IOException("Can't release the current thread transaction because there's none active");
            }
        }
    }
    public static XASession getCurrentXDiskTx() throws IOException
    {
        TX tx = getCurrentScopeTx();

        if (tx == null) {
            throw new IOException("We're not in an active transaction context, so I can't instance an XDisk session inside the current transaction scope");
        }
        else {
            //start up a new XDisk session if needed
            synchronized (tx) {
                if (tx.getXdiskSession() == null) {
                    try {
                        //boot a new xadisk, register it and save it in our wrapper
                        tx.setAndRegisterXdiskSession(getXADiskTransactionManager().createSessionForXATransaction());
                    }
                    catch (Exception e) {
                        throw new IOException("Exception caught while booting up XADisk transaction during request; " + R.requestContext().getJaxRsRequest().getUriInfo().getRequestUri(), e);
                    }
                }
            }

            return tx.getXdiskSession();
        }
    }
    public static XAFileSystem getXADiskTransactionManager() throws IOException
    {
        return cacheManager().getApplicationCache().getAndPutIfAbsent(CacheKeys.XADISK_FILE_SYSTEM, new CacheFunction<CacheKey, Object>()
        {
            @Override
            public Object apply(CacheKey cacheKey) throws IOException
            {
                //by default, we return nothing
                Object retVal = CacheFunction.SKIP;

                URI dir = Settings.instance().getPagesStoreJournalDir();
                if (dir != null) {
                    Path journalDir = Paths.get(dir);
                    if (!Files.exists(journalDir)) {
                        Files.createDirectories(journalDir);
                    }

                    StandaloneFileSystemConfiguration cfg = new StandaloneFileSystemConfiguration(journalDir.toFile().getAbsolutePath(), Settings.instance().getPagesStoreJournalId());
                    //cfg.setTransactionTimeout(Settings.instance().getSubTransactionTimeoutSeconds());

                    XAFileSystem xafs = XAFileSystemProxy.bootNativeXAFileSystem(cfg);
                    try {
                        xafs.waitForBootup(Settings.instance().getPagesStoreJournalBootTimeout());
                        retVal = xafs;
                    }
                    catch (InterruptedException e) {
                        throw new IOException("Error occurred while booting transactional XADisk file system (timeout=" + Settings.instance().getPagesStoreJournalBootTimeout(), e);
                    }
                }

                return retVal;
            }
        });
    }
    public static boolean rebootPageStoreTransactionManager()
    {
        boolean retVal = false;

        try {
            XAFileSystem existingFs = cacheManager().getApplicationCache().removeIfPresent(CacheKeys.XADISK_FILE_SYSTEM, new CacheBiFunction<CacheKey, XAFileSystem, Void>()
            {
                @Override
                public Void apply(CacheKey cacheKey, XAFileSystem value) throws IOException
                {
                    value.shutdown();

                    //uniform reboot
                    getXADiskTransactionManager();

                    return null;
                }
            });

            retVal = existingFs != null;
        }
        catch (IOException e) {
            Logger.error("Exception caught while rebooting a transactional XADisk file system", e);
        }

        return retVal;
    }
    public static Configuration getPageStoreFileSystemConfig() throws IOException
    {
        if (!cacheManager().getApplicationCache().containsKey(CacheKeys.HDFS_PAGESTORE_FS_CONFIG)) {
            cacheManager().getApplicationCache().put(CacheKeys.HDFS_PAGESTORE_FS_CONFIG,
                                                     HdfsUtils.createHdfsConfig(Settings.instance().getPagesStoreUri(), null, Settings.instance().getPagesHdfsProperties()));

        }

        return (Configuration) cacheManager().getApplicationCache().get(CacheKeys.HDFS_PAGESTORE_FS_CONFIG);
    }
    public static FileContext getPageStoreFileSystem() throws IOException
    {
        return cacheManager().getApplicationCache().getAndPutIfAbsent(CacheKeys.HDFS_PAGESTORE_FS, new CacheFunction<CacheKey, FileContext>()
        {
            @Override
            public FileContext apply(CacheKey cacheKey) throws IOException
            {
                FileContext fileContext = StorageFactory.createFileContext(getPageStoreFileSystemConfig());

                //TODO should we move this to the LOCAL_TX implementations?
                if (StorageFactory.needsXADisk(fileContext)) {
                    //boot the XADisk instance too (probably still null here, good place to boot them together)
                    getXADiskTransactionManager();
                }

                //instance the root folder if needed
                //TODO: commented out because we're not in a transaction here
                //            org.apache.hadoop.fs.Path root = new org.apache.hadoop.fs.Path("/");
                //            if (!fileContext.util().exists(root)) {
                //                fileContext.mkdir(root, FsPermission.getDirDefault(), true);
                //            }

                return fileContext;
            }
        });
    }
    public static Configuration getPageViewFileSystemConfig() throws IOException
    {
        return cacheManager().getApplicationCache().getAndPutIfAbsent(CacheKeys.HDFS_PAGEVIEW_FS_CONFIG, new CacheFunction<CacheKey, Configuration>()
        {
            @Override
            public Configuration apply(CacheKey cacheKey) throws IOException
            {
                return HdfsUtils.createHdfsConfig(Settings.instance().getPagesViewUri(), null, Settings.instance().getPagesHdfsProperties());
            }
        });
    }
    public static FileContext getPageViewFileSystem() throws IOException
    {
        return cacheManager().getApplicationCache().getAndPutIfAbsent(CacheKeys.HDFS_PAGEVIEW_FS, new CacheFunction<CacheKey, FileContext>()
        {
            @Override
            public FileContext apply(CacheKey cacheKey) throws IOException
            {
                return StorageFactory.createFileContext(getPageViewFileSystemConfig());
            }
        });
    }
    public static boolean needsXADisk(FileContext fileContext)
    {
        String schema = fileContext.getDefaultFileSystem().getUri().getScheme();
        return schema.equals(FileSystems.LOCAL_TX.getScheme()) || schema.equals(FileSystems.LOCAL_TX_CHROOT.getScheme());
    }

    //-----PROTECTED METHODS-----
    protected static Set<Indexer> getIndexerRegistry()
    {
        try {
            return cacheManager().getApplicationCache().getAndPutIfAbsent(CacheKeys.REGISTERED_INDEXERS, new CacheFunction<CacheKey, Set<Indexer>>()
            {
                @Override
                public Set<Indexer> apply(CacheKey cacheKey) throws IOException
                {
                    return new HashSet<>();
                }
            });
        }
        catch (IOException e) {
            Logger.error("Error while creating a HashSet, this shouldn't happen", e);
            return null;
        }
    }

    //-----PRIVATE METHODS-----

    //-----INNER CLASSES-----
    /**
     * Special lock class that can only be instantiated here to pass to external constructors
     * so we're sure they can't be created manually elsewhere, circumventing our checks and registers.
     */
    public static class Lock
    {
        private Lock()
        {
        }
    }

}
