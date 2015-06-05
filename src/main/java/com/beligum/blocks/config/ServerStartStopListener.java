package com.beligum.blocks.config;

import com.beligum.base.resources.Asset;
import com.beligum.base.server.R;
import com.beligum.base.server.ifaces.ServerLifecycleListener;
import com.beligum.blocks.search.ElasticSearchClient;
import com.beligum.blocks.templating.blocks.HtmlParser;
import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.server.spi.Container;

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
        ElasticSearchClient.instance().getClient();
    }
    @Override
    public void onServerStopped(Server server, Container container)
    {
        ElasticSearchClient.instance().getClient().close();
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
