package com.beligum.blocks.filesystem.index.solr;

import com.beligum.blocks.filesystem.index.entries.pages.AbstractIndexSearchResult;
import com.beligum.blocks.filesystem.index.ifaces.IndexEntry;
import com.beligum.blocks.filesystem.index.ifaces.IndexSearchRequest;
import org.apache.solr.client.solrj.response.QueryResponse;

import java.util.Iterator;

public class SolrIndexSearchResult extends AbstractIndexSearchResult
{
    //-----CONSTANTS-----
    //this is the default number of maximum search results that will be returned when no specific value is passed
    public static final int DEFAULT_MAX_SEARCH_RESULTS = 1000;

    //-----VARIABLES-----
    private final QueryResponse response;

    //-----CONSTRUCTORS-----
    public SolrIndexSearchResult(IndexSearchRequest indexSearchRequest, QueryResponse response)
    {
        super(indexSearchRequest.getPageOffset() < 0 ? 0 : indexSearchRequest.getPageOffset(),
              indexSearchRequest.getPageSize() <= 0 ? DEFAULT_MAX_SEARCH_RESULTS : indexSearchRequest.getPageSize(),
              response.getElapsedTime());

        this.response = response;
    }

    //-----PUBLIC METHODS-----
    @Override
    public Integer size()
    {
        return null;
    }
    @Override
    public Long getTotalHits()
    {
        return
    }
    @Override
    public Iterator<IndexEntry> iterator()
    {
        return null;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
