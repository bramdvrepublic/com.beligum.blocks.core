package com.beligum.blocks.config;

import com.beligum.base.resources.Asset;
import com.beligum.base.server.R;
import com.beligum.base.server.ifaces.ServerLifecycleListener;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.search.ElasticSearch;
import com.beligum.blocks.templating.blocks.HtmlParser;
import org.apache.jena.query.Dataset;
import org.eclipse.jetty.io.RuntimeIOException;
import org.eclipse.jetty.server.Server;
import org.elasticsearch.client.Client;
import org.glassfish.jersey.server.spi.Container;
import org.xadisk.bridge.proxies.interfaces.XAFileSystem;

import java.io.IOException;

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
        R.resourceLoader().registerAssetParser(Asset.MimeType.HTML, new HtmlParser());

        //this will boot the transaction manager (and possibly do a restore)
        try {
            Settings.instance().getPageStoreTransactionManager();
        }
        catch (IOException e) {
            throw new RuntimeIOException("Unable to boot the page store transaction manager during starup, this is bad and I can't proceed", e);
        }

        //this will launch/connect to the ES server
        Client esClient = ElasticSearch.instance().getClient();
    }
    @Override
    public void onServerStopped(Server server, Container container)
    {
        //don't boot it up if it's not there
        if (R.cacheManager().getApplicationCache().containsKey(CacheKeys.XADISK_FILE_SYSTEM)) {
            try {
                XAFileSystem xafs = Settings.instance().getPageStoreTransactionManager();
                xafs.shutdown();
            }
            catch (IOException e) {
                Logger.error("Exception caught while shutting down XADisk", e);
            }
        }

        if (R.cacheManager().getApplicationCache().containsKey(CacheKeys.RDF_DATASET)) {
            Dataset dataset = Settings.instance().getRDFDataset();
            dataset.close();
        }

        ElasticSearch.instance().getClient().close();
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
