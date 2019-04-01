package com.beligum.blocks.filesystem.index.solr;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.filesystem.index.ifaces.IndexEntryField;
import com.beligum.blocks.filesystem.index.ifaces.IndexSearchRequest;
import com.beligum.blocks.filesystem.index.ifaces.PageIndexEntry;
import com.beligum.blocks.filesystem.index.request.AbstractIndexSearchRequest;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.parser.QueryParser;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class SolrIndexSearchRequest extends AbstractIndexSearchRequest
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private StringBuilder queryBuilder;
    private StringBuilder filterQueryBuilder;
    private Map<String, Boolean> sortsFields;

    //-----CONSTRUCTORS-----
    public SolrIndexSearchRequest()
    {
        super();

        this.queryBuilder = new StringBuilder();
        this.filterQueryBuilder = new StringBuilder();
        this.sortsFields = new LinkedHashMap<>();
    }

    //-----PUBLIC METHODS-----
    @Override
    public IndexSearchRequest filter(String value, FilterBoolean filterBoolean)
    {
        this.append(this.queryBuilder, filterBoolean, SolrConfigs._text_.getName(), value);

        return super.filter(value, filterBoolean);
    }
    @Override
    public IndexSearchRequest wildcard(String value, FilterBoolean filterBoolean)
    {
        return super.wildcard(value, filterBoolean);
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
    public SolrQuery buildSolrQuery()
    {
        SolrQuery retVal = new SolrQuery();

        //make sure this happens before calling the retVal.setFilterQueries() below
        if (this.getLanguage() != null) {
            this.filter(PageIndexEntry.language, this.getLanguage().getLanguage(), FilterBoolean.AND);
        }

        if (this.queryBuilder.length() > 0) {
            retVal.setQuery(this.queryBuilder.toString());
        }

        if (this.filterQueryBuilder.length() > 0) {
            retVal.setFilterQueries(this.filterQueryBuilder.toString());
        }

        for (Map.Entry<String, Boolean> e : this.sortsFields.entrySet()) {
            retVal.addSort(e.getKey(), e.getValue() ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc);
        }

        retVal.setRows(this.getPageSize());
        retVal.setStart(this.getPageOffset());
//        if (this.getMaxResults() != null) {
//            TODO...
//        }

        return retVal;
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
    private String nameOf(RdfProperty rdfProperty)
    {
        return new SolrField(rdfProperty).getName();
    }

    //-----MGMT METHODS-----
    @Override
    public String toString()
    {
        return this.buildSolrQuery().toString();
    }
}
