package com.beligum.blocks.index.sparql;

import com.beligum.blocks.index.ifaces.IndexConnection;
import com.beligum.blocks.index.ifaces.IndexEntryField;
import com.beligum.blocks.index.ifaces.IndexSearchRequest;
import com.beligum.blocks.index.request.AbstractIndexSearchRequest;
import com.beligum.blocks.rdf.RdfFactory;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfOntology;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.beligum.blocks.index.sparql.SesamePageIndexConnection.*;

public class SparqlIndexSearchRequest extends AbstractIndexSearchRequest
{
    //-----CONSTANTS-----
    private static final String SPARQL_SORT_FIELD_BINDING_NAME = "sortField";

    private interface Filter
    {
        enum FilterType
        {
            CLASS,
            PROPERTY,
            QUERY,
            SUB,
            LANGUAGE
        }

        FilterType getFilterType();
        FilterBoolean getFilterBoolean();
    }

    //-----VARIABLES-----
    protected List<Filter> filters;

    //-----CONSTRUCTORS-----
    public SparqlIndexSearchRequest(IndexConnection indexConnection)
    {
        super(indexConnection);

        this.filters = new ArrayList<>();
    }

    //-----STATIC METHODS-----
    /**
     * Adds all relevant ontology prefixes to the SPARQL query builder
     */
    public static void addOntologyPrefixes(StringBuilder sparqlQuery)
    {
        for (RdfOntology o : RdfFactory.getRelevantOntologies()) {
            sparqlQuery.append("PREFIX ").append(o.getNamespace().getPrefix()).append(": <").append(o.getNamespace().getUri()).append("> \n");
        }
        sparqlQuery.append("\n");
    }

    //-----PUBLIC METHODS-----
    @Override
    public IndexSearchRequest query(String value, boolean wildcardSuffix, boolean wildcardPrefix, boolean fuzzysearch, FilterBoolean filterBoolean)
    {
        this.filters.add(new QueryFilter(value, wildcardPrefix, wildcardSuffix, fuzzysearch, filterBoolean));

        return this;
    }
    @Override
    public IndexSearchRequest filter(RdfClass type, FilterBoolean filterBoolean)
    {
        this.filters.add(new ClassFilter(type, filterBoolean));

        return this;
    }
    @Override
    public IndexSearchRequest filter(IndexEntryField field, String value, boolean wildcardSuffix, FilterBoolean filterBoolean)
    {
        throw new UnsupportedOperationException("Internal index fields are not supported in the SPARQL query builder; " + field);
    }
    @Override
    public IndexSearchRequest filter(RdfProperty property, String value, boolean wildcardSuffix, FilterBoolean filterBoolean)
    {
        this.filters.add(new PropertyFilter(property, value, wildcardSuffix, false, false, filterBoolean));

        return this;
    }
    @Override
    public IndexSearchRequest filter(IndexSearchRequest subRequest, FilterBoolean filterBoolean) throws IOException
    {
        this.filters.add(new SubFilter(subRequest, filterBoolean));

        return this;
    }
    @Override
    public IndexSearchRequest missing(RdfProperty property, FilterBoolean filterBoolean)
    {
        // Ref, see https://stackoverflow.com/questions/7097307/selecting-using-sparql-based-on-triple-does-not-exist
        throw new UnsupportedOperationException("Unimplemented ; " + property);
    }
    @Override
    public IndexSearchRequest missing(IndexEntryField field, FilterBoolean filterBoolean) throws IOException
    {
        throw new UnsupportedOperationException("Internal index fields are not supported in the SPARQL query builder; " + field);
    }
    @Override
    public IndexSearchRequest all(String value, boolean wildcardSuffix, FilterBoolean filterBoolean)
    {
        throw new UnsupportedOperationException("Unimplemented ; " + value);
    }
    @Override
    public IndexSearchRequest sort(IndexEntryField field, boolean sortAscending)
    {
        throw new UnsupportedOperationException("Internal index fields are not supported in the SPARQL query builder; " + field);
    }
    public String buildSparqlQuery()
    {
        StringBuilder retVal = new StringBuilder();

        addOntologyPrefixes(retVal);

        //links the resource to be found with the following query statements (required)
        retVal.append("SELECT DISTINCT ?").append(SPARQL_SUBJECT_BINDING_NAME).append(" WHERE {\n");

        //---triple selection---
        retVal.append("\t").append("?").append(SPARQL_SUBJECT_BINDING_NAME).append(" ?").append(SPARQL_PREDICATE_BINDING_NAME).append(" ?").append(SPARQL_OBJECT_BINDING_NAME).append(" .\n");

        for (Filter filter : this.filters) {

            if (!filter.getFilterBoolean().equals(FilterBoolean.AND)) {
                throw new IllegalStateException("Unsupported filter boolean (only AND is supported for now); " + filter.getFilterBoolean());
            }

            switch (filter.getFilterType()) {

                case CLASS:

                    ClassFilter classFilter = (ClassFilter) filter;

                    retVal.append("\t").append("?").append(SPARQL_SUBJECT_BINDING_NAME).append(" a <").append(classFilter.rdfClass.getCurie().toString()).append("> . \n");

                    break;

                case PROPERTY:

                    PropertyFilter propertyFilter = (PropertyFilter) filter;

                    if (propertyFilter.wildcardSuffix) {
                        throw new IllegalStateException("wildcardSuffix searches are not supported yet; " + propertyFilter);
                    }

                    retVal.append("\t").append("?").append(SPARQL_SUBJECT_BINDING_NAME).append(" ").append(propertyFilter.property.getCurie().toString()).append(" ")
                          .append(propertyFilter.value).append(" .\n");

                    break;

                case QUERY:

                    QueryFilter queryFilter = (QueryFilter) filter;


                    if (queryFilter.wildcardSuffix) {
                        throw new IllegalStateException("Wildcard (suffix) searches are not supported yet; " + queryFilter);
                    }
                    if (queryFilter.wildcardPrefix) {
                        throw new IllegalStateException("Wildcard (prefix) searches are not supported yet; " + queryFilter);
                    }
                    if (queryFilter.fuzzysearch) {
                        throw new IllegalStateException("Fuzzy searches are not supported yet; " + queryFilter);
                    }

                    if (!StringUtils.isEmpty(queryFilter.value)) {
                        //Note: we'll ignore the wildcard flag for now and expect the query string will be a valid regex value
                        //Note: this will be super slow (regex is executed on all matches)
                        retVal.append("\t").append("FILTER regex(?").append(SPARQL_OBJECT_BINDING_NAME).append(", \"").append(queryFilter.value).append("\", \"i\")\n");
                    }

                    break;
                case SUB:

                    SubFilter subFilter = (SubFilter) filter;

                    throw new IllegalStateException("SPARQL doesn't support subqueries yet; " + subFilter);

                default:

                    throw new IllegalStateException("Unexpected value: " + filter.getFilterType());
            }
        }

        //TODO what about the language?

        //---Save the sort field---
        if (this.sortFields.size() > 1) {
            throw new IllegalStateException("Multiple SPARQL sort detected, this isn't supported yet; " + this.sortFields);
        }
        Map.Entry<String, Boolean> sortField = this.sortFields.isEmpty() ? null : this.sortFields.entrySet().iterator().next();
        if (sortField != null) {
            retVal.append("\t").append("OPTIONAL{ ?")
                  .append(SPARQL_SUBJECT_BINDING_NAME).append(" <").append(sortField.getKey()).append("> ").append("?").append(SPARQL_SORT_FIELD_BINDING_NAME)
                  .append(" . }\n");
        }

        //---Closes the inner SELECT---
        retVal.append("}\n");

        //---Sorting---
        if (sortField != null) {
            retVal.append("ORDER BY ").append(sortField.getValue() ? "ASC" : "DESC").append("(?").append(SPARQL_SORT_FIELD_BINDING_NAME).append(")").append("\n");
        }
        //note that, for pagination to work properly, we need to sort the results, so always add a sort field.
        // eg see here: https://lists.w3.org/Archives/Public/public-rdf-dawg-comments/2011Oct/0024.html
        else {
            retVal.append("ORDER BY ").append("DESC(").append("?").append(SPARQL_SUBJECT_BINDING_NAME).append(")").append("\n");
        }

        //---Paging---
        retVal.append(" LIMIT ").append(this.pageSize).append(" OFFSET ").append(this.pageOffset).append("\n");

        return retVal.toString();
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----MGMT METHODS-----
    @Override
    public String toString()
    {
        return this.buildSparqlQuery();
    }

    //-----INNER CLASSES-----
    private static abstract class AbstractFilter implements Filter
    {
        protected final Filter.FilterType filterType;
        protected final FilterBoolean filterBoolean;

        protected AbstractFilter(Filter.FilterType filterType, FilterBoolean filterBoolean)
        {
            this.filterType = filterType;
            this.filterBoolean = filterBoolean;
        }

        @Override
        public Filter.FilterType getFilterType()
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
        private final boolean fuzzysearch;
        private final boolean wildcardPrefix;
        private final boolean wildcardSuffix;



        private QueryFilter(String value, boolean wildcardSuffix, boolean wildcardPrefix, boolean fuzzysearch, FilterBoolean filterBoolean)
        {
            super(Filter.FilterType.QUERY, filterBoolean);

            this.value = value;
            this.fuzzysearch = fuzzysearch;
            this.wildcardPrefix = wildcardPrefix;
            this.wildcardSuffix = wildcardSuffix;
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

    public static class PropertyFilter extends AbstractFilter
    {
        public final RdfProperty property;
        public final String value;
        private final boolean fuzzysearch;
        private final boolean wildcardPrefix;
        private final boolean wildcardSuffix;

        private PropertyFilter(RdfProperty property, String value , boolean wildcardSuffix, boolean wildcardPrefix, boolean fuzzysearch,FilterBoolean filterBoolean)
        {
            super(FilterType.PROPERTY, filterBoolean);

            this.property = property;
            this.value = value;
            this.wildcardSuffix = wildcardSuffix;
            this.wildcardPrefix = wildcardPrefix;
            this.fuzzysearch = fuzzysearch;
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
