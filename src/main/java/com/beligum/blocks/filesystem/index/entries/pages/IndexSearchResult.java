package com.beligum.blocks.filesystem.index.entries.pages;

import com.beligum.blocks.filesystem.index.entries.IndexEntry;

/**
 * Created by bram on 6/3/17.
 */
public interface IndexSearchResult extends Iterable<IndexEntry>
{
    /**
     * @return true if the size > 0
     */
    boolean isEmpty();

    /**
     * @return the size of this iterator
     */
    Integer size();

    /**
     * @return the total number of hits for this query (regardless of paging)
     */
    Integer getTotalHits();


    /**
     * @return the zero-based page index for this result set
     */
    Integer getPageIndex();

    /**
     * @return the size of the page that was used to generate this result set
     */
    Integer getPageSize();

    /**
     * @return the number of milliseconds it took to perform this query
     */
    Long getSearchDuration();

    /**
     * @return the same as above, but in human-readable seconds
     */
    String getSearchDurationSeconds();

    /**
     * @return the same set of results, but sorted alphabetically on the title of the entries.
     */
    Iterable<IndexEntry> getAlphaSortedResults();
}
