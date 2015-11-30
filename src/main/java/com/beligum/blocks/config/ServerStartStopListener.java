package com.beligum.blocks.config;

import com.beligum.base.resources.Asset;
import com.beligum.base.server.R;
import com.beligum.base.server.ifaces.ServerLifecycleListener;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.search.ElasticSearch;
import com.beligum.blocks.templating.blocks.HtmlParser;
import org.eclipse.jetty.server.Server;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.glassfish.jersey.server.spi.Container;

import java.util.HashMap;
import java.util.Map;

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

        //these two are mandatory to launch the connection or the embedded node
        Client esClient;
        if (BlocksConfig.instance().getElasticSearchLaunchEmbedded()) {
            esClient = this.getElasticSearchClient();
        }
        else {
            esClient = ElasticSearch.instance().getClient();
        }
    }
    @Override
    public void onServerStopped(Server server, Container container)
    {
        this.getElasticSearchClient().close();
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private Client getElasticSearchClient()
    {
        Client retVal = null;

        if (BlocksConfig.instance().getElasticSearchLaunchEmbedded()) {
            retVal = this.getElasticSearchNode().client();
        }
        else {
            retVal = ElasticSearch.instance().getClient();
        }

        return retVal;
    }
    private Node getElasticSearchNode()
    {
        Node esNode = (Node) R.cacheManager().getApplicationCache().get(CacheKeys.ELASTIC_SEARCH_NODE);
        if (esNode==null) {
            NodeBuilder nodeBuilder = new NodeBuilder();
            String clusterName = BlocksConfig.instance().getElasticSearchClusterName();

            //don't really know if this is ok, but since we're launching an embedded node, it makes sense to make it local
            boolean isLocalNode = true;
            nodeBuilder.clusterName(clusterName).local(isLocalNode);
            if (!isLocalNode) {
                nodeBuilder.settings().put("http.enabled", true);
            }

            HashMap<String, String> extraProperties = BlocksConfig.instance().getElasticSearchProperties();
            if (extraProperties!=null) {
                for (Map.Entry<String, String> entry : extraProperties.entrySet()) {
                    nodeBuilder.settings().put(entry.getKey(), entry.getValue());
                }
            }

            esNode = nodeBuilder.node();
            R.cacheManager().getApplicationCache().put(CacheKeys.ELASTIC_SEARCH_NODE, esNode);
        }

        return esNode;
    }

}
