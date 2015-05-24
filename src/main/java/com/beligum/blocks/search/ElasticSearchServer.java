package com.beligum.blocks.search;

import com.beligum.base.server.R;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

/**
 * Created by wouter on 21/05/15.
 */
public class ElasticSearchServer
{
    private static ElasticSearchServer instance;
    private Node node;

    // TODO put settings in config
    private ElasticSearchServer() {
        ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder();
        settings.put("node.name", "mot-node");
        settings.put("path.data", "/Users/wouter/data");

        if (R.configuration().getProduction()) {
            settings.put("http.enabled", false);
        } else {
            settings.put("http.enabled", true);
        }

        this.node = NodeBuilder.nodeBuilder()
                          .settings(settings)
                          .clusterName("mot-cluster")
                        .data(true).local(true).node();

    }

    public static ElasticSearchServer instance()
    {
        if (ElasticSearchServer.instance == null) {
            ElasticSearchServer.instance = new ElasticSearchServer();
        }
        return ElasticSearchServer.instance;
    }

    public Node getNode()
    {
        return node;
    }
}
