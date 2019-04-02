package com.beligum.blocks.filesystem.index.solr;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.filesystem.index.entries.AbstractIndexSearchResult;
import com.beligum.blocks.filesystem.index.ifaces.IndexEntry;
import com.beligum.blocks.filesystem.index.ifaces.IndexSearchRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static com.beligum.blocks.filesystem.index.ifaces.IndexSearchRequest.DEFAULT_MAX_SEARCH_RESULTS;

public class SolrIndexSearchResult extends AbstractIndexSearchResult
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private final QueryResponse response;

    //-----CONSTRUCTORS-----
    public SolrIndexSearchResult(QueryResponse response)
    {
        super(IndexSearchRequest.DEFAULT_PAGE_OFFSET, IndexSearchRequest.DEFAULT_PAGE_SIZE, response.getElapsedTime());

        this.response = response;
    }
    public SolrIndexSearchResult(IndexSearchRequest indexSearchRequest, QueryResponse response)
    {
        super(indexSearchRequest.getPageOffset(), indexSearchRequest.getPageSize(), response.getElapsedTime());

        this.response = response;
    }

    //-----PUBLIC METHODS-----
    @Override
    public Integer size()
    {
        return this.response.getResults().size();
    }
    @Override
    public Long getTotalHits()
    {
        return this.response.getResults().getNumFound();
    }
    @Override
    public Iterator<IndexEntry> iterator()
    {
        return new SolrResultIterator(this.response.getResults());
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----INNER CLASSES-----
    private static class SolrResultIterator implements Iterator<IndexEntry>
    {
        private final Iterator<SolrDocument> solrResultIterator;

        public SolrResultIterator(SolrDocumentList results)
        {
            this.solrResultIterator = results.iterator();
        }

        @Override
        public boolean hasNext()
        {
            return this.solrResultIterator.hasNext();
        }
        @Override
        public IndexEntry next()
        {
            IndexEntry retVal = null;

            if (this.hasNext()) {
                try {
                    //TODO this is not very efficient since it re-parses the entire json string
                    retVal = new SolrPageIndexEntry(this.solrResultIterator.next().jsonStr());
                }
                catch (Exception e) {
                    Logger.error("Error while fetching the next search result; ", e);
                }
            }
            else {
                throw new NoSuchElementException();
            }

            return retVal;
        }
    }
}
