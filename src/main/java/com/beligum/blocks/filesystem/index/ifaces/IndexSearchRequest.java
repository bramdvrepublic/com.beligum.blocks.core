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

package com.beligum.blocks.filesystem.index.ifaces;

import com.beligum.blocks.filesystem.index.solr.SolrIndexSearchRequest;
import com.beligum.blocks.filesystem.index.solr.SolrIndexSearchRequest;
import com.beligum.blocks.filesystem.index.solr.SolrPageIndexConnection;
import com.beligum.blocks.rdf.ifaces.RdfProperty;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Created by bram on 6/3/17.
 */
public interface IndexSearchRequest extends Serializable
{
    enum FilterBoolean
    {
        AND,
        OR,
        NOT
    }

    interface Filter
    {
    }

    static IndexSearchRequest createFor(SolrPageIndexConnection indexConnection)
    {
        return new SolrIndexSearchRequest();
    }
    //    public static IndexSearchRequest createFor(LucenePageIndexConnection indexConnection)
    //    {
    //        return new IndexSearchRequest();
    //    }
    //    public static IndexSearchRequest createFor(SesamePageIndexConnection indexConnection)
    //    {
    //        return new IndexSearchRequest();
    //    }
    static IndexSearchRequest createFor(IndexConnection indexConnection)
    {
        throw new UnsupportedOperationException("Unsupported index connection type; please implement a query builder for this implementation; " + indexConnection);
    }

    List<Filter> getFilters();
    Integer getPageSize();
    Integer getPageOffset();
    Locale getLanguage();
    Long getMaxResults();

    IndexSearchRequest filter(String value, FilterBoolean filterBoolean);

    IndexSearchRequest wildcard(String value, FilterBoolean filterBoolean);

    IndexSearchRequest filter(IndexEntryField field, String value, FilterBoolean filterBoolean);

    IndexSearchRequest filter(RdfProperty property, String value, FilterBoolean filterBoolean);

    IndexSearchRequest wildcard(IndexEntryField field, String value, FilterBoolean filterBoolean);

    IndexSearchRequest wildcard(RdfProperty property, String value, FilterBoolean filterBoolean);

    IndexSearchRequest filter(IndexSearchRequest subRequest, FilterBoolean filterBoolean);

    Collection<Filter> filters();

    IndexSearchRequest sort(RdfProperty property, boolean sortAscending);

    IndexSearchRequest sort(IndexEntryField field, boolean sortAscending);

    IndexSearchRequest pageSize(int pageSize);

    IndexSearchRequest pageOffset(int pageOffset);

    IndexSearchRequest language(Locale language);

    IndexSearchRequest maxResults(long maxResults);

}
