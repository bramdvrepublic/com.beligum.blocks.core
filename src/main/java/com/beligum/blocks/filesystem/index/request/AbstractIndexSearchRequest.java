package com.beligum.blocks.filesystem.index.request;

import com.beligum.blocks.filesystem.index.ifaces.IndexEntryField;
import com.beligum.blocks.filesystem.index.ifaces.IndexSearchRequest;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import org.apache.solr.client.solrj.SolrQuery;

import java.io.IOException;
import java.util.*;

import static com.beligum.blocks.filesystem.index.solr.SolrIndexSearchResult.DEFAULT_MAX_SEARCH_RESULTS;

public abstract class AbstractIndexSearchRequest implements IndexSearchRequest
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private List<IndexSearchRequest.Filter> filters;
    private Integer pageSize;
    private Integer pageOffset;
    private Locale language;
    private Long maxResults;


    //-----CONSTRUCTORS-----
    protected AbstractIndexSearchRequest()
    {
        this.filters = new ArrayList<>();
    }

    //-----PUBLIC METHODS-----
    @Override
    public List<Filter> getFilters()
    {
        return filters;
    }
    @Override
    public int getPageSize()
    {
        return pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
    }
    @Override
    public int getPageOffset()
    {
        return pageOffset == null ? DEFAULT_PAGE_OFFSET : pageOffset;
    }
    @Override
    public Locale getLanguage()
    {
        return language;
    }
    @Override
    public long getMaxResults()
    {
        return maxResults == null ? DEFAULT_MAX_SEARCH_RESULTS : maxResults;
    }

    //-----BUILDER METHODS-----
    public IndexSearchRequest filter(String value, FilterBoolean filterBoolean)
    {
        this.filters.add(new QueryFilter(value, filterBoolean, false));

        return this;
    }
    public IndexSearchRequest wildcard(String value, FilterBoolean filterBoolean)
    {
        this.filters.add(new QueryFilter(value, filterBoolean, true));

        return this;
    }
    public IndexSearchRequest filter(IndexEntryField field, String value, FilterBoolean filterBoolean)
    {
        this.filters.add(new FieldFilter(field, value, filterBoolean, false));

        return this;
    }
    public IndexSearchRequest filter(RdfProperty property, String value, FilterBoolean filterBoolean)
    {
        this.filters.add(new PropertyFilter(property, value, filterBoolean, false));

        return this;
    }
    public IndexSearchRequest wildcard(IndexEntryField field, String value, FilterBoolean filterBoolean)
    {
        this.filters.add(new FieldFilter(field, value, filterBoolean, true));

        return this;
    }
    public IndexSearchRequest wildcard(RdfProperty property, String value, FilterBoolean filterBoolean)
    {
        this.filters.add(new PropertyFilter(property, value, filterBoolean, true));

        return this;
    }
    public IndexSearchRequest filter(IndexSearchRequest subRequest, FilterBoolean filterBoolean) throws IOException
    {
        this.filters.add(new SubFilter(subRequest, filterBoolean));

        return this;
    }
    public Collection<IndexSearchRequest.Filter> filters()
    {
        return this.filters;
    }
    public IndexSearchRequest pageSize(int pageSize)
    {
        this.pageSize = pageSize;

        return this;
    }
    public IndexSearchRequest pageOffset(int pageOffset)
    {
        this.pageOffset = pageOffset;

        return this;
    }
    public IndexSearchRequest language(Locale language)
    {
        this.language = language;

        return this;
    }
    public IndexSearchRequest maxResults(long maxResults)
    {
        this.maxResults = maxResults;

        return this;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----INNER CLASSES-----
    private static abstract class AbstractFilter implements IndexSearchRequest.Filter
    {
        protected final String value;
        protected final FilterBoolean filterBoolean;

        protected AbstractFilter(String value, FilterBoolean filterBoolean)
        {
            this.value = value;
            this.filterBoolean = filterBoolean;
        }
    }

    public static class QueryFilter extends AbstractFilter
    {
        private final boolean isWildcard;

        private QueryFilter(String value, FilterBoolean filterBoolean, boolean isWildcard)
        {
            super(value, filterBoolean);

            this.isWildcard = isWildcard;
        }
    }

    public static class FieldFilter extends AbstractFilter
    {
        private final IndexEntryField field;
        private final boolean isWildcard;

        private FieldFilter(IndexEntryField field, String value, FilterBoolean filterBoolean, boolean isWildcard)
        {
            super(value, filterBoolean);

            this.field = field;
            this.isWildcard = isWildcard;
        }
    }

    public static class PropertyFilter extends AbstractFilter
    {
        private final RdfProperty property;
        private final boolean isWildcard;

        private PropertyFilter(RdfProperty property, String value, FilterBoolean filterBoolean, boolean isWildcard)
        {
            super(value, filterBoolean);

            this.property = property;
            this.isWildcard = isWildcard;
        }
    }

    public static class SubFilter extends AbstractFilter
    {
        private final IndexSearchRequest subRequest;

        private SubFilter(IndexSearchRequest subRequest, FilterBoolean filterBoolean)
        {
            super(null, filterBoolean);

            this.subRequest = subRequest;
        }
    }
}
