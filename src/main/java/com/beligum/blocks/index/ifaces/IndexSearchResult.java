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

package com.beligum.blocks.index.ifaces;

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
    Long getTotalHits();

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
