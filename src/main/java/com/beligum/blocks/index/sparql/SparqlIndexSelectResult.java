package com.beligum.blocks.index.sparql;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.index.results.AbstractIndexSearchResult;
import com.beligum.blocks.index.ifaces.IndexSearchRequest;
import com.beligum.blocks.index.results.SearchResultFilter;
import com.google.common.collect.Iterators;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQueryResult;

import java.util.List;
import java.util.NoSuchElementException;

public class SparqlIndexSelectResult extends AbstractIndexSearchResult<SparqlSelectIndexEntry>
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private List<BindingSet> selectResult;
    private List<String> bindingNames;


    //-----CONSTRUCTORS-----
    // Note that the result is properly closed by the caller, no need to do it here
    public SparqlIndexSelectResult(TupleQueryResult result, long elapsedTime)
    {
        super(IndexSearchRequest.DEFAULT_PAGE_OFFSET, IndexSearchRequest.DEFAULT_PAGE_SIZE, elapsedTime);

        // we "materialize" the query at once, so we have it's size and can close it properly after calling this constructor
        this.selectResult = QueryResults.asList(result);
        if(result != null){
            this.bindingNames = result.getBindingNames();
        }
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
    public java.util.Iterator<SparqlSelectIndexEntry> iterator()
    {
        return Iterators.filter(new SparqlSelectIterator(this.selectResult), new SearchResultFilter());
    }
    /**
     * @return the binding names of the result in order of projection
     * https://www.w3.org/TR/sparql11-query/#selectproject
     */
    public List<String>getBindingNames(){
        return this.bindingNames;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----INNER CLASSES-----
    private static class SparqlSelectIterator implements java.util.Iterator<SparqlSelectIndexEntry>
    {
        private final java.util.Iterator<BindingSet> bindingIterator;

        SparqlSelectIterator(List<BindingSet> results)
        {
            this.bindingIterator = results.iterator();
        }

        @Override
        public boolean hasNext()
        {
            return this.bindingIterator.hasNext();
        }
        @Override
        public SparqlSelectIndexEntry next()
        {
            SparqlSelectIndexEntry retVal = null;

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
