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

import com.beligum.blocks.index.sparql.SparqlIndexSearchRequest;
import com.beligum.blocks.index.sparql.SesamePageIndexConnection;
import com.beligum.blocks.index.solr.SolrIndexSearchRequest;
import com.beligum.blocks.index.solr.SolrPageIndexConnection;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;

import java.io.IOException;
import java.io.Serializable;
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

    enum FilterType
    {
        CLASS,
        FIELD,
        PROPERTY,
        QUERY,
        SUB,
        LANGUAGE
    }

    enum LanguageFilterType
    {
        STRICT,
        PREFERRED
    }

    interface Filter
    {
        FilterType getFilterType();
        FilterBoolean getFilterBoolean();
    }

    int DEFAULT_PAGE_SIZE = 50;
    int DEFAULT_PAGE_OFFSET = 0;

    /**
     * Note that multiple static polymorphic methods didn't seem to work...
     */
    static IndexSearchRequest createFor(IndexConnection indexConnection)
    {
        if (indexConnection instanceof SolrPageIndexConnection) {
            return new SolrIndexSearchRequest(indexConnection);
        }
        else if (indexConnection instanceof SesamePageIndexConnection) {
            return new SparqlIndexSearchRequest(indexConnection);
        }
        else {
            throw new UnsupportedOperationException("Unsupported index connection type; please implement a query builder for this implementation; " + indexConnection);
        }
    }

    /**
     * Returns the connection to the underlying index
     */
    IndexConnection getIndexConnection();

    /**
     * Returns an overview of all configured filters in this instance
     */
    List<Filter> getFilters();

    /**
     * Returns the requested page size
     */
    int getPageSize();

    /**
     * Returns the requested page offset (zero-indexed)
     */
    int getPageOffset();

    /**
     * Returns the requested language.
     */
    Locale getLanguage();

    /**
     * Sets the general search-all query value of this request.
     */
    IndexSearchRequest query(String value, FilterBoolean filterBoolean);

    /**
     * Adds a filter that only selects entries of the specified class.
     * The boolean configures how this filter is linked to previously added filters.
     */
    IndexSearchRequest filter(RdfClass type, FilterBoolean filterBoolean);

    /**
     * Adds a filter that only selects entries that have the exact specified value for the specified field.
     * The boolean configures how this filter is linked to previously added filters.
     */
    IndexSearchRequest filter(IndexEntryField field, String value, FilterBoolean filterBoolean);

    /**
     * Adds a filter that only selects entries that have the exact specified value for the specified property.
     * The boolean configures how this filter is linked to previously added filters.
     */
    IndexSearchRequest filter(RdfProperty property, String value, FilterBoolean filterBoolean);

    /**
     * Adds a sub-request to the chain of filters.
     * The boolean configures how this filter is linked to previously added filters.
     */
    IndexSearchRequest filter(IndexSearchRequest subRequest, FilterBoolean filterBoolean) throws IOException;

    /**
     * Adds a filter that only selects entries that have the specified value as a wildcard for the specified field.
     * The boolean configures how this filter is linked to previously added filters.
     */
    IndexSearchRequest wildcard(IndexEntryField field, String value, FilterBoolean filterBoolean);

    /**
     * Adds a filter that only selects entries that have the specified value as a wildcard for the specified property.
     * The boolean configures how this filter is linked to previously added filters.
     */
    IndexSearchRequest wildcard(RdfProperty property, String value, FilterBoolean filterBoolean);

    /**
     * Requests the results are sorted on the specified property in the specified order
     */
    IndexSearchRequest sort(RdfProperty property, boolean sortAscending);

    /**
     * Requests the results are sorted on the specified field in the specified order
     */
    IndexSearchRequest sort(IndexEntryField field, boolean sortAscending);

    /**
     * Configures the maximum number of results that will be returned (paged)
     */
    IndexSearchRequest pageSize(int pageSize);

    /**
     * Configures the page offset/index (zero-based) for the maximum number of results that will be returned in pages.
     */
    IndexSearchRequest pageOffset(int pageOffset);

    /**
     * A special filter that only selects entries with the specified language
     */
    IndexSearchRequest language(Locale language);

    /**
     * A special filter that will select the best language, when grouping all results on the specified grouping field.
     * The selection first searches for the specified language, then the default language, then all other languages.
     */
    IndexSearchRequest language(Locale language, IndexEntryField groupingField);

}
