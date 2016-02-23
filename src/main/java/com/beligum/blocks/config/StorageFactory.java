package com.beligum.blocks.config;

import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.fs.hdfs.RequestTX;
import com.beligum.blocks.fs.hdfs.TransactionalRawLocalFS;
import com.beligum.blocks.fs.hdfs.TransactionalRawLocalFileSystem;
import com.beligum.blocks.fs.index.LucenePageIndexer;
import com.beligum.blocks.fs.index.SesamePageIndexer;
import com.beligum.blocks.fs.index.ifaces.Indexer;
import com.beligum.blocks.fs.index.ifaces.PageIndexer;
import com.beligum.blocks.fs.pages.SimplePageStore;
import com.beligum.blocks.fs.pages.ifaces.PageStore;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.AbstractFileSystem;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.FsConstants;
import org.apache.hadoop.fs.local.RawLocalFs;
import org.xadisk.bridge.proxies.interfaces.XAFileSystem;
import org.xadisk.bridge.proxies.interfaces.XAFileSystemProxy;
import org.xadisk.bridge.proxies.interfaces.XASession;
import org.xadisk.filesystem.standalone.StandaloneFileSystemConfiguration;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by bram on 2/16/16.
 */
public class StorageFactory
{
    //-----CONSTANTS-----
    public static final Class<? extends AbstractFileSystem> DEFAULT_TX_FILESYSTEM = TransactionalRawLocalFS.class;
    public static final String DEFAULT_TX_FILESYSTEM_SCHEME = TransactionalRawLocalFileSystem.SCHEME;

    public static final Class<? extends AbstractFileSystem> DEFAULT_PAGES_VIEW_FS = RawLocalFs.class;
    public static final String DEFAULT_PAGES_VIEW_FS_SCHEME = FsConstants.LOCAL_FS_URI.getScheme();

    //-----VARIABLES-----
    private static final Object txManagerLock = new Object();

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    public static PageStore getPageStore() throws IOException
    {
        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.HDFS_PAGE_STORE)) {
            PageStore pageStore = new SimplePageStore();
            pageStore.init();

            R.cacheManager().getApplicationCache().put(CacheKeys.HDFS_PAGE_STORE, pageStore);
        }

        return (PageStore) R.cacheManager().getApplicationCache().get(CacheKeys.HDFS_PAGE_STORE);
    }
    public static PageIndexer getMainPageIndexer() throws IOException
    {
        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.MAIN_PAGE_INDEX)) {
            Indexer indexer = new LucenePageIndexer();
            getIndexerRegistry().add(indexer);
            R.cacheManager().getApplicationCache().put(CacheKeys.MAIN_PAGE_INDEX, indexer);
        }

        return (PageIndexer) R.cacheManager().getApplicationCache().get(CacheKeys.MAIN_PAGE_INDEX);
    }
    public static PageIndexer getTriplestorePageIndexer() throws IOException
    {
        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.TRIPLESTORE_PAGE_INDEX)) {
            Indexer indexer = new SesamePageIndexer();
            getIndexerRegistry().add(indexer);
            R.cacheManager().getApplicationCache().put(CacheKeys.TRIPLESTORE_PAGE_INDEX, indexer);
        }

        return (PageIndexer) R.cacheManager().getApplicationCache().get(CacheKeys.TRIPLESTORE_PAGE_INDEX);
    }
    public static TransactionManager getTransactionManager() throws IOException
    {
        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.TRANSACTION_MANAGER)) {
            try {
                Map<String, String> extraProperties = Settings.instance().getTransactionsProperties();
                if (extraProperties != null) {
                    for (Map.Entry<String, String> entry : extraProperties.entrySet()) {
                        System.setProperty(entry.getKey(), entry.getValue());
                    }
                }

                TransactionManager transactionManager = Settings.instance().getTransactionManagerClass().newInstance();

                R.cacheManager().getApplicationCache().put(CacheKeys.TRANSACTION_MANAGER, transactionManager);
            }
            catch (Exception e) {
                throw new IOException("Exception caught while booting up the transaction manager", e);
            }
        }

        return (TransactionManager) R.cacheManager().getApplicationCache().get(CacheKeys.TRANSACTION_MANAGER);
    }
    public static RequestTX getCurrentRequestTx() throws IOException
    {
        //Sync this with the release filter code
        if (!R.cacheManager().getRequestCache().containsKey(CacheKeys.REQUEST_TRANSACTION)) {
            try {
                TransactionManager transactionManager = getTransactionManager();
                //start up a new transaction
                transactionManager.begin();
                //fetch the transaction attached to the current thread
                Transaction transaction = transactionManager.getTransaction();
                //wrap it in a request object
                RequestTX cacheEntry = new RequestTX(transaction);

                R.cacheManager().getRequestCache().put(CacheKeys.REQUEST_TRANSACTION, cacheEntry);
            }
            catch (Exception e) {
                throw new IOException("Exception caught while booting up a request transaction; "+R.requestContext().getJaxRsRequest().getUriInfo().getRequestUri(), e);
            }
        }

        return (RequestTX) R.cacheManager().getRequestCache().get(CacheKeys.REQUEST_TRANSACTION);
    }
    public static XASession getCurrentRequestXDiskTx() throws IOException
    {
        RequestTX requestTx = getCurrentRequestTx();
        //start up a new XDisk session if needed
        if (requestTx.getXdiskSession()==null) {
            try {
                //boot a new xadisk
                XASession xdiskSession = getPageStoreTransactionManager().createSessionForXATransaction();
                //hook it into the request tx
                requestTx.registerResource(xdiskSession.getXAResource());
                //save it in our wrapper
                requestTx.setXdiskSession(xdiskSession);
            }
            catch (Exception e) {
                throw new IOException("Exception caught while booting up XADisk transaction during request; "+R.requestContext().getJaxRsRequest().getUriInfo().getRequestUri(), e);
            }
        }

        return requestTx.getXdiskSession();
    }
    public static XAFileSystem getPageStoreTransactionManager() throws IOException
    {
        synchronized (txManagerLock) {
            if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.XADISK_FILE_SYSTEM)) {
                XAFileSystem xafs = XAFileSystemProxy.bootNativeXAFileSystem(new StandaloneFileSystemConfiguration(Settings.instance().getPagesStoreJournalDir().getAbsolutePath(), Settings.instance().getPagesStoreJournalId()));
                try {
                    xafs.waitForBootup(Settings.instance().getPagesStoreJournalBootTimeout());
                    R.cacheManager().getApplicationCache().put(CacheKeys.XADISK_FILE_SYSTEM, xafs);
                }
                catch (InterruptedException e) {
                    throw new IOException("Error occurred whlie booting transactional XADisk file system (timeout=" + Settings.instance().getPagesStoreJournalBootTimeout(), e);
                }
            }

            return (XAFileSystem) R.cacheManager().getApplicationCache().get(CacheKeys.XADISK_FILE_SYSTEM);
        }
    }
    public static boolean rebootPageStoreTransactionManager()
    {
        synchronized (txManagerLock) {
            boolean retVal = false;

            if (R.cacheManager().getApplicationCache().containsKey(CacheKeys.XADISK_FILE_SYSTEM)) {
                XAFileSystem xafs = (XAFileSystem) R.cacheManager().getApplicationCache().get(CacheKeys.XADISK_FILE_SYSTEM);
                //setting it here will ensure it's null internally, even if the next shutdown fails
                R.cacheManager().getApplicationCache().remove(CacheKeys.XADISK_FILE_SYSTEM);
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
    /**
     * @return this returns a NEW filesystem, that needs to be (auto) closed
     */
    public static FileContext getPageStoreFileSystem() throws IOException
    {
        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.HDFS_PAGESTORE_FS_CONFIG)) {
            Configuration conf = new Configuration();
            URI pageStorePath = Settings.instance().getPagesStorePath();
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
            Map<String, String> extraProperties = Settings.instance().getPagesHdfsProperties();
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
            getPageStoreTransactionManager();
        }

        return FileContext.getFileContext((Configuration) R.cacheManager().getApplicationCache().get(CacheKeys.HDFS_PAGESTORE_FS_CONFIG));
    }
    /**
     * @return this returns a NEW filesystem, that needs to be (auto) closed
     */
    public static FileContext getPageViewFileSystem() throws IOException
    {
        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.HDFS_PAGEVIEW_FS_CONFIG)) {
            Configuration conf = new Configuration();
            URI pageViewPath = Settings.instance().getPagesViewPath();
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
            Map<String, String> extraProperties = Settings.instance().getPagesHdfsProperties();
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
    public static Set<Indexer> getIndexerRegistry()
    {
        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.REGISTERED_INDEXERS)) {
            R.cacheManager().getApplicationCache().put(CacheKeys.REGISTERED_INDEXERS, new HashSet<>());
        }

        return (Set<Indexer>) R.cacheManager().getApplicationCache().get(CacheKeys.REGISTERED_INDEXERS);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
