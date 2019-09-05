package com.beligum.blocks.index.solr;

import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.WidgetType;
import com.beligum.blocks.index.entries.JsonPageIndexEntry;
import com.beligum.blocks.index.ifaces.*;
import com.beligum.blocks.index.request.AbstractIndexSearchRequest;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.google.common.collect.Iterables;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.lucene.analysis.CharArrayMap;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.parser.QueryParser;

import java.io.IOException;
import java.util.*;
import java.util.logging.Filter;
import java.util.stream.Stream;

public class SolrIndexSearchRequest extends AbstractIndexSearchRequest implements JoinSearchRequest, GraphSearchRequest {

    //-----CONSTANTS-----
    public enum ValueOption implements IndexSearchRequest.ValueOption {
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

    }

    private enum InternalValueOption implements FilteredSearchRequest.ValueOption {
        /**
         * Explicitly skips the escaping of the Solr-reserved characters in the value, no matter what
         */
        noEscape,
    }

    public enum TransformerValueOption implements FilteredSearchRequest.ValueOption {

        childDocTransformer("[child]"),
        shardAugmenter("[shard]"),
        docid("[docid]");

        private String value;

        TransformerValueOption(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

    }

    public enum GraphSearchOption implements FilteredSearchRequest.ValueOption {
        returnRoot,
        leafNodesOnly
    }

    public enum JoinValueOption implements IndexSearchRequest.ValueOption {

        blokjoin,
        regular

    }

    //-----VARIABLES-----
    private StringBuilder queryBuilder;
    private StringBuilder filterQueryBuilder;
    private String languageGroupFilter;
    private Map<String, List<String>> customParams;


    //-----CONSTRUCTORS-----
    public SolrIndexSearchRequest(IndexConnection indexConnection) {
        super(indexConnection);

        this.queryBuilder = new StringBuilder();
        this.filterQueryBuilder = new StringBuilder();
        this.customParams = new HashMap<>();

    }

    @Override
    public IndexSearchRequest customQuery(String value) {
        if (this.queryBuilder.length() > 0) {
            throw new UnsupportedOperationException("Can not combine a custom query with a second query. Existing query " + this.queryBuilder.toString());
        }
        this.queryBuilder.append(value);
        return this;
    }

    //-----PUBLIC METHODS-----
    @Override
    public IndexSearchRequest search(String value, FilterBoolean filterBoolean, Option... options) {
        this.appendFilter(this.queryBuilder, filterBoolean, SolrConfigs._text_.getName(), value, Arrays.asList(options));

        return this;
    }

    @Override
    public IndexSearchRequest filter(RdfClass type, FilterBoolean filterBoolean) {
//        this.currentFilterTYpe = type;

        this.appendFilter(this.filterQueryBuilder, filterBoolean, this.nameOf(JsonPageIndexEntry.TYPEOF_PROPERTY), ResourceIndexEntry.typeOfField.serialize(type));

        return this;
    }

    @Override
    public IndexSearchRequest filter(IndexEntryField field, String value, FilterBoolean filterBoolean, Option... options) {
        // I think it makes sense to force phrase search for field filters, right?
        this.appendFilter(this.filterQueryBuilder, filterBoolean, field.getName(), value, Arrays.asList(options), Collections.singleton(ValueOption.phraseSearch));

        return this;
    }


//    public JoinSearchRequest join(RdfProperty property, Option... options)
//    {
//        if (this.currentFilterType == null) {
//            throw new Exeption()
//        }
//
//        ...
//
//
//        return new JoinSearchRequest()
//    }

    @Override
    public GraphSearchRequest constructGraph(IndexEntryField from, IndexEntryField to, Option... options) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{!graph from=").append(from.getName()).append(" ");
        stringBuilder.append("to=").append(to.getName());
        for (Option graphSearchValueOption : GraphSearchOption.values()) {
            if (Arrays.stream(options).anyMatch(s -> s.equals(graphSearchValueOption))) {
                stringBuilder.append(" " + graphSearchValueOption.toString() + "=true");
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
    public FilteredSearchRequest appendQuery(IndexSearchRequest subQuery) {
        if (!(this instanceof GraphSearchRequest) || !(this instanceof JoinSearchRequest)) {
            throw new UnsupportedOperationException("this quert can not be appended");
        }
        //add to queryBuilder
       if (subQuery instanceof SolrIndexSearchRequest) {
           this.queryBuilder.append(((SolrIndexSearchRequest) subQuery).queryBuilder);
           //also add the custom parameters to the current query.
           this.customParams.putAll(((SolrIndexSearchRequest) subQuery).customParams);
       }
        return this;
    }


    @Override
    public JoinSearchRequest join(IndexEntryField from, IndexEntryField to, RdfClass rdfClass, List<FilteredSearchRequestWithBoolean> filteredSearchRequests, Option... options) {
        List optionList = Arrays.asList(options);
        StringBuilder stringBuilder = new StringBuilder();
        if (optionList.contains(JoinOption.regular)) {
            if (optionList.contains(BlockJoinOption.child) || optionList.contains(BlockJoinOption.parent)) {
                throw new UnsupportedOperationException(BlockJoinOption.class.getName() + " should only be used by blockjoins.");
            }
            if (from != null && to != null) {
                stringBuilder.append("{!join from=").append(from.getName()).append(" ");
                stringBuilder.append("to=").append(to.getName()).append("}");
            }
            stringBuilder.append("{!filters param=$").append(rdfClass.getName()).append("}");
            //add the filters as custom parameters. These can be null
            if(filteredSearchRequests != null){
                for (FilteredSearchRequestWithBoolean filteredSearchRequest : filteredSearchRequests) {
                    if (filteredSearchRequest.getFilteredSearchRequest() instanceof SolrIndexSearchRequest) {
                        SolrIndexSearchRequest solrFilteredSearchRequest = (SolrIndexSearchRequest) filteredSearchRequest.getFilteredSearchRequest();
                        if (solrFilteredSearchRequest.filterQueryBuilder.length() > 0) {
                            StringBuilder sb = this.appendBoolean(this.filterQueryBuilder, filteredSearchRequest.getFilterBoolean()).append("(").append(solrFilteredSearchRequest.filterQueryBuilder).append(")");
                            List params = this.customParams.get(rdfClass.getName());
                            if (params == null) {
                                params = new ArrayList();
                            }
                            params.add(sb.toString());
                            this.customParams.put(rdfClass.getName(), params);
                        }
                    }
                }
            }

        } else if (optionList.contains(JoinOption.blockjoin)) {
            // {!parent which=typeOf:crb\:Manifestation}(+resource:\/resource\/Aperture\/3d -resourceType:DEFAULT)
            stringBuilder.append("{!");
            if (optionList.contains(BlockJoinOption.parent)) {
                stringBuilder.append("parent");

            } else if (optionList.contains(BlockJoinOption.child)) {
                throw new UnsupportedOperationException("child blockjoin is not supported yet");
            } else {
                throw new UnsupportedOperationException(BlockJoinOption.class.getSimpleName() + "should be included in options");
            }
            stringBuilder.append("which=typeOf" + this.appendQueryModifiers(rdfClass.getCurie().toString())).append("}");
            for (FilteredSearchRequestWithBoolean filteredSearchRequest : filteredSearchRequests) {
                if (filteredSearchRequest.getFilteredSearchRequest() instanceof SolrIndexSearchRequest) {
                    SolrIndexSearchRequest solrFilteredSearchRequest = (SolrIndexSearchRequest) filteredSearchRequest.getFilteredSearchRequest();
                    if (solrFilteredSearchRequest.filterQueryBuilder.length() > 0) {
                        this.appendBoolean(stringBuilder, filteredSearchRequest.getFilterBoolean()).append("(").append(solrFilteredSearchRequest.filterQueryBuilder).append(")");
                    }
                }
            }
        } else {
            throw new UnsupportedOperationException("Type of Join was not defined. Should be a " + JoinOption.class.getName());
        }
        if (this.queryBuilder.length() > 0) {
            throw new UnsupportedOperationException("Can not combine a JoinSearchRequest with a second query. Existing query " + this.queryBuilder.toString());
        }
        //append the queryBuilder
        //if the
        this.queryBuilder.append(stringBuilder);


        return this;
    }

    @Override
    public IndexSearchRequest blockjoinToParent(RdfClass rdfClass, RdfProperty filterProperty, boolean standalone, String... filterValues) throws IOException {
        if (filterValues != null && filterValues.length > 0) {
            List<String> queries = buildBlockjoins(rdfClass, filterProperty, filterValues);
            if (queries != null && queries.size() > 0) {
                if (!standalone) {
                    //check if param already exists
                    List<String> params = this.customParams.get(rdfClass.getName());
                    if (params == null) {
                        params = new ArrayList<>();
                    }
                    params.addAll(queries);
                    this.customParams.put(rdfClass.getName(), params);
                } else {
                    //add as a filterQuery
                    List<String> params = this.customParams.get("fq");
                    if (params == null) {
                        params = new ArrayList<>();
                    }
                    params.addAll(queries);
                    this.customParams.put("fq", params);
                }
            }
        }
        return this;
    }
//
//    @Override
//    public IndexSearchRequest join(IndexEntryField from, IndexEntryField to, IndexEntryField field, String value, Option... options) throws IOException {
//        //{!join from=id fromIndex=movie_directors to=director_id}has_oscar:true
//        StringBuilder stringBuilder = new StringBuilder();
//        stringBuilder.append("{!join from=").append(from.getName());
//        stringBuilder.append(" to=").append(to.getName()).append("}");
//        value = this.appendQueryModifiers(value, Arrays.asList(options));
//        stringBuilder.append("(").append(this.escapeField(field.getName())).append(":").append(value).append(")");
//        return null;
//    }

    @Override
    public IndexSearchRequest joinedGraphTraversalQuery(boolean returnRoot, boolean leafNodesOnly, RdfClass... rdfClasses) {
        if (this.queryBuilder.length() != 0) {
            throw new UnsupportedOperationException("Can not combine this query with another query");
        }
        if (checkDependency(rdfClasses)) {
            //always  start with the lowest class
            //this one will  always be needed
            for (int i = 0; i < rdfClasses.length; i++) {
                if (i == 0) {
                    this.queryBuilder.append("{!graph from=uri to=parentUri returnRoot=");
                    this.queryBuilder.append(returnRoot);
                    this.queryBuilder.append(" leafNodesOnly=");
                    this.queryBuilder.append(leafNodesOnly);
                    this.queryBuilder.append("}{!filters param=$" + rdfClasses[i].getName() + "}");
                } else {
                    this.queryBuilder.append("{!join from=uri to=parentUri}{!filters param=$" + rdfClasses[i].getName() + "}");
                }
            }
        }
        return this;
    }

    @Override
    public IndexSearchRequest filter(RdfProperty property, String value, FilterBoolean filterBoolean, Option... options) {
        // I think it makes sense to force phrase search for field filters, right?
        this.appendFilter(this.filterQueryBuilder, filterBoolean, this.nameOf(property), value, Arrays.asList(options), Collections.singleton(ValueOption.phraseSearch));

        return this;
    }

    @Override
    public IndexSearchRequest missing(RdfProperty property, FilterBoolean filterBoolean) {
        this.appendFilter(this.filterQueryBuilder, filterBoolean, this.nameOf(property), "[* TO *]", Collections.singleton(InternalValueOption.noEscape));

        return this;
    }

    @Override
    public IndexSearchRequest missing(IndexEntryField field, FilterBoolean filterBoolean) throws IOException {
        this.appendFilter(this.filterQueryBuilder, filterBoolean, field.getName(), "[* TO *]", Collections.singleton(InternalValueOption.noEscape));

        return this;
    }

    @Override
    public IndexSearchRequest transformers(Option... transformers) {
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
    public IndexSearchRequest filter(IndexSearchRequest subRequest, FilterBoolean filterBoolean) throws IOException {
        if (subRequest instanceof SolrIndexSearchRequest) {
            SolrIndexSearchRequest solrSubRequest = (SolrIndexSearchRequest) subRequest;
            if (solrSubRequest.queryBuilder.length() > 0) {
                this.appendBoolean(this.queryBuilder, filterBoolean).append("(").append(solrSubRequest.queryBuilder).append(")");
            }
            if (solrSubRequest.filterQueryBuilder.length() > 0) {
                this.appendBoolean(this.filterQueryBuilder, filterBoolean).append("(").append(solrSubRequest.filterQueryBuilder).append(")");
            }
        } else {
            throw new IOException("Unsupported sub query type; " + subRequest);
        }

        return this;
    }

    @Override
    public IndexSearchRequest language(Locale language) {
        super.language(language);

        this.filter(PageIndexEntry.languageField, this.getLanguage().getLanguage(), FilterBoolean.AND);

        return this;
    }

    @Override
    public IndexSearchRequest language(Locale language, IndexEntryField field) {
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
    SolrQuery buildSolrQuery() {
        //Note: in Solr, a query is always necessary, so we start out with searching for everything
        SolrQuery retVal = new SolrQuery("*:*");

        if (this.queryBuilder.length() > 0) {
            retVal.setQuery(this.queryBuilder.toString());
        } else {
            retVal.setQuery("*:*");
        }

        if (this.filterQueryBuilder.length() > 0) {
            retVal.setFilterQueries(this.filterQueryBuilder.toString());
        } else {
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
        retVal.setStart(this.getPageOffset());

        // number of documents to return starting at "start"
        retVal.setRows(this.getPageSize());

        return retVal;
    }

    //-----PRIVATE METHODS-----
    private StringBuilder appendFilter(StringBuilder stringBuilder, FilterBoolean filterBoolean, String field, String value, Iterable<Option>... options) {
        // calc the flags on the raw incoming value
        value = this.appendQueryModifiers(value, options);

        this.appendBoolean(stringBuilder, filterBoolean).append("(").append(this.escapeField(field)).append(":").append(value).append(")");

        return stringBuilder;
    }

    private String appendQueryModifiers(String value, Iterable<Option>... options) {
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
            } else if (option.equals(ValueOption.phraseSearch)) {

                // this must be done first or the quotes added below would get escaped
                value = this.escapePhrase(value);

                if (value.startsWith("\"") && value.endsWith("\"")) {
                    // no need to add quotes here, they're already here
                } else {
                    value = "\"" + value + "\"";
                }

                escaped = true;
            } else if (option.equals(InternalValueOption.noEscape)) {
                doEscape = false;
            }
            // wildcards for numbers don't really make sense, because it expands the search results way too much
            // if the user added their own wildcard in the query, it kind of makes sense to skip it as well
            else if (option.equals(ValueOption.wildcardSuffix) && !isNumber && !hasAsterisk) {
                suffix = "*";
            } else if (option.equals(ValueOption.wildcardPrefix) && !isNumber && !hasAsterisk) {
                prefix = "*";
            } else if (option.equals(ValueOption.fuzzy)) {
                suffix = "~";
            }
        }

        // perform a default escape if it hasn't been don't yet
        if (doEscape && !escaped) {
            value = this.escapeDefault(value);
        }

        return prefix + value + suffix;
    }

    private StringBuilder appendBoolean(StringBuilder stringBuilder, FilterBoolean filterBoolean) {
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
        } else {
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

    private String escapeField(String field) {
        return QueryParser.escape(field);
    }

    private String escapeDefault(String value) {
        // this is the most stringent mode: escape everything and don't allow any custom modifiers
        //return this.escapeTerm(value);
        // this is the most liberal mode: allow everything (but eg. it crashes when using RDF-colons in values, which happens a lot)
        //return value;

        // by default, we allow a few special characters, but not all of them.
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

    private String escapeTerm(String term) {
        return QueryParser.escape(term);
    }

    private String escapePhrase(String phrase) {
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

    private boolean checkDependency(RdfClass... rdfClasses) {
        if (rdfClasses.length > 1) {
            //check parent-childl relationship
            for (int i = 1; i < rdfClasses.length; i++) {
                boolean found = false;
                //start with  second class. Has to be a child of the first, etc.
                Iterator<RdfProperty> rdfPropertyIterator = rdfClasses[i].getProperties().iterator();
                while (rdfPropertyIterator.hasNext()) {
                    RdfProperty rdfProperty = rdfPropertyIterator.next();
                    if (rdfProperty.getDataType().equals(rdfClasses[i - 1])) {
                        found = true;
                    }
                }
                if (!found) {
                    Logger.error(rdfClasses[i].getCurie().toString() + " is does not have  a dependency on " + rdfClasses[i - 1].getCurie().toString() + ". Can't continue");
                }
            }
        }
        return true;
    }
    private List<String> buildBlockjoins(RdfClass rdfClass, RdfProperty filterProperty, String... filterValues) throws IOException {
        String defaultFilterVariable = ResourceIndexEntry.Type.DEFAULT.toString();
        String resourceTypeFilterKey = ResourceIndexEntry.resourceTypeField.getName();
        String defaultFilterQuery = resourceTypeFilterKey + ":" + defaultFilterVariable;

        String queryPrefix = "{!parent which=typeOf:";
        String queryClosingCurlyBracket = "}";
        String queryClosingParenthesis = ")";
        String queryOpeningParenthesis = "(";

        String column = ":";
        String space = " ";
        String plus = "+";
        List<String> queries = new ArrayList<>();

        if (filterValues != null && filterValues.length > 0) {
            String queryString;
            //OPTION 1: number. Allows for range as [number - number]
            if (filterProperty.getWidgetType().equals(WidgetType.Number)) {
                //just a single filterValue allowed
                if (filterValues.length > 1) {
                    throw new IOException("just a single filter value allowed for " + WidgetType.Number);
                }
                String filterValue = filterValues[0].replaceAll(space, "");
                filterValue = filterValue.replace("-", " TO ");
                filterValue = "[" + filterValue + "]";
                queryString = queryPrefix + QueryParser.escape(rdfClass.getCurie().toString()) + queryClosingCurlyBracket + QueryParser.escape(filterProperty.getCurie().toString()) + column + filterValue;
                queries.add(queryString);
            } else if (filterProperty.getWidgetType().equals(WidgetType.Resource)) {
                if (filterValues.length > 1) {
                    throw new IOException("just a single filter value allowed for " + WidgetType.Resource);
                }
                queryString =
                        queryPrefix + QueryParser.escape(rdfClass.getCurie().toString()) + queryClosingCurlyBracket + queryOpeningParenthesis + plus + ResourceIndexEntry.resourceField.getName() + column +
                                QueryParser.escape(filterValues[0]);
                String addedString = " -" + defaultFilterQuery;
                queryString = queryString + addedString + queryClosingParenthesis;
                queries.add(queryString);
            } else if (filterProperty.getWidgetType().equals(WidgetType.Object)) {
                if (filterProperty.getDataType().getMainProperty() != null &&
                        filterProperty.getDataType().getMainProperty().getDataType() != null &&
                        filterProperty.getDataType().getMainProperty().getDataType().getEndpoint() != null &&
                        filterProperty.getDataType().getMainProperty().getDataType().getEndpoint().isExternal()) {
                    //external  endpoint (e.g.wikidata)
                    queryString = queryPrefix + QueryParser.escape(rdfClass.getCurie().toString()) + queryClosingCurlyBracket;

                    String tempString = queryOpeningParenthesis + plus + QueryParser.escape(ResourceIndexEntry.resourceField.getName()) + column + QueryParser.escape(filterValues[0]);
                    queryString = queryString + tempString;
                    //if we are  looking up  more than  one filterValue, iterate and add  them.
                    if (filterValues.length > 1) {
                        //obviously, ignore  the  first
                        for (int i = 1; i < filterValues.length; i++) {
                            String extraValue = filterValues[i];
                            if (!StringUtils.isEmpty(extraValue)) {
                                String typePropertyString =
                                        space + plus + queryPrefix + QueryParser.escape(filterProperty.getDataType().getCurie().toString()) + queryClosingCurlyBracket + plus +
                                                ResourceIndexEntry.resourceField.getName() + column + QueryParser.escape(extraValue);
                                queryString = queryString + typePropertyString;
                            }
                        }
                    }
                    queryString = queryString + queryClosingParenthesis;
                    queries.add(queryString);
                } else if (filterProperty.getDataType().getMainProperty().getWidgetType().equals(WidgetType.Resource)) {
                    String basicString = "+(" + queryPrefix + QueryParser.escape(rdfClass.getCurie().toString()) + queryClosingCurlyBracket;
                    String resourceString = plus + QueryParser.escape(ResourceIndexEntry.resourceField.getName()) + column + QueryParser.escape(filterValues[0]);
                    queryString = basicString + resourceString + queryClosingParenthesis;
                    if (filterValues.length > 1) {
                        //obviously, ignore  the  first
                        for (int i = 1; i < filterValues.length; i++) {
                            String extraValue = filterValues[i];
                            String typePropertyString = plus + ResourceIndexEntry.resourceField.getName() + column + QueryParser.escape(extraValue);
                            typePropertyString = basicString + typePropertyString;
                            queryString = queryString + space + typePropertyString + queryClosingParenthesis;
                        }
                    }
                    queries.add(queryString);
                } else {
                    queryString =
                            queryPrefix + QueryParser.escape(rdfClass.getCurie().toString()) + queryClosingCurlyBracket + plus +
                                    QueryParser.escape(filterProperty.getDataType().getMainProperty().getCurie().toString()) + column + QueryParser.escape(filterValues[0]);
                    if (filterValues.length > 1) {
                        //obviously, ignore  the  first
                        for (int i = 1; i < filterValues.length; i++) {
                            String extraValue = filterValues[i];
                            String typePropertyString =
                                    space + plus + queryPrefix + QueryParser.escape(filterProperty.getDataType().getCurie().toString()) + queryClosingCurlyBracket + plus +
                                            ResourceIndexEntry.resourceField.getName() + column + QueryParser.escape(extraValue);
                            queryString = queryString + typePropertyString;
                        }
                    }
                    queries.add(queryString);
                }
            }
        }
        return queries;
    }

    //-----MGMT METHODS-----
    @Override
    public String toString() {
        return this.buildSolrQuery().toString();
    }

}
