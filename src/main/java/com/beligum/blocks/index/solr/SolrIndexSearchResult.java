package com.beligum.blocks.index.solr;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.index.entries.AbstractIndexSearchResult;
import com.beligum.blocks.index.ifaces.IndexEntry;
import com.beligum.blocks.index.ifaces.IndexSearchRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import java.util.NoSuchElementException;

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
    public java.util.Iterator iterator()
    {
        return new Iterator(this.response.getResults());
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----INNER CLASSES-----
    private static class Iterator implements java.util.Iterator<IndexEntry>
    {
        private final java.util.Iterator<SolrDocument> solrResultIterator;

        public Iterator(SolrDocumentList results)
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
