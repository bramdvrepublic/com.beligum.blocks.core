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
