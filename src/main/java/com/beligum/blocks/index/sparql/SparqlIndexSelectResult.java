package com.beligum.blocks.index.sparql;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.index.entries.AbstractIndexSearchResult;
import com.beligum.blocks.index.ifaces.ResourceIndexEntry;
import com.beligum.blocks.index.ifaces.IndexSearchRequest;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQueryResult;

import java.util.List;
import java.util.NoSuchElementException;

public class SparqlIndexSelectResult extends AbstractIndexSearchResult
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private List<BindingSet> selectResult;

    //-----CONSTRUCTORS-----
    public SparqlIndexSelectResult(TupleQueryResult result, long elapsedTime)
    {
        super(IndexSearchRequest.DEFAULT_PAGE_SIZE, IndexSearchRequest.DEFAULT_MAX_SEARCH_RESULTS, elapsedTime);

        // we "materialize" the query at once, so we have it's size and can close it properly
        this.selectResult = QueryResults.asList(result);
    }

    //-----PUBLIC METHODS-----
    @Override
    public Integer size()
    {
        return this.selectResult.size();
    }
    @Override
    public Long getTotalHits()
    {
        return Long.valueOf(this.selectResult.size());
    }
    @Override
    public java.util.Iterator iterator()
    {
        return new Iterator(this.selectResult);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----INNER CLASSES-----
    private static class Iterator implements java.util.Iterator<ResourceIndexEntry>
    {
        private final java.util.Iterator<BindingSet> bindingIterator;

        public Iterator(List<BindingSet> results)
        {
            this.bindingIterator = results.iterator();
        }

        @Override
        public boolean hasNext()
        {
            return this.bindingIterator.hasNext();
        }
        @Override
        public ResourceIndexEntry next()
        {
            ResourceIndexEntry retVal = null;

            if (this.hasNext()) {
                try {
                    //we don't really know the id, here, right?
                    retVal = new SparqlSelectIndexEntry(null, this.bindingIterator.next());
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
