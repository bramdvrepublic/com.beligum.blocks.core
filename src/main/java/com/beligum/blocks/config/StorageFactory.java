package com.beligum.blocks.config;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.ResourceRegistrar;
import ch.qos.logback.classic.Level;
import com.beligum.base.cache.Cache;
import com.beligum.base.cache.CacheKey;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
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
    public static final HdfsImplDef DEFAULT_PAGES_NOTX_FILESYSTEM = FileSystems.LOCAL_CHROOT;

    //-----VARIABLES-----
    private static final Object mainPageIndexerLock = new Object();
    private static final Object triplestoreIndexerLock = new Object();
    private static final Object transactionManagerLock = new Object();
    private static final Object pageStoreTxManagerLock = new Object();
    private static final Object requestTxLock = new Object();
    private static final Object pageStoreFsLock = new Object();
    private static final Object pageViewFsLock = new Object();

    /**
     * An inherited thread local is needed if we want to support executor services launched from
     * an asynchronous thread, so all child-threads spawned from the main async thread will
     * share the same transaction.
     * Note: I assume the access to the contents of this variable is thread safe, right?
     */
    private static final ThreadLocal<TX> currentThreadTx = new InheritableThreadLocal();

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
    public static PageIndexer getMainPageIndexer() throws IOException
    {
        synchronized (mainPageIndexerLock) {
            if (!cacheManager().getApplicationCache().containsKey(CacheKeys.MAIN_PAGE_INDEX)) {
                PageIndexer indexer = new LucenePageIndexer();
                getIndexerRegistry().add(indexer);
                cacheManager().getApplicationCache().put(CacheKeys.MAIN_PAGE_INDEX, indexer);
            }
        }

        return (PageIndexer) cacheManager().getApplicationCache().get(CacheKeys.MAIN_PAGE_INDEX);
    }
    public static LuceneQueryConnection getMainPageQueryConnection() throws IOException
    {
        //Note that we don't supply a transaction to a query connection since we assume querying is read-only
        return (LuceneQueryConnection) getMainPageIndexer().connect(null);
    }
    public static PageIndexer getTriplestoreIndexer() throws IOException
    {
        synchronized (triplestoreIndexerLock) {
            if (!cacheManager().getApplicationCache().containsKey(CacheKeys.TRIPLESTORE_PAGE_INDEX)) {
                PageIndexer indexer = new SesamePageIndexer();
                getIndexerRegistry().add(indexer);
                cacheManager().getApplicationCache().put(CacheKeys.TRIPLESTORE_PAGE_INDEX, indexer);
            }
        }

        return (PageIndexer) cacheManager().getApplicationCache().get(CacheKeys.TRIPLESTORE_PAGE_INDEX);
    }
    public static SparqlQueryConnection getTriplestoreQueryConnection() throws IOException
    {
        //Note that we don't supply a transaction to a query connection since we assume querying is read-only
        return (SparqlQueryConnection) getTriplestoreIndexer().connect(null);
    }
    public static TransactionManager getTransactionManager() throws IOException
    {
        synchronized (transactionManagerLock) {
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

                    cacheManager().getApplicationCache().put(CacheKeys.TRANSACTION_MANAGER, transactionManager);
                }
                catch (Exception e) {
                    throw new IOException("Exception caught while booting up the transaction manager", e);
                }
            }
        }

        return (TransactionManager) cacheManager().getApplicationCache().get(CacheKeys.TRANSACTION_MANAGER);
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
            //Sync this with the release code below
            synchronized (requestTxLock) {
                if (!txCache.containsKey(CacheKeys.REQUEST_TRANSACTION)) {
                    try {
                        txCache.put(CacheKeys.REQUEST_TRANSACTION, new TX(getTransactionManager()));
                    }
                    catch (Exception e) {
                        throw new IOException("Exception caught while booting up a request transaction; " + R.requestContext().getJaxRsRequest().getUriInfo().getRequestUri(), e);
                    }
                }

                retVal = (TX) txCache.get(CacheKeys.REQUEST_TRANSACTION);
            }
        }

        return retVal;
    }
    public static boolean hasCurrentRequestTx() throws IOException
    {
        Cache<CacheKey, Object> txCache = R.cacheManager().getRequestCache();

        synchronized (requestTxLock) {
            return txCache != null && txCache.containsKey(CacheKeys.REQUEST_TRANSACTION);
        }
    }
    public static void releaseCurrentRequestTx(boolean forceRollback) throws IOException
    {
        Cache<CacheKey, Object> txCache = R.cacheManager().getRequestCache();

        //happens when the server hasn't started up yet or we're not in a request context
        if (txCache != null) {
            TX tx = (TX) txCache.get(CacheKeys.REQUEST_TRANSACTION);

            //don't release anything if no tx is active
            if (tx != null) {
                synchronized (requestTxLock) {
                    try {
                        tx.close(forceRollback);
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
                    finally {
                        //make sure we only do this once
                        txCache.remove(CacheKeys.REQUEST_TRANSACTION);
                    }
                }
            }
        }
    }
    /**
     * Creates a manual transaction, attached to the calling thread that needs to be closed manually using releaseCurrentThreadTx().
     */
    public static TX createCurrentThreadTx(TX.Listener listener, long timeoutMillis) throws IOException
    {
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
    public static XASession getCurrentXDiskTx() throws IOException
    {
        TX tx = R.requestContext().isActive() ? getCurrentRequestTx() : getCurrentThreadTx();

        if (tx == null) {
            throw new IOException("We're not in an active transaction context, so I can't instance an XDisk session inside the current transaction scope");
        }
        else {
            //start up a new XDisk session if needed (synchronized method)
            if (tx.getXdiskSession() == null) {
                try {
                    //boot a new xadisk, register it and save it in our wrapper
                    tx.setAndRegisterXdiskSession(getPageStoreTransactionManager().createSessionForXATransaction());
                }
                catch (Exception e) {
                    throw new IOException("Exception caught while booting up XADisk transaction during request; " + R.requestContext().getJaxRsRequest().getUriInfo().getRequestUri(), e);
                }
            }

            return tx.getXdiskSession();
        }
    }
    public static XAFileSystem getPageStoreTransactionManager() throws IOException
    {
        synchronized (pageStoreTxManagerLock) {
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
                        throw new IOException("Error occurred while booting transactional XADisk file system (timeout=" + Settings.instance().getPagesStoreJournalBootTimeout(), e);
                    }
                }
            }

            return (XAFileSystem) cacheManager().getApplicationCache().get(CacheKeys.XADISK_FILE_SYSTEM);
        }
    }
    public static boolean rebootPageStoreTransactionManager()
    {
        synchronized (pageStoreTxManagerLock) {
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
                pageStorePath = URI.create(DEFAULT_PAGES_TX_FILESYSTEM.getScheme() + ":" + pageStorePath.getSchemeSpecificPart());
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
        synchronized (pageStoreFsLock) {
            if (!cacheManager().getApplicationCache().containsKey(CacheKeys.HDFS_PAGESTORE_FS)) {
                FileContext fileContext = StorageFactory.createFileContext(getPageStoreFileSystemConfig());

                //boot the XADisk instance too (probably still null here, good place to doIsValid them together)
                getPageStoreTransactionManager();

                //instance the root folder if needed
                //TODO: commented out because we're not in a transaction here
                //            org.apache.hadoop.fs.Path root = new org.apache.hadoop.fs.Path("/");
                //            if (!fileContext.util().exists(root)) {
                //                fileContext.mkdir(root, FsPermission.getDirDefault(), true);
                //            }

                cacheManager().getApplicationCache().put(CacheKeys.HDFS_PAGESTORE_FS, fileContext);
            }
        }

        return (FileContext) cacheManager().getApplicationCache().get(CacheKeys.HDFS_PAGESTORE_FS);
    }
    public static Configuration getPageViewFileSystemConfig() throws IOException
    {
        if (!cacheManager().getApplicationCache().containsKey(CacheKeys.HDFS_PAGEVIEW_FS_CONFIG)) {

            URI pageViewPath = Settings.instance().getPagesViewPath();
            if (StringUtils.isEmpty(pageViewPath.getScheme())) {
                //make sure we have a schema
                pageViewPath = URI.create(DEFAULT_PAGES_VIEW_FILESYSTEM.getScheme() + ":" + pageViewPath.getSchemeSpecificPart());
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
        synchronized (pageViewFsLock) {
            if (!cacheManager().getApplicationCache().containsKey(CacheKeys.HDFS_PAGEVIEW_FS)) {
                cacheManager().getApplicationCache().put(CacheKeys.HDFS_PAGEVIEW_FS, StorageFactory.createFileContext(getPageViewFileSystemConfig()));
            }
        }

        return (FileContext) cacheManager().getApplicationCache().get(CacheKeys.HDFS_PAGEVIEW_FS);
    }

    //-----PROTECTED METHODS-----
    protected static synchronized Set<Indexer> getIndexerRegistry()
    {
        if (!cacheManager().getApplicationCache().containsKey(CacheKeys.REGISTERED_INDEXERS)) {
            cacheManager().getApplicationCache().put(CacheKeys.REGISTERED_INDEXERS, new HashSet<>());
        }

        return (Set<Indexer>) cacheManager().getApplicationCache().get(CacheKeys.REGISTERED_INDEXERS);
    }

    //-----PRIVATE METHODS-----

}
