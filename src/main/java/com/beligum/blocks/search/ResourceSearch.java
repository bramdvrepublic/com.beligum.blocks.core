package com.beligum.blocks.search;

import java.util.Locale;

/**
 * Created by wouter on 3/09/15.
 */
public class ResourceSearch extends AbstractSearch
{
    public ResourceSearch() {
        this.countBuilder = ElasticSearch.instance().getClient().prepareCount(ElasticSearch.instance().getResourceIndexName(Locale.ROOT));
        this.builder = ElasticSearch.instance().getClient().prepareSearch(ElasticSearch.instance().getResourceIndexName(Locale.ROOT));
    }
}
