package com.beligum.blocks.filesystem.index.request;

import com.beligum.blocks.filesystem.index.ifaces.IndexEntryField;
import com.beligum.blocks.filesystem.index.ifaces.IndexSearchRequest;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;

import java.io.IOException;
import java.util.*;

public abstract class AbstractIndexSearchRequest implements IndexSearchRequest
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    protected List<IndexSearchRequest.Filter> filters;
    protected Map<String, Boolean> sortFields;
    protected int pageSize = DEFAULT_PAGE_SIZE;
    protected int pageOffset = DEFAULT_PAGE_OFFSET;
    protected Locale language;
    protected long maxResults = DEFAULT_MAX_SEARCH_RESULTS;

    //-----CONSTRUCTORS-----
    protected AbstractIndexSearchRequest()
    {
        this.filters = new ArrayList<>();
        this.sortFields = new LinkedHashMap<>();
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
    @Override
    public IndexSearchRequest query(String value, FilterBoolean filterBoolean)
    {
        this.filters.add(new QueryFilter(value, filterBoolean, false));

        return this;
    }
    @Override
    public IndexSearchRequest filter(RdfClass type, FilterBoolean filterBoolean)
    {
        this.filters.add(new ClassFilter(type, filterBoolean));

        return this;
    }
    @Override
    public IndexSearchRequest filter(IndexEntryField field, String value, FilterBoolean filterBoolean)
    {
        this.filters.add(new FieldFilter(field, value, filterBoolean, false));

        return this;
    }
    @Override
    public IndexSearchRequest wildcard(IndexEntryField field, String value, FilterBoolean filterBoolean)
    {
        this.filters.add(new FieldFilter(field, value, filterBoolean, true));

        return this;
    }
    @Override
    public IndexSearchRequest filter(RdfProperty property, String value, FilterBoolean filterBoolean)
    {
        this.filters.add(new PropertyFilter(property, value, filterBoolean, false));

        return this;
    }
    @Override
    public IndexSearchRequest wildcard(RdfProperty property, String value, FilterBoolean filterBoolean)
    {
        this.filters.add(new PropertyFilter(property, value, filterBoolean, true));

        return this;
    }
    @Override
    public IndexSearchRequest filter(IndexSearchRequest subRequest, FilterBoolean filterBoolean) throws IOException
    {
        this.filters.add(new SubFilter(subRequest, filterBoolean));

        return this;
    }
    @Override
    public IndexSearchRequest sort(RdfProperty property, boolean sortAscending)
    {
        this.sortFields.put(this.nameOf(property), sortAscending);

        return this;
    }
    @Override
    public IndexSearchRequest sort(IndexEntryField field, boolean sortAscending)
    {
        this.sortFields.put(field.getName(), sortAscending);

        return this;
    }
    @Override
    public IndexSearchRequest pageSize(int pageSize)
    {
        this.pageSize = pageSize;

        return this;
    }
    @Override
    public IndexSearchRequest pageOffset(int pageOffset)
    {
        this.pageOffset = pageOffset;

        return this;
    }
    @Override
    public IndexSearchRequest language(Locale language)
    {
        this.language = language;

        return this;
    }
    @Override
    public IndexSearchRequest maxResults(long maxResults)
    {
        this.maxResults = maxResults;

        return this;
    }

    //-----PROTECTED METHODS-----
    protected String nameOf(RdfProperty rdfProperty)
    {
        return rdfProperty.getCurie().toString();
    }

    //-----PRIVATE METHODS-----

    //-----INNER CLASSES-----
    private static abstract class AbstractFilter implements IndexSearchRequest.Filter
    {
        protected final FilterType filterType;
        protected final FilterBoolean filterBoolean;

        protected AbstractFilter(FilterType filterType, FilterBoolean filterBoolean)
        {
            this.filterType = filterType;
            this.filterBoolean = filterBoolean;
        }

        @Override
        public FilterType getFilterType()
        {
            return filterType;
        }
        @Override
        public FilterBoolean getFilterBoolean()
        {
            return filterBoolean;
        }
    }

    public static class QueryFilter extends AbstractFilter
    {
        public final String value;
        public final boolean isWildcard;

        private QueryFilter(String value, FilterBoolean filterBoolean, boolean isWildcard)
        {
            super(FilterType.QUERY, filterBoolean);

            this.value = value;
            this.isWildcard = isWildcard;
        }
    }

    public static class ClassFilter extends AbstractFilter
    {
        public final RdfClass rdfClass;

        private ClassFilter(RdfClass rdfClass, FilterBoolean filterBoolean)
        {
            super(FilterType.CLASS, filterBoolean);

            this.rdfClass = rdfClass;
        }
    }

    public static class FieldFilter extends AbstractFilter
    {
        public final IndexEntryField field;
        public final String value;
        public final boolean isWildcard;

        private FieldFilter(IndexEntryField field, String value, FilterBoolean filterBoolean, boolean isWildcard)
        {
            super(FilterType.FIELD, filterBoolean);

            this.field = field;
            this.value = value;
            this.isWildcard = isWildcard;
        }
    }

    public static class PropertyFilter extends AbstractFilter
    {
        public final RdfProperty property;
        public final String value;
        public final boolean isWildcard;

        private PropertyFilter(RdfProperty property, String value, FilterBoolean filterBoolean, boolean isWildcard)
        {
            super(FilterType.PROPERTY, filterBoolean);

            this.property = property;
            this.value = value;
            this.isWildcard = isWildcard;
        }
    }

    public static class SubFilter extends AbstractFilter
    {
        public final IndexSearchRequest subRequest;

        private SubFilter(IndexSearchRequest subRequest, FilterBoolean filterBoolean)
        {
            super(FilterType.SUB, filterBoolean);

            this.subRequest = subRequest;
        }
    }
}
