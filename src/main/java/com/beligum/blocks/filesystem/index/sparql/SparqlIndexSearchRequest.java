package com.beligum.blocks.filesystem.index.sparql;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.filesystem.index.ifaces.IndexEntryField;
import com.beligum.blocks.filesystem.index.ifaces.IndexSearchRequest;
import com.beligum.blocks.filesystem.index.request.AbstractIndexSearchRequest;
import com.beligum.blocks.filesystem.index.solr.SolrField;
import com.beligum.blocks.rdf.RdfFactory;
import com.beligum.blocks.rdf.ifaces.RdfOntology;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.parser.QueryParser;

import java.util.Map;
import java.util.Set;

import static com.beligum.blocks.filesystem.index.sparql.SesamePageIndexConnection.*;

public class SparqlIndexSearchRequest extends AbstractIndexSearchRequest
{
    //-----CONSTANTS-----
    private static final String SPARQL_SORT_FIELD_BINDING_NAME = "sortField";

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public SparqlIndexSearchRequest()
    {
        super();
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
    public IndexSearchRequest filter(IndexEntryField field, String value, FilterBoolean filterBoolean)
    {
        throw new UnsupportedOperationException("Internal index fields are not supported in the SPARQL query builder; " + field);
    }
    @Override
    public IndexSearchRequest wildcard(IndexEntryField field, String value, FilterBoolean filterBoolean)
    {
        throw new UnsupportedOperationException("Internal index fields are not supported in the SPARQL query builder; " + field);
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

                    retVal.append("\t").append("?").append(SPARQL_SUBJECT_BINDING_NAME).append(" a <").append(classFilter.rdfClass.getCurieName().toString()).append("> . \n");

                    break;
                case FIELD:

                    FieldFilter fieldFilter = (FieldFilter) filter;

                    throw new IllegalStateException("SPARQL doesn't support internal field filters; " + fieldFilter);

                case PROPERTY:

                    PropertyFilter propertyFilter = (PropertyFilter) filter;

                    if (propertyFilter.isWildcard) {
                        throw new IllegalStateException("Wildcards in property filters are not supported yet; " + propertyFilter);
                    }

                    retVal.append("\t").append("?").append(SPARQL_SUBJECT_BINDING_NAME).append(" ").append(propertyFilter.property.getCurieName().toString()).append(" ")
                          .append(propertyFilter.value).append(" .\n");

                    break;
                case QUERY:

                    QueryFilter queryFilter = (QueryFilter) filter;

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
}
