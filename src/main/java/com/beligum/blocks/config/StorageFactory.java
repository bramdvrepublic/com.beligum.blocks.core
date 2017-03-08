package com.beligum.blocks.config;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.ResourceRegistrar;
import ch.qos.logback.classic.Level;
import com.beligum.base.cache.Cache;
import com.beligum.base.cache.CacheKey;
import com.beligum.base.cache.EhCacheAdaptor;
import com.beligum.base.cache.HashMapCache;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.filesystem.hdfs.HdfsImplDef;
import com.beligum.blocks.filesystem.hdfs.HdfsUtils;
import com.beligum.blocks.filesystem.hdfs.RequestTX;
import com.beligum.blocks.filesystem.hdfs.bitronix.CustomBitronixResourceProducer;
import com.beligum.blocks.filesystem.hdfs.bitronix.SimpleXAResourceProducer;
import com.beligum.blocks.filesystem.hdfs.impl.FileSystems;
import com.beligum.blocks.filesystem.ifaces.XAttrFS;
import com.beligum.blocks.filesystem.index.LucenePageIndexer;
import com.beligum.blocks.filesystem.index.SesamePageIndexer;
import com.beligum.blocks.filesystem.index.ifaces.Indexer;
import com.beligum.blocks.filesystem.index.ifaces.LuceneQueryConnection;
import com.beligum.blocks.filesystem.index.ifaces.PageIndexer;
import com.beligum.blocks.filesystem.index.ifaces.SparqlQueryConnection;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.UnsupportedFileSystemException;
import org.slf4j.LoggerFactory;
import org.xadisk.bridge.proxies.interfaces.XAFileSystem;
import org.xadisk.bridge.proxies.interfaces.XAFileSystemProxy;
import org.xadisk.bridge.proxies.interfaces.XASession;
import org.xadisk.filesystem.standalone.StandaloneFileSystemConfiguration;

import javax.transaction.Status;
import javax.transaction.Transaction;
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
import static com.beligum.blocks.caching.CacheKeys.TX_FAKE_REQUEST_CACHE;

/**
 * Created by bram on 2/16/16.
 */
public class StorageFactory
{
    //-----CONSTANTS-----
    public static final HdfsImplDef DEFAULT_PAGES_TX_FILESYSTEM = FileSystems.LOCAL_TX_CHROOT;
    public static final HdfsImplDef DEFAULT_PAGES_VIEW_FILESYSTEM = FileSystems.LOCAL_RO_CHROOT;

    //-----VARIABLES-----
    private static final Object txManagerLock = new Object();
    private static final Object requestTxLock = new Object();

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    public static FileContext createFileContext(Configuration configuration) throws UnsupportedFileSystemException
    {
        FileContext fileContext = FileContext.getFileContext(configuration);

        if (fileContext.getDefaultFileSystem() instanceof XAttrFS) {
            ((XAttrFS)fileContext.getDefaultFileSystem()).register(Settings.instance().getXAttrResolverFactory().create(fileContext));
        }

        return fileContext;
    }
    public static PageIndexer getMainPageIndexer() throws IOException
    {
        if (!cacheManager().getApplicationCache().containsKey(CacheKeys.MAIN_PAGE_INDEX)) {
            Indexer indexer = new LucenePageIndexer();
            getIndexerRegistry().add(indexer);
            cacheManager().getApplicationCache().put(CacheKeys.MAIN_PAGE_INDEX, indexer);
        }

        return (PageIndexer) cacheManager().getApplicationCache().get(CacheKeys.MAIN_PAGE_INDEX);
    }
    public static LuceneQueryConnection getMainPageQueryConnection() throws IOException
    {
        return (LuceneQueryConnection) getMainPageIndexer().connect();
    }
    public static PageIndexer getTriplestoreIndexer() throws IOException
    {
        if (!cacheManager().getApplicationCache().containsKey(CacheKeys.TRIPLESTORE_PAGE_INDEX)) {
            Indexer indexer = new SesamePageIndexer();
            getIndexerRegistry().add(indexer);
            cacheManager().getApplicationCache().put(CacheKeys.TRIPLESTORE_PAGE_INDEX, indexer);
        }

        return (PageIndexer) cacheManager().getApplicationCache().get(CacheKeys.TRIPLESTORE_PAGE_INDEX);
    }
    public static SparqlQueryConnection getTriplestoreQueryConnection() throws IOException
    {
        return (SparqlQueryConnection) getTriplestoreIndexer().connect();
    }
    public static TransactionManager getTransactionManager() throws IOException
    {
        if (!cacheManager().getApplicationCache().containsKey(CacheKeys.TRANSACTION_MANAGER)) {
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
                //This doesn't work because Hibernate picks up Bitronix during starup and this throws a 'cannot change the configuration while the transaction manager is running' exception...
                //workaround is to create a file called "bitronix-default-config.properties" in the resources folder that holds the "bitronix.tm.timer.defaultTransactionTimeout = xxx" property
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

                //tweak the log level if we're using atomikos
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

                cacheManager().getApplicationCache().put(CacheKeys.TRANSACTION_MANAGER, transactionManager);
            }
            catch (Exception e) {
                throw new IOException("Exception caught while booting up the transaction manager", e);
            }
        }

        return (TransactionManager) cacheManager().getApplicationCache().get(CacheKeys.TRANSACTION_MANAGER);
    }
    public static CustomBitronixResourceProducer getBitronixResourceProducer() throws IOException
    {
        //make sure there's a transaction manager first..
        TransactionManager tm = getTransactionManager();

        return (CustomBitronixResourceProducer) ResourceRegistrar.get(SimpleXAResourceProducer.UNIQUE_NAME);
    }
    public static RequestTX getCurrentRequestTx() throws IOException
    {
        Cache<CacheKey, Object> txCache = getCurrentTxCache();

        //Sync this with the release code below
        synchronized (requestTxLock) {
            if (!txCache.containsKey(CacheKeys.REQUEST_TRANSACTION)) {
                try {
                    TransactionManager transactionManager = getTransactionManager();
                    //start up a new transaction
                    transactionManager.begin();
                    //fetch the transaction attached to the current thread
                    Transaction transaction = transactionManager.getTransaction();
                    //wrap it in a request object
                    RequestTX cacheEntry = new RequestTX(transaction);

                    txCache.put(CacheKeys.REQUEST_TRANSACTION, cacheEntry);
                }
                catch (Exception e) {
                    throw new IOException("Exception caught while booting up a request transaction; " + R.requestContext().getJaxRsRequest().getUriInfo().getRequestUri(), e);
                }
            }

            return (RequestTX) txCache.get(CacheKeys.REQUEST_TRANSACTION);
        }
    }
    public static boolean hasCurrentRequestTx() throws IOException
    {
        return getCurrentTxCache().containsKey(CacheKeys.REQUEST_TRANSACTION);
    }
    /**
     * Normally, this is called from RequestTransactionFilter, but sometimes
     * we want to call multiple transactional request-operations during a single request (eg. during re-indexing).
     * The transaction system complains when doing this, so calling this method after every 'request-operation', we simulate a real request,
     * but it certainly is not the best approach...
     */
    public static void releaseCurrentRequestTx(boolean forceRollback) throws IOException
    {
        Cache<CacheKey, Object> txCache = getCurrentTxCache();

        //happens when the server hasn't started up yet
        if (txCache != null) {
            RequestTX tx = (RequestTX) txCache.get(CacheKeys.REQUEST_TRANSACTION);
            if (tx != null) {
                try {
                    if (forceRollback || tx.getStatus() != Status.STATUS_ACTIVE) {
                        tx.rollback();
                    }
                    else {
                        //this is the general case: try to commit and (try to) rollback on error
                        try {
                            tx.commit();
                        }
                        catch (Throwable e) {
                            try {
                                Logger.warn("Caught exception while committing a file system transaction, trying to rollback...", e);
                                tx.rollback();
                            }
                            catch (Throwable e1) {
                                //don't wait for the next reboot before trying to revert to a clean state; try it now
                                //note that the reboot method is implemented so that it doesn't throw (another) exception, so we can rely on it's return value quite safely
                                if (!StorageFactory.rebootPageStoreTransactionManager()) {
                                    throw new IOException(
                                                    "Exception caught while processing a file system transaction and the reboot because of a faulty rollback failed too; this is VERY bad and I don't really know what to do. You should investigate this!",
                                                    e1);
                                }
                                else {
                                    //we can't just swallow the exception; something's wrong and we should report it to the user
                                    throw new IOException(
                                                    "I was unable to commit a file system transaction and even the resulting rollback failed, but I did manage to reboot the filesystem. I'm adding the exception below;",
                                                    e1);
                                }
                            }
                        }
                    }
                }
                catch (Throwable e) {
                    throw new IOException("Exception caught while processing a file system transaction; this is bad", e);
                }
                finally {
                    try {
                        tx.close();
                    }
                    catch (Throwable e) {
                        throw new IOException("Exception caught while closing a file system transaction; this is bad", e);
                    }
                    finally {
                        //make sure we only do this once
                        txCache.remove(CacheKeys.REQUEST_TRANSACTION);

                        //detects if we're using a fake transaction cache and releases it if necessary
                        releaseCurrentTxCache();
                    }
                }
            }
        }
    }
    public static XASession getCurrentRequestXDiskTx() throws IOException
    {
        RequestTX requestTx = getCurrentRequestTx();

        //start up a new XDisk session if needed
        if (requestTx.getXdiskSession() == null) {
            try {
                //boot a new xadisk, register it and save it in our wrapper
                requestTx.setAndRegisterXdiskSession(getPageStoreTransactionManager().createSessionForXATransaction());
            }
            catch (Exception e) {
                throw new IOException("Exception caught while booting up XADisk transaction during request; " + R.requestContext().getJaxRsRequest().getUriInfo().getRequestUri(), e);
            }
        }

        return requestTx.getXdiskSession();
    }
    public static XAFileSystem getPageStoreTransactionManager() throws IOException
    {
        synchronized (txManagerLock) {
            if (!cacheManager().getApplicationCache().containsKey(CacheKeys.XADISK_FILE_SYSTEM)) {
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
                        cacheManager().getApplicationCache().put(CacheKeys.XADISK_FILE_SYSTEM, xafs);
                    }
                    catch (InterruptedException e) {
                        throw new IOException("Error occurred whlie booting transactional XADisk file system (timeout=" + Settings.instance().getPagesStoreJournalBootTimeout(), e);
                    }
                }
            }

            return (XAFileSystem) cacheManager().getApplicationCache().get(CacheKeys.XADISK_FILE_SYSTEM);
        }
    }
    public static boolean rebootPageStoreTransactionManager()
    {
        synchronized (txManagerLock) {
            boolean retVal = false;

            if (cacheManager().getApplicationCache().containsKey(CacheKeys.XADISK_FILE_SYSTEM)) {
                XAFileSystem xafs = (XAFileSystem) cacheManager().getApplicationCache().get(CacheKeys.XADISK_FILE_SYSTEM);
                //setting it here will ensure it's null internally, even if the next shutdown fails
                cacheManager().getApplicationCache().remove(CacheKeys.XADISK_FILE_SYSTEM);
                try {
                    xafs.shutdown();

                    //uniform reboot
                    getPageStoreTransactionManager();

                    retVal = true;
                }
                catch (IOException e) {
                    Logger.error("Exception caught while rebooting a transactional XADisk file system", e);
                }
            }

            return retVal;
        }
    }
    public static Configuration getPageStoreFileSystemConfig() throws IOException
    {
        if (!cacheManager().getApplicationCache().containsKey(CacheKeys.HDFS_PAGESTORE_FS_CONFIG)) {

            URI pageStorePath = Settings.instance().getPagesStorePath();
            if (StringUtils.isEmpty(pageStorePath.getScheme())) {
                //make sure we have a schema
                pageStorePath = URI.create(DEFAULT_PAGES_TX_FILESYSTEM.getScheme() + "://" + pageStorePath.toString());
                Logger.warn("The page store path doesn't have a schema, adding the HDFS " + DEFAULT_PAGES_TX_FILESYSTEM.getScheme() + "'://' prefix to use the local transactional file system; " +
                            pageStorePath.toString());
            }

            Configuration hadoopConfig = HdfsUtils.createHdfsConfig(pageStorePath, DEFAULT_PAGES_TX_FILESYSTEM, null, Settings.instance().getPagesHdfsProperties());

            cacheManager().getApplicationCache().put(CacheKeys.HDFS_PAGESTORE_FS_CONFIG, hadoopConfig);
        }

        return (Configuration) cacheManager().getApplicationCache().get(CacheKeys.HDFS_PAGESTORE_FS_CONFIG);
    }
    public static FileContext getPageStoreFileSystem() throws IOException
    {
        if (!cacheManager().getApplicationCache().containsKey(CacheKeys.HDFS_PAGESTORE_FS)) {
            FileContext fileContext = StorageFactory.createFileContext(getPageStoreFileSystemConfig());

            //boot the XADisk instance too (probably still null here, good place to doIsValid them together)
            getPageStoreTransactionManager();

            //create the root folder if needed
            //TODO: commented out because we're not in a transaction here
//            org.apache.hadoop.fs.Path root = new org.apache.hadoop.fs.Path("/");
//            if (!fileContext.util().exists(root)) {
//                fileContext.mkdir(root, FsPermission.getDirDefault(), true);
//            }

            cacheManager().getApplicationCache().put(CacheKeys.HDFS_PAGESTORE_FS, fileContext);
        }

        return (FileContext) cacheManager().getApplicationCache().get(CacheKeys.HDFS_PAGESTORE_FS);
    }
    public static Configuration getPageViewFileSystemConfig() throws IOException
    {
        if (!cacheManager().getApplicationCache().containsKey(CacheKeys.HDFS_PAGEVIEW_FS_CONFIG)) {

            URI pageViewPath = Settings.instance().getPagesViewPath();
            if (StringUtils.isEmpty(pageViewPath.getScheme())) {
                //make sure we have a schema
                pageViewPath = URI.create(DEFAULT_PAGES_VIEW_FILESYSTEM.getScheme() + "://" + pageViewPath.toString());
                Logger.warn("The page view path doesn't have a schema, adding the HDFS " + DEFAULT_PAGES_VIEW_FILESYSTEM.getScheme() + "'://' prefix to use the local file system; " +
                            pageViewPath.toString());
            }

            Configuration hadoopConfig = HdfsUtils.createHdfsConfig(pageViewPath, DEFAULT_PAGES_VIEW_FILESYSTEM, null, Settings.instance().getPagesHdfsProperties());

            cacheManager().getApplicationCache().put(CacheKeys.HDFS_PAGEVIEW_FS_CONFIG, hadoopConfig);
        }

        return (Configuration) cacheManager().getApplicationCache().get(CacheKeys.HDFS_PAGEVIEW_FS_CONFIG);
    }
    public static FileContext getPageViewFileSystem() throws IOException
    {
        if (!cacheManager().getApplicationCache().containsKey(CacheKeys.HDFS_PAGEVIEW_FS)) {
            cacheManager().getApplicationCache().put(CacheKeys.HDFS_PAGEVIEW_FS, StorageFactory.createFileContext(getPageViewFileSystemConfig()));
        }

        return (FileContext) cacheManager().getApplicationCache().get(CacheKeys.HDFS_PAGEVIEW_FS);
    }
    public static Set<Indexer> getIndexerRegistry()
    {
        if (!cacheManager().getApplicationCache().containsKey(CacheKeys.REGISTERED_INDEXERS)) {
            cacheManager().getApplicationCache().put(CacheKeys.REGISTERED_INDEXERS, new HashSet<>());
        }

        return (Set<Indexer>) cacheManager().getApplicationCache().get(CacheKeys.REGISTERED_INDEXERS);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    /**
     * Since a request can span more than a request-period (eg. request booting an async long running atomic action),
     * we decided to separate this from the request cache. This goes hand in hand with RequestTransactionFilter.executeManualTxCleanup()
     * which enables us to manually end the currently running transaction.
     */
    private static Cache<CacheKey, Object> getCurrentTxCache() throws IOException
    {
        //sync this with releaseCurrentRequestTx()
        Cache<CacheKey, Object> retVal = R.cacheManager().getRequestCache();

        //this means we're not in a request context (most probably in an async method),
        // so we'll boot up a fake request cache for this transaction
        if (R.booted() && retVal == null) {
            Cache allRequestsCache = getFakeRequestCache();

            String currentCacheId = getFakeRequestId();
            retVal = (Cache<CacheKey, Object>) allRequestsCache.get(currentCacheId);
            if (retVal == null) {
                Logger.warn("Building a fake TX (request) cache for id " + currentCacheId + " to support a long-running asynchronous execution. The cache currently holds " + allRequestsCache.size() +
                            " entries. Please use this sparingly, it's quite untested...");
                //let's re-use the id as the name of the cache, hope that's ok...
                allRequestsCache.put(currentCacheId, retVal = new HashMapCache<CacheKey, Object>(currentCacheId));
            }
        }

        return retVal;
    }
    private static void releaseCurrentTxCache()
    {
        //detectAndReplace if we're using a fake request cache and remove it if necessary
        Cache<CacheKey, Object> requestCache = R.cacheManager().getRequestCache();
        if (requestCache == null) {
            Cache allRequestsCache = getFakeRequestCache();
            String currentCacheId = getFakeRequestId();
            Cache<CacheKey, Object> fakeRequestCache = (Cache<CacheKey, Object>) allRequestsCache.get(currentCacheId);
            if (fakeRequestCache != null) {
                allRequestsCache.remove(currentCacheId);
            }
        }
    }
    private static String getFakeRequestId()
    {
        return String.valueOf(Thread.currentThread().getId());
    }
    private static Cache getFakeRequestCache()
    {
        if (!R.cacheManager().cacheExists(TX_FAKE_REQUEST_CACHE.name())) {

            //we create a cache where it's entries live for at most one day (both from creation time as from last accessed time),
            //doesn't overflow to disk and keep at most 100 results
            //Note: one day seems to be a long time, but that's the whose point of supporting long-running asynchronous methods, right?
            long timeToLiveSeconds = 60 * 60 * 24;
            long timeToIdleSeconds = timeToLiveSeconds;
            R.cacheManager().registerCache(new EhCacheAdaptor(TX_FAKE_REQUEST_CACHE.name(), 100, false, false, timeToLiveSeconds, timeToIdleSeconds));
        }

        return R.cacheManager().getCache(TX_FAKE_REQUEST_CACHE.name());
    }
}
