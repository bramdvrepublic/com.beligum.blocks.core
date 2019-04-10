/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.beligum.blocks.index.entries;

import com.beligum.blocks.index.ifaces.ResourceIndexEntry;
import com.beligum.blocks.index.ifaces.IndexSearchResult;
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
    private List<ResourceIndexEntry> cachedAlphaSortedResults;

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
    public Iterable<ResourceIndexEntry> getAlphaSortedResults()
    {
        if (this.cachedAlphaSortedResults == null) {
            this.cachedAlphaSortedResults = Lists.newArrayList(this);
            Collections.sort(this.cachedAlphaSortedResults, new Comparator<ResourceIndexEntry>()
            {
                @Override
                public int compare(ResourceIndexEntry o1, ResourceIndexEntry o2)
                {
                    if (o1.getLabel() == null) {
                        return -1;
                    }
                    if (o2.getLabel() == null) {
                        return 1;
                    }

                    return o1.getLabel().compareTo(o2.getLabel());
                }
            });
        }

        return this.cachedAlphaSortedResults;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
