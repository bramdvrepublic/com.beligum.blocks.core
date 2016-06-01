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
     * The (zero-based) page index of this search result
     */
    private Integer pageIndex;

    /**
     * The maximum number of search results per page
     */
    private Integer pageSize;

    /**
     * The time it took to lookup the result, in milliseconds or null if not set
     */
    private Long searchDuration;

    //-----CONSTRUCTORS-----
    public IndexSearchResult(List<IndexEntry> results)
    {
        this(results, null, null, null, null);
    }
    public IndexSearchResult(List<IndexEntry> results, Integer totalHits, Integer pageIndex, Integer pageSize)
    {
        this(results, totalHits, pageIndex, pageSize, null);
    }
    public IndexSearchResult(List<IndexEntry> results, Integer totalHits, Integer pageIndex, Integer pageSize, Long searchDuration)
    {
        this.results = results;
        this.totalHits = totalHits;
        this.pageIndex = pageIndex;
        this.pageSize = pageSize;
        this.searchDuration = searchDuration;
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
    public Integer getPageIndex()
    {
        return pageIndex;
    }
    public Integer getPageSize()
    {
        return pageSize;
    }
    public Long getSearchDuration()
    {
        return searchDuration;
    }
    public String getSearchDurationSeconds()
    {
        return String.format("%.3f", searchDuration/1000.0f);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
