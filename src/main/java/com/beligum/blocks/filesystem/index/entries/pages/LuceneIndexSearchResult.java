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

import com.beligum.base.utils.Logger;
import com.beligum.blocks.filesystem.index.entries.IndexEntry;
import com.google.common.collect.Sets;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by bram on 5/30/16.
 */
public class LuceneIndexSearchResult extends AbstractIndexSearchResult
{
    //-----CONSTANTS-----
    private static final Set<String> INDEX_FIELDS_TO_LOAD = Sets.newHashSet(PageIndexEntry.Field.object.name());

    //-----VARIABLES-----
    /**
     * The searcher used to produce the result set
     */
    private IndexSearcher indexSearcher;

    /**
     * The result of a lucene query
     */
    private TopDocs results;

    /**
     * The internal pointer
     */
    private int index;

    //-----CONSTRUCTORS-----
    public LuceneIndexSearchResult(IndexSearcher indexSearcher, TopDocs results, Integer pageIndex, Integer pageSize, Long searchDuration)
    {
        super(pageIndex, pageSize, searchDuration);

        this.indexSearcher = indexSearcher;
        this.results = results;
        this.index = 0;
    }

    //-----PUBLIC METHODS-----
    @Override
    public Integer size()
    {
        return this.results.scoreDocs.length;
    }
    @Override
    public Integer getTotalHits()
    {
        return this.results.totalHits;
    }
    @Override
    public Iterator<IndexEntry> iterator()
    {
        return new LuceneResultIterator(this.indexSearcher, this.results);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----INNER CLASSES-----
    private static class LuceneResultIterator implements Iterator<IndexEntry>
    {
        /**
         * The searcher used to produce the result set
         */
        private IndexSearcher indexSearcher;

        /**
         * The result of a lucene query
         */
        private TopDocs results;

        /**
         * The internal pointer
         */
        private int index;

        public LuceneResultIterator(IndexSearcher indexSearcher, TopDocs results)
        {
            this.indexSearcher = indexSearcher;
            this.results = results;
            this.index = 0;
        }

        @Override
        public boolean hasNext()
        {
            return this.index < this.results.scoreDocs.length;
        }
        @Override
        public IndexEntry next()
        {
            IndexEntry retVal = null;

            if (this.hasNext()) {
                try {
                    retVal = SimplePageIndexEntry.fromLuceneDoc(this.indexSearcher.doc(this.results.scoreDocs[this.index].doc, INDEX_FIELDS_TO_LOAD));
                    this.index++;
                }
                catch (IOException e) {
                    Logger.error("Error while preparing the next search result (index "+this.index+"); ", e);
                }
            }

            return retVal;
        }
    }
}