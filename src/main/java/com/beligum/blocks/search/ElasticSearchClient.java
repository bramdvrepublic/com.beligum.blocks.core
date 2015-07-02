package com.beligum.blocks.search;

import com.beligum.blocks.config.BlocksConfig;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.node.Node;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by wouter on 21/05/15.
 */
public class ElasticSearchClient
{
    private static ElasticSearchClient instance;
    private Client client;


    private ElasticSearchClient() {
        Map params = new HashMap();
        params.put("cluster.name", BlocksConfig.instance().getElasticSearchClusterName());
        Settings settings = ImmutableSettings.settingsBuilder().put(params).build();
        this.client = new TransportClient(settings).addTransportAddress(new InetSocketTransportAddress("localhost", 9300));
        if (!this.client.admin().indices().exists(new IndicesExistsRequest(ElasticSearchServer.instance().getPageIndexName(BlocksConfig.instance().getDefaultLanguage()))).actionGet().isExists()) {
            for (Locale locale : BlocksConfig.instance().getLanguages().values()) {
                this.client.admin().indices().prepareCreate(ElasticSearchServer.instance().getPageIndexName(locale)).execute().actionGet();
                this.client.admin().indices().prepareCreate(ElasticSearchServer.instance().getResourceIndexName(locale)).execute().actionGet();
            }
        }
    }

    public static ElasticSearchClient instance()
    {
        if (ElasticSearchClient.instance == null) {
            ElasticSearchClient.instance = new ElasticSearchClient();
        }
        return ElasticSearchClient.instance;
    }

    public Client getClient()
    {
        return client;
    }
}
