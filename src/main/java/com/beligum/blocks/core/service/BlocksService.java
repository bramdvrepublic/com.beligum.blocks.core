package com.beligum.blocks.core.service;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.dynamic.DynamicBlockHandler;
//import org.apache.solr.client.solrj.impl.HttpSolrClient;

/**
 * Created by wouter on 3/03/15.
 */
public class BlocksService
{
    private static BlocksService instance;

//    private HttpSolrClient solrClient;
//
//    public HttpSolrClient getSolrClient() {
//        return solrClient;
//    }

    public void registerBlock(DynamicBlockHandler listener) {

    }

    public void unregisterBlock(DynamicBlockHandler listener) {

    }

    private BlocksService() {
//        this.solrClient = new HttpSolrClient(BlocksConfig.getSolrServerUrl());
    }

    public static BlocksService instance() {
        if (BlocksService.instance == null) {
            BlocksService.instance = new BlocksService();
        }
        return BlocksService.instance;
    }



}
