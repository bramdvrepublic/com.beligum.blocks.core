package com.beligum.blocks.filesystem.index.request;

import com.beligum.blocks.filesystem.index.ifaces.IndexEntry;
import com.beligum.blocks.filesystem.index.ifaces.IndexEntryField;
import com.beligum.blocks.filesystem.index.ifaces.IndexSearchRequest;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.google.common.collect.Iterables;

import java.util.*;

public class DefaultIndexSearchRequest implements IndexSearchRequest
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private List<Filter> filters;
    private RdfProperty sortProperty;
    private boolean sortAscending;
    private int pageSize;
    private int pageOffset;
    private Locale language;
    private long maxResults;

    //-----CONSTRUCTORS-----
    public static DefaultIndexSearchRequest create()
    {
        return new DefaultIndexSearchRequest();
    }
    private DefaultIndexSearchRequest()
    {
        this.filters = new ArrayList<>();
        this.sortAscending = true;
        this.maxResults = Integer.MAX_VALUE;
    }

    //-----PUBLIC METHODS-----
    @Override
    public List<Filter> getFilters()
    {
        return filters;
    }
    @Override
    public RdfProperty getSortProperty()
    {
        return sortProperty;
    }
    @Override
    public boolean isSortAscending()
    {
        return sortAscending;
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

    //-----FACTORY METHODS-----
    public DefaultIndexSearchRequest filter(String value, FilterBoolean filterBoolean)
    {
        this.filters.add(new QueryFilter(value, filterBoolean, false));

        return this;
    }
    public DefaultIndexSearchRequest wildcard(String value, FilterBoolean filterBoolean)
    {
        this.filters.add(new QueryFilter(value, filterBoolean, true));

        return this;
    }
    public DefaultIndexSearchRequest filter(IndexEntryField field, String value, FilterBoolean filterBoolean)
    {
        this.filters.add(new FieldFilter(field, value, filterBoolean, false));

        return this;
    }
    public DefaultIndexSearchRequest filter(RdfProperty property, String value, FilterBoolean filterBoolean)
    {
        this.filters.add(new PropertyFilter(property, value, filterBoolean, false));

        return this;
    }
    public DefaultIndexSearchRequest wildcard(IndexEntryField field, String value, FilterBoolean filterBoolean)
    {
        this.filters.add(new FieldFilter(field, value, filterBoolean, true));

        return this;
    }
    public DefaultIndexSearchRequest wildcard(RdfProperty property, String value, FilterBoolean filterBoolean)
    {
        this.filters.add(new PropertyFilter(property, value, filterBoolean, true));

        return this;
    }
    public DefaultIndexSearchRequest filter(IndexSearchRequest subRequest, FilterBoolean filterBoolean)
    {
        this.filters.add(new SubFilter(subRequest, filterBoolean));

        return this;
    }
    public Collection<Filter> filters()
    {
        return this.filters;
    }
    public DefaultIndexSearchRequest sort(RdfProperty property)
    {
        this.sortProperty = property;

        return this;
    }
    public DefaultIndexSearchRequest sortAscending(boolean sortAscending)
    {
        this.sortAscending = sortAscending;

        return this;
    }
    public DefaultIndexSearchRequest pageSize(int pageSize)
    {
        this.pageSize = pageSize;

        return this;
    }
    public DefaultIndexSearchRequest pageOffset(int pageOffset)
    {
        this.pageOffset = pageOffset;

        return this;
    }
    public DefaultIndexSearchRequest language(Locale language)
    {
        this.language = language;

        return this;
    }
    public DefaultIndexSearchRequest maxResults(long maxResults)
    {
        this.maxResults = maxResults;

        return this;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----INNER CLASSES-----
    private abstract class AbstractFilter implements Filter
    {
        protected final String value;
        protected final FilterBoolean filterBoolean;

        protected AbstractFilter(String value, FilterBoolean filterBoolean)
        {
            this.value = value;
            this.filterBoolean = filterBoolean;
        }
    }
    private class QueryFilter extends AbstractFilter
    {
        private final boolean isWildcard;

        private QueryFilter(String value, FilterBoolean filterBoolean, boolean isWildcard)
        {
            super(value, filterBoolean);

            this.isWildcard = isWildcard;
        }
    }
    private class FieldFilter extends AbstractFilter
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
    private class PropertyFilter extends AbstractFilter
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
    private class SubFilter extends AbstractFilter
    {
        private final IndexSearchRequest subRequest;

        private SubFilter(IndexSearchRequest subRequest, FilterBoolean filterBoolean)
        {
            super(null, filterBoolean);

            this.subRequest = subRequest;
        }
    }
}
