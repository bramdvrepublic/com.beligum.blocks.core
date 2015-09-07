package com.beligum.blocks.search.queries;

import com.beligum.blocks.search.fields.Field;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

/**
 * Created by wouter on 1/09/15.
 */
public class Query
{

    protected QueryBuilder query = null;

    public static enum Type {
        EXACT, EXACT_TERMS, PREFIX, PREFIX_TERMS
    }

    protected Query() {
    }

    public Query(Field field, String value, Query.Type type) {
        FieldQuery q = new FieldQuery(field, value, type);
        this.query = q.getQuery();
    }

    public QueryBuilder getQuery() {
        return this.query;
    }

    // -------- PROTECTED CLASS  ------------

    protected class FieldQuery {
        private Field field;
        private String value;
        private Query.Type type;

        public FieldQuery(Field field, String value, Query.Type type) {
            this.field = field;
            this.value = value;
            this.type = type;
        }

        protected QueryBuilder getQuery() {
            QueryBuilder retVal = null;
            String[] terms = this.value.split(" ");
            if (type.equals(Type.EXACT)) {
                retVal = QueryBuilders.termQuery(field.getRawField(), this.value);
            } else if (terms.length == 1) {
                if (type.equals(Type.PREFIX)) {
                    retVal = QueryBuilders.prefixQuery(field.getRawField(), this.value);
                }
                else if (type.equals(Type.EXACT_TERMS)) {
                    retVal = QueryBuilders.matchQuery(field.getField(), value);
                }
                else if (type.equals(Type.PREFIX_TERMS)) {
                    retVal = QueryBuilders.prefixQuery(field.getField(), value);
                }
            } else {
                retVal = QueryBuilders.boolQuery();
                for (String term: terms) {
                    if (type.equals(Type.PREFIX)) {
                        ((BoolQueryBuilder)retVal).must(QueryBuilders.prefixQuery(field.getRawField(), term));
                    }
                    else if (type.equals(Type.EXACT_TERMS)) {
                        ((BoolQueryBuilder)retVal).must(QueryBuilders.matchQuery(field.getField(), term));
                    }
                    else if (type.equals(Type.PREFIX_TERMS)) {
                        ((BoolQueryBuilder)retVal).must(QueryBuilders.prefixQuery(field.getField(), term));
                    }
                }
            }
            return retVal;
        }
    }


}
