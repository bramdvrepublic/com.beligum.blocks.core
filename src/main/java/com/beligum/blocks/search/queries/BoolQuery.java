package com.beligum.blocks.search.queries;


import com.beligum.blocks.search.fields.Field;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

/**
 * Created by wouter on 3/09/15.
 */
public class BoolQuery extends Query
{
    public BoolQuery() {
        this.query = QueryBuilders.boolQuery();
    }

    // Creates a query
    public BoolQuery should(Field field, String value, Query.Type type) {
        ((BoolQueryBuilder)this.query).should(new FieldQuery(field, value, type).getQuery());
        return this;
    }

    public BoolQuery must(Field field, String value, Query.Type type) {
        ((BoolQueryBuilder)this.query).must(new FieldQuery(field, value, type).getQuery());
        return this;
    }

    public BoolQuery not(Field field, String value, Query.Type type) {
        ((BoolQueryBuilder)this.query).mustNot(new FieldQuery(field, value, type).getQuery());
        return this;
    }

    // Creates a query
    public BoolQuery should(Query query) {
        ((BoolQueryBuilder)this.query).should(query.getQuery());
        return this;
    }

    public BoolQuery must(Query query) {
        ((BoolQueryBuilder)this.query).must(query.getQuery());
        return this;
    }

    public BoolQuery not(Query query) {
        ((BoolQueryBuilder)this.query).mustNot(query.getQuery());
        return this;
    }

}
