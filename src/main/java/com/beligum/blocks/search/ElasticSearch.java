package com.beligum.blocks.search;

import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.controllers.interfaces.PersistenceController;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by wouter on 21/05/15.
 */
public class ElasticSearch
{
    private Client client;
    private BulkRequestBuilder bulkRequestBuilder;

    private ElasticSearch()
    {
        if (BlocksConfig.instance().getElasticSearchLaunchEmbedded()) {
            this.client = this.getEmbeddedNode().client();
        }
        else {
            this.client = this.buildRemoteClient();
        }

        try {
            init();
        }
        catch (IOException e) {
            throw new RuntimeException("Caught error while launching Elastic Search; this is bad so I'm aborting", e);
        }
    }

    public static ElasticSearch instance()
    {
        ElasticSearch instance = (ElasticSearch) R.cacheManager().getApplicationCache().get(CacheKeys.ELASTIC_SEARCH_INSTANCE);
        if (instance==null) {
            R.cacheManager().getApplicationCache().put(CacheKeys.ELASTIC_SEARCH_INSTANCE, instance = new ElasticSearch());
        }

        return instance;
    }

    public Client getClient()
    {
        return client;
    }

    public String getPageIndexName(Locale locale)
    {
//        String retVal = "page";
//        if (locale != null && locale != Locale.ROOT) {
//            retVal = retVal + "_" + locale.getLanguage();
//        }

        return "page";
    }

    public String getResourceIndexName(Locale locale)
    {
//        String retVal = "resource";
//        if (locale != null && locale != Locale.ROOT) {
//            retVal = retVal + "_" + locale.getLanguage();
//        }

        return "resource";
    }

    // Start a bulk transaction for this request
    public BulkRequestBuilder getBulk()
    {
        BulkRequestBuilder bulk = getBulkFromCache();
        if (bulk == null) {
            setBulkInCache();
        }
        return getBulkFromCache();
    }

    // save bulk transaction for this request if one exists
    public BulkResponse saveBulk()
    {
        BulkResponse retVal = null;
        if (getBulkFromCache() != null) {
            retVal = getBulkFromCache().execute().actionGet();
            if (retVal.hasFailures()) {
                Logger.error("failed to index all resources in document store");
            }
            removeBulkFromCache();
        }
        return retVal;
    }
    public void flush() throws IOException
    {
        this.client.admin().indices().delete(new DeleteIndexRequest("*")).actionGet();
        this.reset();
    }

    // -------- PRIVATE METHODS -----------
    private Node getEmbeddedNode()
    {
        Node esNode = (Node) R.cacheManager().getApplicationCache().get(CacheKeys.ELASTIC_SEARCH_NODE);
        if (esNode==null) {
            NodeBuilder nodeBuilder = new NodeBuilder();
            String clusterName = BlocksConfig.instance().getElasticSearchClusterName();

            //don't really know if this is ok, but since we're launching an embedded node, it makes sense to make it local
            final boolean isLocalNode = true;
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
    private Client buildRemoteClient()
    {
        Map params = new HashMap();
        params.put("cluster.name", BlocksConfig.instance().getElasticSearchClusterName());

        HashMap<String, String> extraProperties = BlocksConfig.instance().getElasticSearchProperties();
        if (extraProperties!=null) {
            for (Map.Entry<String, String> entry : extraProperties.entrySet()) {
                params.put(entry.getKey(), entry.getValue());
            }
        }

        Settings settings = ImmutableSettings.settingsBuilder().put(params).build();
        String hostname = BlocksConfig.instance().getElasticSearchHostName();
        Integer port = BlocksConfig.instance().getElasticSearchPort();

        return new TransportClient(settings).addTransportAddress(new InetSocketTransportAddress(hostname, port));
    }
    private BulkRequestBuilder getBulkFromCache()
    {
        return this.bulkRequestBuilder;
    }
    private void setBulkInCache()
    {
        bulkRequestBuilder = this.getClient().prepareBulk();
    }
    private void removeBulkFromCache()
    {
        bulkRequestBuilder = null;
    }
    private void reset() throws IOException
    {
        init();
    }
    private void init() throws IOException
    {
        // TODO: should check for all indexes. e.g. when new language is created we don't have to remove all indexes
        if (!this.client.admin().indices().exists(new IndicesExistsRequest(getPageIndexName(BlocksConfig.instance().getDefaultLanguage()))).actionGet().isExists()) {
            // Delete all to start fresh
            this.client.admin().indices().delete(new DeleteIndexRequest("*")).actionGet();

            ClassLoader classLoader = getClass().getClassLoader();
            String resourceMapping = IOUtils.toString(classLoader.getResourceAsStream("elastic/resource.json"));
            String pageMapping = IOUtils.toString(classLoader.getResourceAsStream("elastic/page.json"));
            String pathMapping = IOUtils.toString(classLoader.getResourceAsStream("elastic/path.json"));
            String settings = IOUtils.toString(classLoader.getResourceAsStream("elastic/settings.json"));

            this.client.admin().indices().prepareCreate(this.getPageIndexName(Locale.ROOT)).setSettings(settings)
                       .addMapping(PersistenceController.WEB_PAGE_CLASS.toLowerCase(),
                                   pageMapping).execute().actionGet();
            this.client.admin().indices().prepareCreate(this.getResourceIndexName(Locale.ROOT)).setSettings(settings)
                       .addMapping("_default_", resourceMapping).execute()
                       .actionGet();
            this.client.admin().indices().prepareCreate(PersistenceController.PATH_CLASS).setSettings(settings).addMapping(PersistenceController.PATH_CLASS, pathMapping)
                       .execute()
                       .actionGet();
        }
    }
}
