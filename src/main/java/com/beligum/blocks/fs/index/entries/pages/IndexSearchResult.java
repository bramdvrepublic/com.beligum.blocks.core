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
     * The total number of hits in the entire collection for the search or null if not set
     */
    private Integer totalHits;

    /**
     * The time it took to lookup the result, in milliseconds or null if not set
     */
    private Long lookupDuration;

    //-----CONSTRUCTORS-----
    public IndexSearchResult(List<IndexEntry> results)
    {
        this(results, null, null);
    }
    public IndexSearchResult(List<IndexEntry> results, Integer totalHits)
    {
        this(results, totalHits, null);
    }
    public IndexSearchResult(List<IndexEntry> results, Integer totalHits, Long lookupDuration)
    {
        this.results = results;
        this.totalHits = totalHits;
        this.lookupDuration = lookupDuration;
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
    public Long getLookupDuration()
    {
        return lookupDuration;
    }
    public void setLookupDuration(Long lookupDuration)
    {
        this.lookupDuration = lookupDuration;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
