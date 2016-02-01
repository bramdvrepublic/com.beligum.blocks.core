package com.beligum.blocks.config;

import com.beligum.base.resources.Asset;
import com.beligum.base.server.R;
import com.beligum.base.server.ifaces.ServerLifecycleListener;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.search.ElasticSearch;
import com.beligum.blocks.templating.blocks.HtmlParser;
import org.eclipse.jetty.server.Server;
import org.elasticsearch.client.Client;
import org.glassfish.jersey.server.spi.Container;
import org.xadisk.bridge.proxies.interfaces.Session;

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

        //this will launch/connect to the ES server
        Client esClient = ElasticSearch.instance().getClient();
    }
    @Override
    public void onServerStopped(Server server, Container container)
    {
        Session tx = (Session) R.cacheManager().getRequestCache().get(CacheKeys.XADISK_REQUEST_TRANSACTION);
        if (tx != null) {

        }

        ElasticSearch.instance().getClient().close();
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
