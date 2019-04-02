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

import com.beligum.blocks.filesystem.index.sparql.SparqlIndexSearchRequest;
import com.beligum.blocks.filesystem.index.sparql.SesamePageIndexConnection;
import com.beligum.blocks.filesystem.index.solr.SolrIndexSearchRequest;
import com.beligum.blocks.filesystem.index.solr.SolrPageIndexConnection;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;

import java.io.IOException;
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

    //this is the default number of maximum search results that will be returned when no specific value is passed
    int DEFAULT_MAX_SEARCH_RESULTS = 1000;
    int DEFAULT_PAGE_SIZE = 50;
    int DEFAULT_PAGE_OFFSET = 0;

    /**
     * Note that multiple static polymorphic methods didn't seem to work...
     */
    static IndexSearchRequest createFor(IndexConnection indexConnection)
    {
        if (indexConnection instanceof SolrPageIndexConnection) {
            return new SolrIndexSearchRequest();
        }
        else if (indexConnection instanceof SesamePageIndexConnection) {
            return new SparqlIndexSearchRequest();
        }
        else {
            throw new UnsupportedOperationException("Unsupported index connection type; please implement a query builder for this implementation; " + indexConnection);
        }
    }

    List<Filter> getFilters();

    int getPageSize();

    int getPageOffset();

    Locale getLanguage();

    long getMaxResults();

    IndexSearchRequest query(String value, FilterBoolean filterBoolean);

    IndexSearchRequest filter(RdfClass type, FilterBoolean filterBoolean);

    IndexSearchRequest filter(IndexEntryField field, String value, FilterBoolean filterBoolean);

    IndexSearchRequest filter(RdfProperty property, String value, FilterBoolean filterBoolean);

    IndexSearchRequest wildcard(IndexEntryField field, String value, FilterBoolean filterBoolean);

    IndexSearchRequest wildcard(RdfProperty property, String value, FilterBoolean filterBoolean);

    IndexSearchRequest filter(IndexSearchRequest subRequest, FilterBoolean filterBoolean) throws IOException;

    Collection<Filter> filters();

    IndexSearchRequest sort(RdfProperty property, boolean sortAscending);

    IndexSearchRequest sort(IndexEntryField field, boolean sortAscending);

    IndexSearchRequest pageSize(int pageSize);

    IndexSearchRequest pageOffset(int pageOffset);

    IndexSearchRequest language(Locale language);

    IndexSearchRequest maxResults(long maxResults);

}
