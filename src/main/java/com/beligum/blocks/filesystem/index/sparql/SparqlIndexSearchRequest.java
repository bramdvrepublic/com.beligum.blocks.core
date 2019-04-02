package com.beligum.blocks.filesystem.index.sparql;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.filesystem.index.entries.JsonPageIndexEntry;
import com.beligum.blocks.filesystem.index.ifaces.IndexEntryField;
import com.beligum.blocks.filesystem.index.ifaces.IndexSearchRequest;
import com.beligum.blocks.filesystem.index.ifaces.PageIndexEntry;
import com.beligum.blocks.filesystem.index.request.AbstractIndexSearchRequest;
import com.beligum.blocks.filesystem.index.solr.SolrConfigs;
import com.beligum.blocks.filesystem.index.solr.SolrField;
import com.beligum.blocks.rdf.RdfFactory;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfOntology;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import org.apache.solr.parser.QueryParser;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.beligum.blocks.filesystem.index.sparql.SesamePageIndexConnection.SPARQL_SUBJECT_BINDING_NAME;

public class SparqlIndexSearchRequest extends AbstractIndexSearchRequest
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private StringBuilder queryBuilder;
    private StringBuilder filterQueryBuilder;
    private Map<String, Boolean> sortsFields;

    //-----CONSTRUCTORS-----
    public SparqlIndexSearchRequest()
    {
        super();

        this.queryBuilder = new StringBuilder();
        this.filterQueryBuilder = new StringBuilder();
        this.sortsFields = new LinkedHashMap<>();
    }

    //-----PUBLIC METHODS-----
    @Override
    public IndexSearchRequest query(String value, FilterBoolean filterBoolean)
    {
        this.append(this.queryBuilder, filterBoolean, SolrConfigs._text_.getName(), value);

        return super.query(value, filterBoolean);
    }
    @Override
    public IndexSearchRequest filter(RdfClass type, FilterBoolean filterBoolean)
    {
        this.append(this.filterQueryBuilder, filterBoolean, this.nameOf(JsonPageIndexEntry.TYPEOF_PROPERTY), PageIndexEntry.generateTypeOf(type));

        return super.filter(type, filterBoolean);
    }
    @Override
    public IndexSearchRequest filter(IndexEntryField field, String value, FilterBoolean filterBoolean)
    {
        this.append(this.filterQueryBuilder, filterBoolean, field.getName(), value);

        return super.filter(field, value, filterBoolean);
    }
    @Override
    public IndexSearchRequest wildcard(IndexEntryField field, String value, FilterBoolean filterBoolean)
    {
        return super.wildcard(field, value, filterBoolean);
    }
    @Override
    public IndexSearchRequest filter(RdfProperty property, String value, FilterBoolean filterBoolean)
    {
        this.append(this.filterQueryBuilder, filterBoolean, this.nameOf(property), value);

        return super.filter(property, value, filterBoolean);
    }
    @Override
    public IndexSearchRequest wildcard(RdfProperty property, String value, FilterBoolean filterBoolean)
    {
        return super.wildcard(property, value, filterBoolean);
    }
    @Override
    public IndexSearchRequest filter(IndexSearchRequest subRequest, FilterBoolean filterBoolean) throws IOException
    {
        if (subRequest instanceof SparqlIndexSearchRequest) {
            SparqlIndexSearchRequest solrSubRequest = (SparqlIndexSearchRequest) subRequest;
            if (solrSubRequest.queryBuilder.length() > 0) {
                this.appendBoolean(this.queryBuilder, filterBoolean).append("(").append(solrSubRequest.queryBuilder).append(")");
            }
            if (solrSubRequest.filterQueryBuilder.length() > 0) {
                this.appendBoolean(this.filterQueryBuilder, filterBoolean).append("(").append(solrSubRequest.filterQueryBuilder).append(")");
            }
        }
        else {
            throw new IOException("Unsupported sub query type; " + subRequest);
        }

        return super.filter(subRequest, filterBoolean);
    }
    @Override
    public IndexSearchRequest sort(RdfProperty property, boolean sortAscending)
    {
        this.sortsFields.put(this.nameOf(property), sortAscending);

        return this;
    }
    @Override
    public IndexSearchRequest sort(IndexEntryField field, boolean sortAscending)
    {
        this.sortsFields.put(field.getName(), sortAscending);

        return this;
    }
    public String buildSparqlQuery()
    {
        StringBuilder retVal = new StringBuilder();

        for (RdfOntology o : RdfFactory.getRelevantOntologies()) {
            retVal.append("PREFIX ").append(o.getNamespace().getPrefix()).append(": <").append(o.getNamespace().getUri()).append("> \n");
        }
        retVal.append("\n");

        //links the resource to be found with the following query statements (required)
        retVal.append("SELECT DISTINCT ?").append(SPARQL_SUBJECT_BINDING_NAME).append(" WHERE {\n");

//        //TODO implement the type
        if (type != null) {
            retVal.append("\t").append("?").append(SPARQL_SUBJECT_BINDING_NAME).append(" a <").append(type.getFullName().toString()).append("> . \n");
        }
//
//        //---Lucene---
//        if (!StringUtils.isEmpty(luceneQuery)) {
//            retVal.append("\t").append("?").append(SPARQL_SUBJECT_BINDING_NAME).append(" ").append(searchPrefix).append(":matches [\n")
//                        //specifies the Lucene query (required)
//                        .append("\t").append("\t").append(searchPrefix).append(":query \"").append(QueryParser.escape(luceneQuery)).append("*").append("\";\n")
//                        //specifies the property to search. If omitted all properties are searched (optional)
//                        //                    .append("\t").append("\t").append(searchPrefix).append(":property ").append(Settings.instance().getRdfOntologyPrefix()).append(":").append("streetName").append(";\n")
//                        //specifies a variable for the score (optional)
//                        //                    .append("\t").append("\t").append(searchPrefix).append(":score ?score;\n")
//                        //specifies a variable for a highlighted snippet (optional)
//                        //.append("\t").append("\t").append(searchPrefix).append(":snippet ?snippet;\n")
//                        .append("\t").append("] .\n");
//        }
//
//        //---triple selection---
//        retVal.append("\t").append("?").append(SPARQL_SUBJECT_BINDING_NAME).append(" ?").append(SPARQL_PREDICATE_BINDING_NAME).append(" ?").append(SPARQL_OBJECT_BINDING_NAME).append(" .\n");
//
//        //---Filters---
//        if (fieldValues != null) {
//            Set<Map.Entry<RdfProperty, String>> entries = fieldValues.entrySet();
//            for (Map.Entry<RdfProperty, String> filter : entries) {
//                retVal.append("\t").append("?").append(SPARQL_SUBJECT_BINDING_NAME).append(" ").append(filter.getKey().getCurieName().toString()).append(" ")
//                            .append(filter.getValue()).append(" .\n");
//            }
//        }
//
//        //---Save the sort field---
//        if (sortField != null) {
//            retVal.append("\t").append("OPTIONAL{ ?").append(SPARQL_SUBJECT_BINDING_NAME).append(" <").append(sortField.getFullName().toString()).append("> ")
//                        .append("?sortField").append(" . }\n");
//        }
//
//        //---Closes the inner SELECT---
//        retVal.append("}\n");
//
//        //---Sorting---
//        if (sortField != null) {
//            retVal.append("ORDER BY ").append(sortAscending ? "ASC(" : "DESC(").append("?sortField").append(")").append("\n");
//        }
//        //note that, for pagination to work properly, we need to sort the results, so always add a sort field.
//        // eg see here: https://lists.w3.org/Archives/Public/public-rdf-dawg-comments/2011Oct/0024.html
//        else {
//            retVal.append("ORDER BY ").append(sortAscending ? "ASC(" : "DESC(").append("?").append(SPARQL_SUBJECT_BINDING_NAME).append(")").append("\n");
//        }
//
//        //---Paging---
//        retVal.append(" LIMIT ").append(pageSize).append(" OFFSET ").append(pageOffset).append("\n");

        return retVal.toString();
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private StringBuilder append(StringBuilder stringBuilder, FilterBoolean filterBoolean, String field, String value)
    {
        this.appendBoolean(stringBuilder, filterBoolean).append("(").append(QueryParser.escape(field)).append(":").append(QueryParser.escape(value)).append(")");

        return stringBuilder;
    }
    private StringBuilder appendBoolean(StringBuilder stringBuilder, FilterBoolean filterBoolean)
    {
        if (stringBuilder.length() > 0) {
            //see https://lucene.apache.org/solr/guide/7_7/the-standard-query-parser.html#boolean-operators-supported-by-the-standard-query-parser
            switch (filterBoolean) {
                case AND:
                    stringBuilder.append("&&");
                    break;
                case OR:
                    stringBuilder.append("||");
                    break;
                case NOT:
                    stringBuilder.append("!");
                    break;
                default:
                    Logger.error("Encountered unimplemented filter boolean, ignoring silently; " + filterBoolean);
                    break;
            }
        }
        else {
            switch (filterBoolean) {
                case AND:
                case OR:
                    //NOOP
                    break;
                case NOT:
                    stringBuilder.append("!");
                    break;
                default:
                    Logger.error("Encountered unimplemented filter boolean, ignoring silently; " + filterBoolean);
                    break;
            }
        }

        return stringBuilder;
    }
    private String nameOf(RdfClass rdfClass)
    {
        return new SolrField(rdfProperty).getName();
    }
    private String nameOf(RdfProperty rdfProperty)
    {
        return new SolrField(rdfProperty).getName();
    }

    //-----MGMT METHODS-----
    @Override
    public String toString()
    {
        return this.buildSparqlQuery();
    }
}
