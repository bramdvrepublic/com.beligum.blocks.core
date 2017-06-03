package com.beligum.blocks.filesystem.index.entries.pages;

import com.beligum.blocks.filesystem.index.entries.IndexEntry;

import java.util.Iterator;
import java.util.List;

/**
 * Created by bram on 5/30/16.
 */
public class SimpleIndexSearchResult extends AbstractIndexSearchResult
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    /**
     * The result of a lucene query
     */
    private List<IndexEntry> results;

    //-----CONSTRUCTORS-----
    public SimpleIndexSearchResult(List<IndexEntry> results)
    {
        this(results, null, null, null);
    }
    public SimpleIndexSearchResult(List<IndexEntry> results, Integer pageIndex, Integer pageSize, Long searchDuration)
    {
        super(pageIndex, pageSize, searchDuration);

        if (results==null) {
            throw new IllegalArgumentException("It's not possible to create a search result with a null array list");
        }
        else {
            this.results = results;
        }
    }

    //-----PUBLIC METHODS-----
    @Override
    public Integer size()
    {
        return this.results.size();
    }
    @Override
    public Integer getTotalHits()
    {
        return this.size();
    }
    @Override
    public Iterator<IndexEntry> iterator()
    {
        return this.results.iterator();
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
