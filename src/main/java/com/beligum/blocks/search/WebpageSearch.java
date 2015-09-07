package com.beligum.blocks.search;

import java.util.Locale;

/**
 * Created by wouter on 3/09/15.
 */
public class WebpageSearch extends AbstractSearch
{
    public WebpageSearch() {
        this.countBuilder = ElasticSearch.instance().getClient().prepareCount(ElasticSearch.instance().getPageIndexName(Locale.ROOT));
        this.builder = ElasticSearch.instance().getClient().prepareSearch(ElasticSearch.instance().getPageIndexName(Locale.ROOT));
    }
}
