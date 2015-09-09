package com.beligum.blocks.search;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.models.factories.ResourceFactoryImpl;
import com.beligum.blocks.models.interfaces.Resource;
import com.beligum.blocks.search.fields.AbstractField;
import com.beligum.blocks.search.fields.Field;
import com.beligum.blocks.search.queries.Query;
import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.sort.SortOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by wouter on 3/09/15.
 */
public abstract class AbstractSearch
{
    protected Query query = null;
//    protected Filter filter = null;
    protected Integer page = null;
    protected Integer pageLength = null;
    protected SearchRequestBuilder builder = null;
    protected CountRequestBuilder countBuilder = null;


    public AbstractSearch setQuery(Query query) {
        this.query = query;
        return this;
    }

    public Query getQuery() {
        return this.query;
    }

//  we do not add filters at the moment
//    because the count does not work with filters
//
//    public void setFilter(Filter filter) {
//        this.filter = filter;
//    }
//
//    public Filter getFilter() {
//        return this.filter;
//    }

    public AbstractSearch addSort(Field field, SortOrder order) {
        this.builder.addSort(field.getRawField(), order);
        return this;
    }

    // This is not null based. So 1 is the first page
    public AbstractSearch setPage(Integer page)
    {
        this.page = page;
        return this;
    }


    public AbstractSearch setPageLength(Integer pageLength)
    {
        this.pageLength = pageLength;
        return this;
    }

    public List<Resource> search(Locale locale) {
        if (this.query != null) {
            this.builder.setQuery(this.query.getQuery());
        }

        if (this.page != null && this.pageLength != null) {
            this.builder.setFrom((int) ((this.page - 1) * this.pageLength));
            this.builder.setSize(this.pageLength);
        }

        SearchResponse searchResponse = this.builder.execute().actionGet();
        List<Resource> retVal = new ArrayList<>();
        try {
            for (int index = 0; index < searchResponse.getHits().getHits().length; index++) {
                Resource resource = ResourceFactoryImpl.instance().deserializeResource(searchResponse.getHits().getAt(index).getSourceAsString().getBytes(), locale);
                resource.setLanguage(locale);
                retVal.add(resource);
            }
        } catch (Exception e) {
            Logger.error("Could not parse resource from ElasticSearch", e);
        }
        return retVal;
    }

    public Long totalHits() {
        if (this.query != null) {
            this.countBuilder.setQuery(this.query.getQuery());
        }

        return this.countBuilder.execute().actionGet().getCount();
    }
}
