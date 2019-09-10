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

package com.beligum.blocks.index.results;

import com.beligum.blocks.index.ifaces.ResourceIndexEntry;
import com.beligum.blocks.index.ifaces.IndexSearchResult;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by bram on 5/30/16.
 */
public class SimpleIndexSearchResult extends AbstractIndexSearchResult
{
    //-----CONSTANTS-----
    public static final IndexSearchResult EMPTY_LIST = new SimpleIndexSearchResult(Collections.emptyList(), 0, 0, 0l);

    //-----VARIABLES-----
    /**
     * The result of a lucene query
     */
    private List<ResourceIndexEntry> results;

    //-----CONSTRUCTORS-----
    public SimpleIndexSearchResult(List<ResourceIndexEntry> results)
    {
        this(results, null, null, null);
    }
    public SimpleIndexSearchResult(List<ResourceIndexEntry> results, Integer pageIndex, Integer pageSize, Long searchDuration)
    {
        super(pageIndex, pageSize, searchDuration);

        if (results == null) {
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
    public Long getTotalHits()
    {
        return Long.valueOf(this.size());
    }
    @Override
    public Iterator<ResourceIndexEntry> iterator()
    {
        return Iterators.filter(this.results.iterator(), new SearchResultFilter());
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
