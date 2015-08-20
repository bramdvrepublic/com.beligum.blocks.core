package com.beligum.blocks.search;

import com.beligum.base.cache.CacheKey;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by wouter on 21/05/15.
 */
public class ElasticSearch
{
    private static ElasticSearch instance;
    private Client client;
    private BulkRequestBuilder bulkRequestBuilder;


    public enum ESCacheKeys implements CacheKey
    {
        BULK_REQUEST
    }



    private ElasticSearch() {
        Map params = new HashMap();
        params.put("cluster.name", BlocksConfig.instance().getElasticSearchClusterName());
        String host = BlocksConfig.instance().getElasticSearchHostName();
        Integer port = BlocksConfig.instance().getElasticSearchPort();
        Settings settings = ImmutableSettings.settingsBuilder().put(params).build();
        this.client = new TransportClient(settings).addTransportAddress(new InetSocketTransportAddress(host, port));
        init();
    }

    public static ElasticSearch instance()
    {
        if (ElasticSearch.instance == null) {
            ElasticSearch.instance = new ElasticSearch();
        }
        return ElasticSearch.instance;
    }

    public Client getClient()
    {
        return client;
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

    // Start a bulk transaction for this request
    public BulkRequestBuilder getBulk() {
        BulkRequestBuilder bulk = getBulkFromCache();
        if (bulk == null) {
            setBulkInCache();
        }
        return getBulkFromCache();
    }

    // save bulk transaction for this request if one exists
    public BulkResponse saveBulk() {
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


    // -------- PRIVATE METHODS -----------

    private BulkRequestBuilder getBulkFromCache() {
        return this.bulkRequestBuilder;
    }

    private void setBulkInCache() {
        bulkRequestBuilder =  this.getClient().prepareBulk();
    }

    private void removeBulkFromCache() {
        bulkRequestBuilder = null;
    }

    private void init() {
        // TODO: should check for all indexes. e.g. when new language is created we don't have to remove all indexes
        if (!this.client.admin().indices().exists(new IndicesExistsRequest(getPageIndexName(BlocksConfig.instance().getDefaultLanguage()))).actionGet().isExists()) {
            // Delete all to start fresh
            this.client.admin().indices().delete(new DeleteIndexRequest("*")).actionGet();

            ClassLoader classLoader = getClass().getClassLoader();
            String resourceMapping = null;
            String pageMapping = null;
            String pathMapping = null;
            String settings = null;
            try {
                resourceMapping = IOUtils.toString(classLoader.getResourceAsStream("elastic/resource.json"));
                pageMapping = IOUtils.toString(classLoader.getResourceAsStream("elastic/page.json"));
                pathMapping = IOUtils.toString(classLoader.getResourceAsStream("elastic/path.json"));
                settings = IOUtils.toString(classLoader.getResourceAsStream("elastic/settings.json"));
            }
            catch (Exception e) {
                Logger.error("Could not read mapings for elastic search", e);
            }

            for (Locale locale : BlocksConfig.instance().getLanguages().values()) {
                this.client.admin().indices().prepareCreate(this.getPageIndexName(locale)).setSettings(settings)
                             .addMapping(PersistenceController.WEB_PAGE_CLASS.toLowerCase(),
                                         pageMapping).execute().actionGet();
                this.client.admin().indices().prepareCreate(this.getResourceIndexName(locale)).setSettings(settings)
                             .addMapping("_default_", resourceMapping).execute()
                             .actionGet();
            }

            this.client.admin().indices().prepareCreate(PersistenceController.PATH_CLASS).setSettings(settings).addMapping(PersistenceController.PATH_CLASS, pathMapping)
                         .execute()
                         .actionGet();
        }
    }

    private void reset()
    {


        init();
    }

    public void flush() {
        this.client.admin().indices().delete(new DeleteIndexRequest("*")).actionGet();
        this.reset();
    }

}
