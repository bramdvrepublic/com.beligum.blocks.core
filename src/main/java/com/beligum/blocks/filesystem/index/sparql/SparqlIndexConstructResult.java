package com.beligum.blocks.filesystem.index.sparql;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.filesystem.index.entries.AbstractIndexSearchResult;
import com.beligum.blocks.filesystem.index.ifaces.IndexEntry;
import com.beligum.blocks.filesystem.index.ifaces.IndexSearchRequest;
import com.beligum.blocks.utils.RdfTools;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryResults;

import java.util.NoSuchElementException;
import java.util.Set;

public class SparqlIndexConstructResult extends AbstractIndexSearchResult
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private Model model;
    private Set<Resource> subjects;

    //-----CONSTRUCTORS-----
    public SparqlIndexConstructResult(GraphQueryResult result, long elapsedTime)
    {
        super(IndexSearchRequest.DEFAULT_PAGE_SIZE, IndexSearchRequest.DEFAULT_MAX_SEARCH_RESULTS, elapsedTime);

        // we "materialize" the query at once, so we have it's size and can close it properly
        this.model = QueryResults.asModel(result);

        //we'll assume every distinct subject in the model is a new result
        this.subjects = this.model.subjects();
    }

    //-----PUBLIC METHODS-----
    @Override
    public Integer size()
    {
        return this.subjects.size();
    }
    @Override
    public Long getTotalHits()
    {
        return Long.valueOf(this.subjects.size());
    }
    @Override
    public java.util.Iterator iterator()
    {
        return new Iterator(this.subjects, this.model);
    }
    public Model getModel()
    {
        return model;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----INNER CLASSES-----
    private static class Iterator implements java.util.Iterator<IndexEntry>
    {
        private final java.util.Iterator<Resource> subjectIterator;
        private final Model model;

        public Iterator(Set<Resource> subjects, Model model)
        {
            this.subjectIterator = subjects.iterator();
            this.model = model;
        }

        @Override
        public boolean hasNext()
        {
            return this.subjectIterator.hasNext();
        }
        @Override
        public IndexEntry next()
        {
            IndexEntry retVal = null;

            if (this.hasNext()) {
                try {
                    IRI subject = (IRI) this.subjectIterator.next();
                    retVal = new SparqlConstructIndexEntry(RdfTools.iriToUri(subject), this.model.filter(subject, null, null));
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
