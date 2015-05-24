package com.beligum.blocks.base;

import com.beligum.base.server.ifaces.ServerLifecycleListener;
import com.beligum.blocks.models.jsonld.OrientResourceFactory;
import com.beligum.blocks.search.ElasticSearchClient;
import com.beligum.blocks.search.ElasticSearchServer;
import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.server.spi.Container;

/**
 * Created by wouter on 15/05/15.
 */
public class ServerStartStopListener implements ServerLifecycleListener
{
    @Override
    public void onServerStarted(Server server, Container container)
    {
        ElasticSearchServer.instance().getNode();
        ElasticSearchClient.instance().getClient();
    }

    @Override
    public void onServerStopped(Server server, Container container)
    {

        ElasticSearchClient.instance().getClient().close();
        ElasticSearchServer.instance().getNode().close();
        OrientResourceFactory.instance().stopServer();
    }
}
