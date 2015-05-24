package com.beligum.blocks.search;

import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;

/**
 * Created by wouter on 21/05/15.
 */
public class ElasticSearchClient
{
    private static ElasticSearchClient instance;
    private Client client;


    private ElasticSearchClient() {
        Node node = ElasticSearchServer.instance().getNode();

        this.client = node.client();
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
