package com.beligum.blocks.filesystem.index.solr;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.filesystem.index.entries.pages.AbstractIndexSearchResult;
import com.beligum.blocks.filesystem.index.ifaces.IndexEntry;
import com.beligum.blocks.filesystem.index.ifaces.IndexSearchRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

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
                catch (IOException e) {
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
