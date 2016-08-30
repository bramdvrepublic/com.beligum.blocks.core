package com.beligum.blocks.config;

import bitronix.tm.BitronixTransactionManager;
import com.beligum.base.resources.ifaces.Resource;
import com.beligum.base.server.R;
import com.beligum.base.server.ifaces.ServerLifecycleListener;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.endpoints.PageEndpoint;
import com.beligum.blocks.fs.hdfs.bitronix.XAResourceProducer;
import com.beligum.blocks.fs.index.ifaces.Indexer;
import com.beligum.blocks.templating.blocks.HtmlParser;
import org.eclipse.jetty.io.RuntimeIOException;
import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.server.spi.Container;
import org.xadisk.bridge.proxies.interfaces.XAFileSystem;

import javax.transaction.TransactionManager;
import java.io.IOException;
import java.util.Iterator;

/**
 * Created by bram on 11/10/14.
 */
public class ServerStartStopListener implements ServerLifecycleListener
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    public void onServerStarted(Server server, Container container)
    {
        if (Settings.instance().hasBlocksCoreConfig()) {
            //let all .html files pass through our HtmlParser
            R.resourceFactory().registerParser(Resource.MimeType.HTML, new HtmlParser());

            //we might as well pre-load the templates here
            HtmlParser.getTemplateCache();

            //this will boot the xadisk transaction manager (and possibly do a restore)
            try {
                StorageFactory.getPageStoreTransactionManager();
            }
            catch (Exception e) {
                throw new RuntimeIOException("Unable to boot the page store transaction manager during starup, this is bad and I can't proceed", e);
            }

            //boot up all the static RDF fields
            RdfFactory.assertInitialized();
        }
    }
    @Override
    public void onServerStopped(Server server, Container container)
    {
        if (Settings.instance().hasBlocksCoreConfig()) {

            //start all possible running async tasks
            PageEndpoint.endAllAsyncTasksNow();

            //don't boot it up if it's not there
            if (R.cacheManager().getApplicationCache().containsKey(CacheKeys.XADISK_FILE_SYSTEM)) {
                try {
                    XAFileSystem xafs = StorageFactory.getPageStoreTransactionManager();
                    xafs.shutdown();
                }
                catch (IOException e) {
                    Logger.error("Exception caught while shutting down XADisk", e);
                }
            }

            Iterator<Indexer> indexIter = StorageFactory.getIndexerRegistry().iterator();
            while (indexIter.hasNext()) {
                Indexer indexer = indexIter.next();
                try {
                    indexer.shutdown();
                }
                catch (IOException e) {
                    Logger.error("Exception caught while shutting down indexer; " + indexer, e);
                }
                indexIter.remove();
            }

            if (R.cacheManager().getApplicationCache().containsKey(CacheKeys.TRANSACTION_MANAGER)) {
                try {
                    TransactionManager transactionManager = StorageFactory.getTransactionManager();
                    if (transactionManager instanceof BitronixTransactionManager) {
                        BitronixTransactionManager bitronixTransactionManager = (BitronixTransactionManager) transactionManager;
                        //we need to explicitly shut down the manually registered ones
                        XAResourceProducer.shutdown();
                        bitronixTransactionManager.shutdown();
                    }
                    else {
                        //TODO no public close method on a transaction manager??
                    }
                }
                catch (IOException e) {
                    Logger.error("Error while shutting down transaction manager", e);
                }
            }
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
