package com.beligum.blocks.index.solr;

import com.beligum.base.server.R;
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
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

public class SolrIndexSearchRequest extends AbstractIndexSearchRequest
{
    //-----CONSTANTS-----
    public enum ValueOption implements IndexSearchRequest.ValueOption
    {
        /**
         * If wildcardSuffix is true, the value is adjusted to value[wildcard].
         */
        wildcardSuffix,

        /**
         * If wildcardPrefix is true, the value is adjusted to [wildcard]value.
         */
        wildcardPrefix,

        /**
         * If fuzzySearch is true, the value is adjusted to perform a fuzzy search.
         */
        fuzzy,
    }

    private enum InternalValueOption implements IndexSearchRequest.ValueOption
    {
        /**
         * Skips escape of the Solr-reserved characters in the value
         */
        noEscape,
    }

    //-----VARIABLES-----
    private SolrQuery solrQuery;
    private StringBuilder queryBuilder;
    private StringBuilder filterQueryBuilder;
    private String languageGroupFilter;

    //-----CONSTRUCTORS-----
    public SolrIndexSearchRequest(IndexConnection indexConnection)
    {
        super(indexConnection);

        this.queryBuilder = new StringBuilder();
        this.filterQueryBuilder = new StringBuilder();

        //Note: in Solr, a query is always necessary, so we start out with searching for everything
        this.solrQuery = new SolrQuery("*:*");
    }

    //-----PUBLIC METHODS-----
    @Override
    public IndexSearchRequest search(String value, FilterBoolean filterBoolean, Option... options)
    {
        //this.appendFullTextFilter(this.queryBuilder, filterBoolean, value, wildcardSuffix, wildcardPrefix, fuzzySearch);
        this.appendFilter(this.queryBuilder, filterBoolean, SolrConfigs._text_.getName(), value, options);
        this.updateQueries();

        return this;
    }
    @Override
    public IndexSearchRequest filter(RdfClass type, FilterBoolean filterBoolean)
    {
        this.appendFilter(this.filterQueryBuilder, filterBoolean, this.nameOf(JsonPageIndexEntry.TYPEOF_PROPERTY), ResourceIndexEntry.typeOfField.serialize(type));
        this.updateQueries();

        return this;
    }
    @Override
    public IndexSearchRequest filter(IndexEntryField field, String value, FilterBoolean filterBoolean, Option... options)
    {
        this.appendFilter(this.filterQueryBuilder, filterBoolean, field.getName(), value, options);
        this.updateQueries();

        return this;
    }
    @Override
    public IndexSearchRequest filter(RdfProperty property, String value, FilterBoolean filterBoolean, Option... options)
    {
        this.appendFilter(this.filterQueryBuilder, filterBoolean, this.nameOf(property), value, options);
        this.updateQueries();

        return this;
    }
    @Override
    public IndexSearchRequest missing(RdfProperty property, FilterBoolean filterBoolean)
    {
        this.appendFilter(this.filterQueryBuilder, filterBoolean, this.nameOf(property), "[* TO *]", InternalValueOption.noEscape);
        this.updateQueries();

        return this;
    }
    @Override
    public IndexSearchRequest missing(IndexEntryField field, FilterBoolean filterBoolean) throws IOException
    {
        this.appendFilter(this.filterQueryBuilder, filterBoolean, field.getName(), "[* TO *]", InternalValueOption.noEscape);
        this.updateQueries();

        return this;
    }
    @Override
    public IndexSearchRequest filter(IndexSearchRequest subRequest, FilterBoolean filterBoolean) throws IOException
    {
        if (subRequest instanceof SolrIndexSearchRequest) {
            SolrIndexSearchRequest solrSubRequest = (SolrIndexSearchRequest) subRequest;
            if (solrSubRequest.queryBuilder.length() > 0) {
                this.appendBoolean(this.queryBuilder, filterBoolean).append("(").append(solrSubRequest.queryBuilder).append(")");
                this.updateQueries();
            }
            if (solrSubRequest.filterQueryBuilder.length() > 0) {
                this.appendBoolean(this.filterQueryBuilder, filterBoolean).append("(").append(solrSubRequest.filterQueryBuilder).append(")");
                this.updateQueries();
            }
        }
        else {
            throw new IOException("Unsupported sub query type; " + subRequest);
        }

        return this;
    }
    @Override
    public IndexSearchRequest language(Locale language)
    {
        super.language(language);

        this.filter(PageIndexEntry.languageField, this.getLanguage().getLanguage(), FilterBoolean.AND);

        return this;
    }
    @Override
    public IndexSearchRequest language(Locale language, IndexEntryField field)
    {
        super.language(language, field);

        // Explanation: group all results with the same groupField (probably always "resource") and use the function below to decide
        // which one to take. The function will select entries where the returned value of the configured function is the highest ('max=').
        // That function will calculate the maximum ('max()') of two inner functions: the first will check if the language of the entry
        // is the requested entry (returns 3, highest score). The second will check if the language of the entry equals the default
        // site language (returns 2, second highest). In all other cases, 1 is returned, which means the entry has no special language.
        this.languageGroupFilter = "{!collapse" +

                                   // group on this field
                                   " field=" + this.languageGroupField +

                                   // remove documents with a null value in the collapse field
                                   // note that this shouldn't happen because the language field must always be filled in
                                   " nullPolicy=ignore" +

                                   // use the highest value of the function below as the best entry
                                   " max=" +
                                   "max(" +
                                   "if(eq(" + PageIndexEntry.languageField + ",'" + this.language.getLanguage() + "'),3,1)" +
                                   "," +
                                   "if(eq(" + PageIndexEntry.languageField + ",'" + R.configuration().getDefaultLanguage() + "'),2,1)" +
                                   ")" +

                                   // close the collapse
                                   "}";

        this.updateQueries();

        return this;
    }
    @Override
    public IndexSearchRequest sort(RdfProperty property, boolean sortAscending)
    {
        super.sort(property, sortAscending);

        this.updateSort();

        return this;
    }
    @Override
    public IndexSearchRequest sort(IndexEntryField field, boolean sortAscending)
    {
        super.sort(field, sortAscending);

        this.updateSort();

        return this;
    }
    @Override
    public IndexSearchRequest pageSize(int pageSize)
    {
        super.pageSize(pageSize);

        this.updateBounds();

        return this;
    }
    @Override
    public IndexSearchRequest pageOffset(int pageOffset)
    {
        super.pageOffset(pageOffset);

        this.updateBounds();

        return this;
    }

    //-----PROTECTED METHODS-----
    SolrQuery getSolrQuery()
    {
        return this.solrQuery;
    }

    //-----PRIVATE METHODS-----
    private StringBuilder appendFilter(StringBuilder stringBuilder, FilterBoolean filterBoolean, String field, String value, Option... options)
    {
        // calc the flags on the raw incoming value
        value = this.appendQueryModifiers(value, options);

        this.appendBoolean(stringBuilder, filterBoolean).append("(").append(QueryParser.escape(field)).append(":").append(value).append(")");

        return stringBuilder;
    }

    private String appendQueryModifiers(String value, Option... options)
    {
        boolean isNumber = !NumberUtils.isNumber(value);
        boolean hasAsterisk = value.contains("*");

        if (!Arrays.asList(options).contains(InternalValueOption.noEscape)) {
            value = QueryParser.escape(value);
        }

        for (Option option : options) {
            // wildcards for numbers don't really make sense, because it expands the search results way too much
            // if the user added their own wildcard in the query, it kind of makes sense to skip it as well
            if (option.equals(ValueOption.wildcardSuffix) && !isNumber && !hasAsterisk) {
                value = value + "*";
            }
            else if (option.equals(ValueOption.wildcardPrefix) && !isNumber && !hasAsterisk) {
                value = "*" + value;
            }
            else if (option.equals(ValueOption.fuzzy)) {
                value = value + "~";
            }
        }

        return value;
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
    private void updateQueries()
    {
        if (this.queryBuilder.length() > 0) {
            this.solrQuery.setQuery(this.queryBuilder.toString());
        }
        else {
            this.solrQuery.setQuery("*:*");
        }

        if (this.filterQueryBuilder.length() > 0) {
            this.solrQuery.setFilterQueries(this.filterQueryBuilder.toString());
        }
        else {
            this.solrQuery.setFilterQueries();
        }

        // the above call overwrites all filters, so make sure to add the language grouping filter
        // again if we have one
        if (this.languageGroupFilter != null) {
            this.solrQuery.addFilterQuery(this.languageGroupFilter);
        }
    }
    private void updateSort()
    {
        this.solrQuery.clearSorts();
        for (Map.Entry<String, Boolean> e : this.sortFields.entrySet()) {
            this.solrQuery.addSort(e.getKey(), e.getValue() ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc);
        }
    }
    private void updateBounds()
    {
        // zero based offset of matching documents to retrieve
        this.solrQuery.setStart(this.getPageOffset());

        // number of documents to return starting at "start"
        this.solrQuery.setRows(this.getPageSize());
    }

    //-----MGMT METHODS-----
    @Override
    public String toString()
    {
        return this.getSolrQuery().toString();
    }
}
