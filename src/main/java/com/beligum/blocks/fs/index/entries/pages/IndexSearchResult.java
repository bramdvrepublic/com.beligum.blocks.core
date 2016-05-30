package com.beligum.blocks.fs.index.entries.pages;

import com.beligum.blocks.fs.index.entries.IndexEntry;

import java.util.List;

/**
 * Created by bram on 5/30/16.
 */
public class IndexSearchResult
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    /**
     * The list of (requested, possibly paged) hits in this result
     */
    private List<IndexEntry> results;
    /**
     * The total number of hits in the entire collection for the search
     */
    private Integer totalHits;

    //-----CONSTRUCTORS-----
    public IndexSearchResult(List<IndexEntry> results)
    {
        this(results, null);
    }
    public IndexSearchResult(List<IndexEntry> results, Integer totalHits)
    {
        this.results = results;
        this.totalHits = totalHits;
    }

    //-----PUBLIC METHODS-----
    public List<IndexEntry> getResults()
    {
        return results;
    }
    public Integer getTotalHits()
    {
        return totalHits;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
