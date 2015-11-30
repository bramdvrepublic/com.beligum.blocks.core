package com.beligum.blocks.search.queries;

import com.beligum.blocks.search.fields.Field;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;

/**
 * Created by wouter on 3/09/15.
 */
public class Filter
{

    public enum Type
    {
        EXACT,
        EXACT_PREFIX
    }

    protected FilterBuilder filter = null;

    protected Filter()
    {

    }

    public Filter(Field field, String value, Type type)
    {
        this.filter = new FieldFilter(field, value, type).getFilter();
    }

    public FilterBuilder getFilter()
    {
        return this.filter;
    }

    // ------- PROTECTED CLASSES --------

    protected class FieldFilter
    {
        private Field field;
        private String value;
        private Type type;

        public FieldFilter(Field field, String value, Type type)
        {
            this.field = field;
            this.value = value;
            this.type = type;
        }

        protected FilterBuilder getFilter()
        {
            FilterBuilder retVal = null;
            if (type.equals(Type.EXACT)) {
                retVal = FilterBuilders.termFilter(field.getRawField(), this.value);
            }
            else if (type.equals(Type.EXACT_PREFIX)) {
                retVal = FilterBuilders.prefixFilter(field.getRawField(), this.value);
            }
            return retVal;
        }
    }

}
