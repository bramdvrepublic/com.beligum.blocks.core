package com.beligum.blocks.index.solr;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.index.entries.JsonPageIndexEntry;
import com.beligum.blocks.index.ifaces.*;
import com.beligum.blocks.index.request.AbstractIndexSearchRequest;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.parser.QueryParser;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

public class SolrIndexSearchRequest extends AbstractIndexSearchRequest
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private StringBuilder queryBuilder;
    private StringBuilder filterQueryBuilder;

    //-----CONSTRUCTORS-----
    public SolrIndexSearchRequest(IndexConnection indexConnection)
    {
        super(indexConnection);

        this.queryBuilder = new StringBuilder();
        this.filterQueryBuilder = new StringBuilder();
    }

    //-----PUBLIC METHODS-----
    @Override
    public IndexSearchRequest query(String value, FilterBoolean filterBoolean)
    {
        this.appendFilter(this.queryBuilder, filterBoolean, SolrConfigs._text_.getName(), value);

        return super.query(value, filterBoolean);
    }
    @Override
    public IndexSearchRequest filter(RdfClass type, FilterBoolean filterBoolean)
    {
        this.appendFilter(this.filterQueryBuilder, filterBoolean, this.nameOf(JsonPageIndexEntry.TYPEOF_PROPERTY), ResourceIndexEntry.typeOfField.serialize(type));

        return super.filter(type, filterBoolean);
    }
    @Override
    public IndexSearchRequest filter(IndexEntryField field, String value, FilterBoolean filterBoolean)
    {
        this.appendFilter(this.filterQueryBuilder, filterBoolean, field.getName(), value);

        return super.filter(field, value, filterBoolean);
    }
    @Override
    public IndexSearchRequest filter(RdfProperty property, String value, FilterBoolean filterBoolean)
    {
        this.appendFilter(this.filterQueryBuilder, filterBoolean, this.nameOf(property), value);

        return super.filter(property, value, filterBoolean);
    }
    @Override
    public IndexSearchRequest filter(IndexSearchRequest subRequest, FilterBoolean filterBoolean) throws IOException
    {
        if (subRequest instanceof SolrIndexSearchRequest) {
            SolrIndexSearchRequest solrSubRequest = (SolrIndexSearchRequest) subRequest;
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
    public IndexSearchRequest wildcard(IndexEntryField field, String value, FilterBoolean filterBoolean)
    {
        this.appendWildcard(this.filterQueryBuilder, filterBoolean, field.getName(), value);

        return super.wildcard(field, value, filterBoolean);
    }
    @Override
    public IndexSearchRequest wildcard(RdfProperty property, String value, FilterBoolean filterBoolean)
    {
        this.appendWildcard(this.filterQueryBuilder, filterBoolean, this.nameOf(property), value);

        return super.wildcard(property, value, filterBoolean);
    }

    //-----PROTECTED METHODS-----
    SolrQuery buildSolrQuery()
    {
        //Note: in Solr, a query is always necessary, so we start out with searching for everything
        SolrQuery retVal = new SolrQuery("*:*");

        //make sure this happens before calling the retVal.setFilterQueries() below
        if (this.getLanguage() != null) {

            // do this here because we'll just add a filter in strict mode
            switch (this.languageFilterType) {
                case STRICT:
                    this.filter(PageIndexEntry.languageField, this.getLanguage().getLanguage(), FilterBoolean.AND);
                    break;
                case PREFERRED:
                    StringBuilder langFilterQuery = new StringBuilder();
                    // see https://lucene.apache.org/solr/guide/7_7/collapse-and-expand-results.html
                    langFilterQuery.append("!collapse field=").append(this.languageGroupField);
                    retVal.addFilterQuery(langFilterQuery.toString());
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + this.languageFilterType);
            }
        }

        if (this.queryBuilder.length() > 0) {
            retVal.setQuery(this.queryBuilder.toString());
        }

        if (this.filterQueryBuilder.length() > 0) {
            retVal.setFilterQueries(this.filterQueryBuilder.toString());
        }

        for (Map.Entry<String, Boolean> e : this.sortFields.entrySet()) {
            retVal.addSort(e.getKey(), e.getValue() ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc);
        }

        retVal.setRows(this.getPageSize());
        retVal.setStart(this.getPageOffset());


        //        if (this.getMaxResults() != null) {
        //            TODO...
        //        }

        return retVal;
    }

    //-----PRIVATE METHODS-----
    private StringBuilder appendFilter(StringBuilder stringBuilder, FilterBoolean filterBoolean, String field, String value)
    {
        this.appendBoolean(stringBuilder, filterBoolean).append("(").append(QueryParser.escape(field)).append(":").append(QueryParser.escape(value)).append(")");

        return stringBuilder;
    }
    private StringBuilder appendWildcard(StringBuilder stringBuilder, FilterBoolean filterBoolean, String field, String value)
    {
        StringBuilder query = new StringBuilder(QueryParser.escape(value));

        if (!NumberUtils.isNumber(value) && !value.contains("*")) {
            query.append("*");
        }

        this.appendBoolean(stringBuilder, filterBoolean).append("(").append(QueryParser.escape(field)).append(":").append(query.toString()).append(")");

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

    //-----MGMT METHODS-----
    @Override
    public String toString()
    {
        return this.buildSolrQuery().toString();
    }
}
