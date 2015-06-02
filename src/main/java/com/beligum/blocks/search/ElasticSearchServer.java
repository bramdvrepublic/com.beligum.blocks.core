package com.beligum.blocks.search;

import com.beligum.base.server.R;
import com.beligum.blocks.base.Blocks;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Created by wouter on 21/05/15.
 */
public class ElasticSearchServer
{
    private static ElasticSearchServer instance;
    private Node node;

    // TODO put settings in config
    private ElasticSearchServer() {
//        ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder();
//        settings.put("node.name", "mot-node");
//        settings.put("path.data", "/Users/wouter/data");
//
//        if (R.configuration().getProduction()) {
//            settings.put("http.enabled", false);
//        } else {
//            settings.put("http.enabled", true);
//        }
//
//        this.node = NodeBuilder.nodeBuilder()
//                          .settings(settings)
//                          .clusterName("mot-cluster")
//                        .data(true).local(true).node();

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

    public String getPageIndexName(Locale locale)
    {
        String retVal = "page";
        if (locale != null && locale != Locale.ROOT) {
            retVal = retVal + "_" + locale.getLanguage();
        }
        return retVal;
    }

    public String getResourceIndexName(Locale locale)
    {
        String retVal = "resource";
        if (locale != null && locale != Locale.ROOT) {
            retVal = retVal + "_" + locale.getLanguage();
        }
        return retVal;
    }
}
