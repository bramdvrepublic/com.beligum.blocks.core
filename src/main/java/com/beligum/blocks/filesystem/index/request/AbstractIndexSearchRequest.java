package com.beligum.blocks.filesystem.index.request;

import com.beligum.blocks.filesystem.index.ifaces.IndexEntryField;
import com.beligum.blocks.filesystem.index.ifaces.IndexSearchRequest;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import org.apache.solr.client.solrj.SolrQuery;

import java.io.IOException;
import java.util.*;

public abstract class AbstractIndexSearchRequest implements IndexSearchRequest
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private List<IndexSearchRequest.Filter> filters;
    private int pageSize = DEFAULT_PAGE_SIZE;
    private int pageOffset = DEFAULT_PAGE_OFFSET;
    private Locale language;
    private long maxResults = DEFAULT_MAX_SEARCH_RESULTS;

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
        return pageSize;
    }
    @Override
    public int getPageOffset()
    {
        return pageOffset;
    }
    @Override
    public Locale getLanguage()
    {
        return language;
    }
    @Override
    public long getMaxResults()
    {
        return maxResults;
    }

    //-----BUILDER METHODS-----
    public IndexSearchRequest query(String value, FilterBoolean filterBoolean)
    {
        this.filters.add(new QueryFilter(value, filterBoolean, false));

        return this;
    }
    public IndexSearchRequest filter(RdfClass type, FilterBoolean filterBoolean)
    {
        this.filters.add(new ClassFilter(type, filterBoolean));

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
        protected final FilterBoolean filterBoolean;

        protected AbstractFilter(FilterBoolean filterBoolean)
        {
            this.filterBoolean = filterBoolean;
        }
    }

    public static class QueryFilter extends AbstractFilter
    {
        private final String value;
        private final boolean isWildcard;

        private QueryFilter(String value, FilterBoolean filterBoolean, boolean isWildcard)
        {
            super(filterBoolean);

            this.value = value;
            this.isWildcard = isWildcard;
        }
    }

    public static class ClassFilter extends AbstractFilter
    {
        private final RdfClass type;

        private ClassFilter(RdfClass type, FilterBoolean filterBoolean)
        {
            super(filterBoolean);

            this.type = type;
        }
    }

    public static class FieldFilter extends AbstractFilter
    {
        private final IndexEntryField field;
        private final String value;
        private final boolean isWildcard;

        private FieldFilter(IndexEntryField field, String value, FilterBoolean filterBoolean, boolean isWildcard)
        {
            super(filterBoolean);

            this.field = field;
            this.value = value;
            this.isWildcard = isWildcard;
        }
    }

    public static class PropertyFilter extends AbstractFilter
    {
        private final RdfProperty property;
        private final String value;
        private final boolean isWildcard;

        private PropertyFilter(RdfProperty property, String value, FilterBoolean filterBoolean, boolean isWildcard)
        {
            super(filterBoolean);

            this.property = property;
            this.value = value;
            this.isWildcard = isWildcard;
        }
    }

    public static class SubFilter extends AbstractFilter
    {
        private final IndexSearchRequest subRequest;

        private SubFilter(IndexSearchRequest subRequest, FilterBoolean filterBoolean)
        {
            super(filterBoolean);

            this.subRequest = subRequest;
        }
    }
}
