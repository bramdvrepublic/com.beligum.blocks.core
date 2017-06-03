package com.beligum.blocks.filesystem.index.entries.pages;

import com.beligum.blocks.filesystem.index.entries.IndexEntry;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by bram on 5/30/16.
 */
public abstract class AbstractIndexSearchResult implements IndexSearchResult
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
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

    /**
     * The lazy-loaded alphabetically sorted list of (requested, possibly paged) hits in this result.
     * Note: this is mainly needed for the letter-format result box, which needs to be sorted alphabetically,
     * no matter what was configured by the search box.
     */
    private List<IndexEntry> cachedAlphaSortedResults;

    //-----CONSTRUCTORS-----
    public AbstractIndexSearchResult(Integer pageIndex, Integer pageSize, Long searchDuration)
    {
        this.pageIndex = pageIndex;
        this.pageSize = pageSize;
        this.searchDuration = searchDuration;
    }

    //-----PUBLIC METHODS-----
    @Override
    public boolean isEmpty()
    {
        return this.size() <= 0;
    }
    @Override
    public Integer getPageIndex()
    {
        return pageIndex;
    }
    @Override
    public Integer getPageSize()
    {
        return pageSize;
    }
    @Override
    public Long getSearchDuration()
    {
        return searchDuration;
    }
    @Override
    public String getSearchDurationSeconds()
    {
        return searchDuration == null ? null : String.format("%.3f", searchDuration / 1000.0f);
    }
    @Override
    public Iterable<IndexEntry> getAlphaSortedResults()
    {
        if (this.cachedAlphaSortedResults == null) {
            this.cachedAlphaSortedResults = Lists.newArrayList(this);
            Collections.sort(this.cachedAlphaSortedResults, new Comparator<IndexEntry>()
            {
                @Override
                public int compare(IndexEntry o1, IndexEntry o2)
                {
                    if (o1.getTitle() == null) {
                        return -1;
                    }
                    if (o2.getTitle() == null) {
                        return 1;
                    }

                    return o1.getTitle().compareTo(o2.getTitle());
                }
            });
        }

        return this.cachedAlphaSortedResults;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
