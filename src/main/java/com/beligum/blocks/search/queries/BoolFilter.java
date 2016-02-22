//package com.beligum.blocks.search.queries;
//
//import com.beligum.blocks.search.fields.Field;
//import org.elasticsearch.index.query.BoolFilterBuilder;
//import org.elasticsearch.index.query.BoolQueryBuilder;
//
///**
// * Created by wouter on 3/09/15.
// */
//public class BoolFilter extends Filter
//{
//    public BoolFilter()
//    {
//        this.filter = new BoolFilterBuilder();
//    }
//
//    // Creates a query
//    public BoolFilter should(Field field, String value, Filter.Type type)
//    {
//        ((BoolFilterBuilder) this.filter).should(new FieldFilter(field, value, type).getFilter());
//        return this;
//    }
//
//    public BoolFilter must(Field field, String value, Filter.Type type)
//    {
//        ((BoolFilterBuilder) this.filter).must(new FieldFilter(field, value, type).getFilter());
//        return this;
//    }
//
//    public BoolFilter not(Field field, String value, Filter.Type type)
//    {
//        ((BoolFilterBuilder) this.filter).mustNot(new FieldFilter(field, value, type).getFilter());
//        return this;
//    }
//
//    // Creates a query
//    public BoolFilter should(Query query)
//    {
//        ((BoolQueryBuilder) this.filter).should(query.getQuery());
//        return this;
//    }
//
//    public BoolFilter must(Query query)
//    {
//        ((BoolQueryBuilder) this.filter).must(query.getQuery());
//        return this;
//    }
//
//    public BoolFilter not(Query query)
//    {
//        ((BoolQueryBuilder) this.filter).mustNot(query.getQuery());
//        return this;
//    }
//}
