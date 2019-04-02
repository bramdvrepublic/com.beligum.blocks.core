package com.beligum.blocks.filesystem.index.sparql;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.filesystem.index.entries.AbstractIndexSearchResult;
import com.beligum.blocks.filesystem.index.ifaces.IndexEntry;
import com.beligum.blocks.filesystem.index.ifaces.IndexSearchRequest;
import org.eclipse.rdf4j.query.BindingSet;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class SparqlIndexSearchResult extends AbstractIndexSearchResult
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private final List<BindingSet> result;

    //-----CONSTRUCTORS-----
    public SparqlIndexSearchResult(List<BindingSet> result, long elapsedTime)
    {
        super(IndexSearchRequest.DEFAULT_PAGE_SIZE, IndexSearchRequest.DEFAULT_MAX_SEARCH_RESULTS, elapsedTime);

        this.result = result;
    }

    //-----PUBLIC METHODS-----
    @Override
    public Integer size()
    {
        return this.result.size();
    }
    @Override
    public Long getTotalHits()
    {
        return Long.valueOf(this.result.size());
    }
    @Override
    public Iterator<IndexEntry> iterator()
    {
        return new SparqlResultIterator(this.result);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----INNER CLASSES-----
    private static class SparqlResultIterator implements Iterator<IndexEntry>
    {
        private final Iterator<BindingSet> solrResultIterator;

        public SparqlResultIterator(List<BindingSet> results)
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
                    //we don't really know the id, here, right?
                    retVal = new SparqlSelectIndexEntry(null, this.solrResultIterator.next());
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
