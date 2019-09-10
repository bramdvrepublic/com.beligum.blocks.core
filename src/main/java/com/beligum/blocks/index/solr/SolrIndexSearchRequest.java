package com.beligum.blocks.index.solr;

import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.index.entries.JsonPageIndexEntry;
import com.beligum.blocks.index.ifaces.*;
import com.beligum.blocks.index.request.AbstractIndexSearchRequest;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.google.common.collect.Iterables;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.parser.QueryParser;

import java.io.IOException;
import java.net.URI;
import java.util.*;

import static com.beligum.blocks.index.ifaces.JoinSearchRequest.AddJoinOption.PIGGYBACK;

public class SolrIndexSearchRequest extends AbstractIndexSearchRequest implements JoinSearchRequest, GraphSearchRequest
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

        /**
         * Interprets the value as a list of terms and escapes all Solr-reserved characters in the value.
         * The default mode doesn't escape any characters.
         */
        termSearch,

        /**
         * Interprets the value as a phrase and surrounds it with double quotes, escaping where necessary.
         */
        phraseSearch,
        /**
         * Interprets the value as a range. This query will be formed like [int - int] or {string - string} and should not be escaped
         */
        rangeSearch,

    }

    private enum InternalValueOption implements FilteredSearchRequest.ValueOption
    {
        /**
         * Explicitly skips the escaping of the Solr-reserved characters in the value, no matter what
         */
        noEscape,
    }

    /**
     * Adds transformers to the query
     * https://lucene.apache.org/solr/guide/8_0/transforming-result-documents.html#TransformingResultDocuments-_elevated_and_excluded_
     *
     */
    public enum TransformerValueOption implements FilteredSearchRequest.Option
    {

        childDocTransformer("[child]"),
        shardAugmenter("[shard]"),
        docid("[docid]");

        private String value;

        TransformerValueOption(String value)
        {
            this.value = value;
        }

        public String getValue()
        {
            return value;
        }

    }

    //-----VARIABLES-----
    private StringBuilder queryBuilder;
    private StringBuilder filterQueryBuilder;
    private String languageGroupFilter;
    private Map<String, List<String>> customParams;

    //-----CONSTRUCTORS-----
    public SolrIndexSearchRequest(IndexConnection indexConnection)
    {
        super(indexConnection);

        this.queryBuilder = new StringBuilder();
        this.filterQueryBuilder = new StringBuilder();
        this.customParams = new HashMap<>();
    }

    //-----PUBLIC METHODS-----
    @Override
    public IndexSearchRequest search(String value, FilterBoolean filterBoolean, Option... options)
    {
        this.appendFilter(this.queryBuilder, filterBoolean, SolrConfigs._text_.getName(), value, Arrays.asList(options));

        return this;
    }
    @Override
    public IndexSearchRequest filter(RdfClass type, FilterBoolean filterBoolean)
    {
        this.appendFilter(this.filterQueryBuilder, filterBoolean, this.nameOf(JsonPageIndexEntry.TYPEOF_PROPERTY), ResourceIndexEntry.typeOfField.serialize(type));

        return this;
    }
    @Override
    public IndexSearchRequest filter(IndexEntryField field, String value, FilterBoolean filterBoolean, Option... options)
    {
        // I think it makes sense to force phrase search for field filters, right?
        this.appendFilter(this.filterQueryBuilder, filterBoolean, field.getName(), value, Arrays.asList(options), Collections.singleton(ValueOption.phraseSearch));

        return this;
    }

    @Override
    public GraphSearchRequest constructGraph(IndexEntryField from, IndexEntryField to, Option... options)
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{!graph from=").append(from.getName()).append(" ");
        stringBuilder.append("to=").append(to.getName());
        for (Option graphSearchValueOption : GraphSearchOption.values()) {
            if (Arrays.asList(options).contains(graphSearchValueOption)) {
                stringBuilder.append(" " + graphSearchValueOption.toString() + "=true");
            }
            else {
                stringBuilder.append(" " + graphSearchValueOption.toString() + "=false");
            }
        }
        stringBuilder.append("}");
        if (this.queryBuilder.length() > 0) {
            throw new UnsupportedOperationException("Can not combine a GraphSearchRequest with a second query. Existing query " + this.queryBuilder.toString());
        }
        this.queryBuilder.append(stringBuilder);
        return this;
    }

    /**
     * No need to be able to filter this; just append it with the  relevant subqueries
     *
     * @param subQuery
     * @return
     */
    @Override
    public GraphSearchRequest appendGraphSource(FilteredSearchRequest subQuery)
    {
        if (!(this instanceof GraphSearchRequest) || !(this instanceof JoinSearchRequest)) {
            throw new UnsupportedOperationException("this query can not be appended");
        }
        //add to queryBuilder
        if (subQuery instanceof SolrIndexSearchRequest) {
            this.queryBuilder.append(((SolrIndexSearchRequest) subQuery).queryBuilder);
            //also add the custom parameters to the current query.
            this.customParams.putAll(((SolrIndexSearchRequest) subQuery).customParams);
        }
        return this;
    }

    /**
     * Some of the options are required:
     * JoinOption (regular/blockjoin)
     * if blockjoin : child or parent.
     *
     * @param from                  the first leg of the 'join on' query
     * @param to                    the last leg of the 'join on' query
     * @param rdfClass              the rdfClass to want to query
     * @param filteredSearchRequest
     * @param options
     * @return
     */
    @Override
    public JoinSearchRequest addJoin(IndexEntryField from, IndexEntryField to, RdfClass rdfClass, FilteredSearchRequest filteredSearchRequest, Option... options)
    {
        List optionList = Arrays.asList(options);
        StringBuilder stringBuilder = new StringBuilder();
        if (optionList.contains(JoinOption.REGULAR)) {
            if (optionList.contains(BlockJoinOption.CHILD) || optionList.contains(BlockJoinOption.PARENT)) {
                throw new UnsupportedOperationException(BlockJoinOption.class.getName() + " should only be used by blockjoins.");
            }
            if (from != null && to != null) {
                stringBuilder.append("{!join from=").append(from.getName()).append(" ");
                stringBuilder.append("to=").append(to.getName()).append("}");
            }
            stringBuilder.append("{!filters param=$").append(rdfClass.getName()).append("}");
            if (filteredSearchRequest != null) {
                //add a custom param where we'll  put the filteredSearchRequest.
                if (filteredSearchRequest instanceof SolrIndexSearchRequest) {
                    SolrIndexSearchRequest currentSubRequest = (SolrIndexSearchRequest) filteredSearchRequest;
                    if (currentSubRequest.queryBuilder.length() > 0 && currentSubRequest.filterQueryBuilder.length() > 0) {
                        throw new UnsupportedOperationException("Both a query and a filterquery are defined. I don't know how to join both of them.");
                    }
                    List<String> filters = this.customParams.get(rdfClass.getName());

                    if (currentSubRequest.queryBuilder.length() > 0) {
                        //add to params
                        if (filters == null) {
                            filters = new ArrayList<>();
                        }
                        filters.add(currentSubRequest.queryBuilder.toString());
                    }
                    else if (currentSubRequest.filterQueryBuilder.length() > 0) {
                        //add to params
                        if (filters == null) {
                            filters = new ArrayList<>();
                        }
                        stringBuilder.append(currentSubRequest.queryBuilder);
                    }
                    if (filters != null) {
                        this.customParams.put(rdfClass.getName(), filters);
                    }
                }
            }
        }
        else {
            throw new UnsupportedOperationException("Type of Join was not supported.");
        }
        this.queryBuilder.append(stringBuilder);

        return this;
    }

    /**
     * Some of the options are required:
     * JoinOption (blockjoin or regular)
     *
     * @param rdfClass
     * @param rdfProperty
     * @param from
     * @param to
     * @param value
     * @param filterBoolean
     * @param options
     * @return
     */
    @Override
    public JoinSearchRequest addJoinFilter(RdfClass rdfClass, RdfProperty rdfProperty, IndexEntryField from, IndexEntryField to, String value, FilterBoolean filterBoolean, Option... options)
    {
        return this.buildJoinFilter(rdfClass, rdfProperty, from, to, value, filterBoolean, options);
    }

    /**
     * Some of the options are required:
     * JoinOption (blockjoin or regular)
     *
     * @param rdfClass
     * @param from
     * @param to
     * @param value
     * @param filterBoolean
     * @param options
     * @return
     */
    @Override
    public JoinSearchRequest addJoinFilter(RdfClass rdfClass, IndexEntryField indexEntryField, IndexEntryField from, IndexEntryField to, String value, FilterBoolean filterBoolean, Option... options)
    {
        return this.buildJoinFilter(rdfClass, indexEntryField, from, to, value, filterBoolean, options);
    }

    private JoinSearchRequest buildJoinFilter(RdfClass rdfClass, Object keyProperty, IndexEntryField from, IndexEntryField to, String value, FilterBoolean filterBoolean, Option... options)
    {
        List optionList = Arrays.asList(options);
        StringBuilder sb = new StringBuilder();
        if (optionList.contains(JoinOption.BLOCKJOIN)) {
            if (queryBuilder.length() > 0) {
                //add a space in  front
                sb.append(" ");
            }
            if (filterBoolean.equals(FilterBoolean.AND)) {
                //FIXME this can probably be dropped, no?
                sb.append("+");
            }
            else if (filterBoolean.equals(FilterBoolean.NOT)) {
                sb.append("-");
            }
            else if (filterBoolean.equals(FilterBoolean.OR) || filterBoolean.equals(FilterBoolean.NONE)) {
                //append  nothing
            }
            //            sb.append("(");
            if (!optionList.contains(PIGGYBACK)) {

                sb.append("{!");
                if (optionList.contains(BlockJoinOption.PARENT)) {
                    sb.append("parent which=typeOf:");
                }
                else {
                    throw new UnsupportedOperationException("child blockjoins are not supported yet");
                }
                String rdfClassCurieString = (rdfClass.getCurie()).toString();
                //always escape
                sb.append(escapeField(rdfClassCurieString));
                sb.append("}");
                sb.append("+");

            }
            value = this.appendQueryModifiers(value, optionList);
            if (keyProperty instanceof IndexEntryField) {
                sb.append(((IndexEntryField) keyProperty).getName()).append(":").append(value);
                //                        .append(")");
            }
            else if (keyProperty instanceof RdfProperty) {
                URI curie = ((RdfProperty) keyProperty).getCurie();
                String escapedCurieField = escapeField(curie.toString());
                sb.append(escapedCurieField).append(":").append(value);
                //                        .append(")");
            }
            else {
                throw new UnsupportedOperationException("encountered an invalid key");
            }
            if (optionList.contains(FilteredSearchRequest.QueryType.FILTER)) {
                this.appendBoolean(this.filterQueryBuilder, filterBoolean).append("(").append(sb).append(")");
            }
            else if (optionList.contains(FilteredSearchRequest.QueryType.MAIN)) {
                //                sb.append(")");
                queryBuilder.append(sb);
            }
            else {
                throw new UnsupportedOperationException("please define the QueryType");
            }
        }
        else {
            throw new UnsupportedOperationException("please define the join type");
        }
        return this;
    }

    @Override
    public IndexSearchRequest filter(RdfProperty property, String value, FilterBoolean filterBoolean, Option... options)
    {
        // I think it makes sense to force phrase search for field filters, right?
        //not really, if we want to query things like ranges, these should not be escaped. Using it as default makes sense though.
        Option customValueOption = null;
        if (options != null) {
            for (Option option : options) {
                if (option instanceof ValueOption) {
                    customValueOption = option;
                }
            }
        }
        Set valueOptions = customValueOption == null ? Collections.singleton(ValueOption.phraseSearch) : Collections.singleton(customValueOption);

        this.appendFilter(this.filterQueryBuilder, filterBoolean, this.nameOf(property), value, Arrays.asList(options), valueOptions);

        return this;
    }
    @Override
    public IndexSearchRequest missing(RdfProperty property, FilterBoolean filterBoolean)
    {
        this.appendFilter(this.filterQueryBuilder, filterBoolean, this.nameOf(property), "[* TO *]", Collections.singleton(InternalValueOption.noEscape));

        return this;
    }
    @Override
    public IndexSearchRequest missing(IndexEntryField field, FilterBoolean filterBoolean) throws IOException
    {
        this.appendFilter(this.filterQueryBuilder, filterBoolean, field.getName(), "[* TO *]", Collections.singleton(InternalValueOption.noEscape));

        return this;
    }

    @Override
    public IndexSearchRequest transformers(Option... transformers)
    {
        String fieldListParam = "fl";
        StringBuilder fieldList = new StringBuilder("*");
        List<String> fieldLists = this.customParams.get(fieldListParam);
        if (fieldLists != null) {
            throw new UnsupportedOperationException("transformers can only be declared once");
        }
        fieldLists = new ArrayList<>();
        for (Option option : transformers) {
            if (!(option instanceof TransformerValueOption)) {
                throw new UnsupportedOperationException(option + " is not an instance of TransformerValueOption");
            }
            fieldList.append(",");
            fieldList.append(((TransformerValueOption) option).getValue());
        }
        fieldLists.add(fieldList.toString());
        this.customParams.put(fieldListParam, fieldLists);
        return this;
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

        return this;
    }

    //-----PROTECTED METHODS-----
    SolrQuery buildSolrQuery()
    {
        //Note: in Solr, a query is always necessary, so we start out with searching for everything
        SolrQuery retVal = new SolrQuery("*:*");

        if (this.queryBuilder.length() > 0) {
            retVal.setQuery(this.queryBuilder.toString());
        }
        else {
            retVal.setQuery("*:*");
        }

        if (this.filterQueryBuilder.length() > 0) {
            retVal.setFilterQueries(this.filterQueryBuilder.toString());
        }
        else {
            retVal.setFilterQueries();
        }
        //add custom parameters
        if (this.customParams.keySet().size() > 0) {
            for (String key : this.customParams.keySet()) {
                if (!key.equals("fq")) {
                    retVal.setParam(key, this.customParams.get(key).toArray(new String[this.customParams.get(key).size()]));
                }
            }
        }

        // the above call overwrites all filters, so make sure to add the language grouping filter
        // again if we have one
        if (this.languageGroupFilter != null) {
            retVal.addFilterQuery(this.languageGroupFilter);
        }

        // update sort
        retVal.clearSorts();
        for (Map.Entry<String, Boolean> e : this.sortFields.entrySet()) {
            retVal.addSort(e.getKey(), e.getValue() ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc);
        }

        // zero based offset of matching documents to retrieve
        retVal.setStart(this.getPageOffset() * this.getPageSize());

        // number of documents to return starting at "start"
        retVal.setRows(this.getPageSize());

        return retVal;
    }

    //-----PRIVATE METHODS-----
    private StringBuilder appendFilter(StringBuilder stringBuilder, FilterBoolean filterBoolean, String field, String value, Iterable<Option>... options)
    {
        // calc the flags on the raw incoming value
        value = this.appendQueryModifiers(value, options);

        this.appendBoolean(stringBuilder, filterBoolean).append("(").append(this.escapeField(field)).append(":").append(value).append(")");

        return stringBuilder;
    }

    private String appendQueryModifiers(String value, Iterable<Option>... options)
    {
        Iterable<Option> mergedOptions = Iterables.concat(options);

        // we always at least do a trim on the value
        value = value.trim();

        boolean isNumber = !NumberUtils.isNumber(value);
        boolean hasAsterisk = value.contains("*");

        String prefix = "";
        String suffix = "";
        boolean doEscape = true;
        boolean escaped = false;
        for (Option option : mergedOptions) {

            if (option.equals(ValueOption.termSearch)) {

                value = this.escapeTerm(value);

                escaped = true;
            }
            else if (option.equals(ValueOption.phraseSearch)) {

                // this must be done first or the quotes added below would get escaped
                value = this.escapePhrase(value);

                if (value.startsWith("\"") && value.endsWith("\"")) {
                    // no need to add quotes here, they're already here
                }
                else {
                    value = "\"" + value + "\"";
                }

                escaped = true;
            }
            else if (option.equals(ValueOption.rangeSearch)) {
                doEscape = false;
            }
            else if (option.equals(InternalValueOption.noEscape)) {
                doEscape = false;
            }
            // wildcards for numbers don't really make sense, because it expands the search results way too much
            // if the user added their own wildcard in the query, it kind of makes sense to skip it as well
            else if (option.equals(ValueOption.wildcardSuffix) && !isNumber && !hasAsterisk) {
                suffix = "*";
            }
            else if (option.equals(ValueOption.wildcardPrefix) && !isNumber && !hasAsterisk) {
                prefix = "*";
            }
            else if (option.equals(ValueOption.fuzzy)) {
                suffix = "~";
            }
        }

        // perform a default escape if it hasn't been don't yet
        if (doEscape && !escaped) {
            value = this.escapeDefault(value);
        }

        return prefix + value + suffix;
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
                case NONE:
                    //do not append a filterBoolean
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
                case NONE:
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
    private String escapeField(String field)
    {
        return QueryParser.escape(field);
    }
    private String escapeDefault(String value)
    {
        // this is the most stringent mode: escape everything and don't allow any custom modifiers
        //return this.escapeTerm(value);

        // this is the most liberal mode: allow everything (but eg. it crashes when using RDF-colons in values, which happens a lot)
        //return value;

        // by default, we will allow a few special characters, but not all of them.
        // Let's start by allowing the end user to insert:
        // - custom wildcards (also see appendQueryModifiers() wildcard escaping rules)
        // - custom double-quotes
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')' || c == ':'
                || c == '^' || c == '[' || c == ']' /*|| c == '\"'*/ || c == '{' || c == '}' || c == '~'
                /*|| c == '*'*/ || c == '?' || c == '|' || c == '&' || c == '/') {
                sb.append('\\');
            }
            sb.append(c);
        }

        return sb.toString();
    }
    private String escapeTerm(String term)
    {
        return QueryParser.escape(term);
    }
    private String escapePhrase(String phrase)
    {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < phrase.length(); i++) {
            char c = phrase.charAt(i);

            // inspired by http://api.drupalhelp.net/api/apachesolr/Drupal_Apache_Solr_Service.php/function/DrupalApacheSolrService%3A%3AescapePhrase/7
            // We should probably make it smarter to allow inner quoting, like in https://www.drupal.org/project/search_api_solr/issues/3017342
            if (c == '\\' || c == '\"') {
                sb.append('\\');
            }
            sb.append(c);
        }

        return sb.toString();
    }

    //-----MGMT METHODS-----
    @Override
    public String toString()
    {
        return this.buildSolrQuery().toString();
    }
}
