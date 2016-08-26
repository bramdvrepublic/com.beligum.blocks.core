package com.beligum.blocks.controllers;

import com.beligum.base.cache.CacheKey;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.RdfFactory;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.fs.index.LucenePageIndexer;
import com.beligum.blocks.fs.index.entries.pages.IndexSearchRequest;
import com.beligum.blocks.fs.index.entries.pages.IndexSearchResult;
import com.beligum.blocks.fs.index.entries.pages.PageIndexEntry;
import com.beligum.blocks.fs.index.ifaces.LuceneQueryConnection;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.templating.blocks.DefaultTemplateController;
import gen.com.beligum.blocks.core.constants.blocks.core;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.TermQuery;

import java.io.IOException;
import java.net.URI;
import java.util.*;

import static com.beligum.blocks.controllers.SearchController.CacheKeys.SEARCH_REQUEST;
import static com.beligum.blocks.controllers.SearchController.CacheKeys.SEARCH_RESULT;

/**
 * Created by bram on 6/6/16.
 */
public class SearchController extends DefaultTemplateController
{
    //-----CONSTANTS-----
    public static final int FIRST_PAGE_INDEX = 0;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;

    public enum CacheKeys implements CacheKey
    {
        SEARCH_REQUEST,
        SEARCH_RESULT
    }

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    @Override
    public void created()
    {
        if (!R.cacheManager().getRequestCache().containsKey(SEARCH_RESULT)) {
            try {
                Locale locale = R.i18nFactory().getOptimalLocale();

                //Set the searchterm
                String searchTerm = getQueryParam(core.SEARCH_PARAM_QUERY);

                //set the sort field
                RdfProperty sortField = null;
                String sortParam = getQueryParam(core.SEARCH_PARAM_SORT);
                if (!StringUtils.isEmpty(sortParam)) {
                    sortField = (RdfProperty) RdfFactory.getForResourceType(URI.create(sortParam));
                }

                // Set the page index
                int pageIndex = FIRST_PAGE_INDEX;
                String pageIndexParam = null;
                try {
                    pageIndexParam = getQueryParam(core.SEARCH_PARAM_INDEX);
                    if (pageIndexParam != null) {
                        pageIndex = Integer.parseInt(pageIndexParam);
                    }
                }
                catch (Exception e) {
                    Logger.warn("Invalid search index offset; setting it to 0; " + pageIndexParam);
                }

                // Set the page size
                int pageSize = DEFAULT_PAGE_SIZE;
                String pageSizeParam = null;
                try {
                    pageSizeParam = getQueryParam(core.SEARCH_PARAM_SIZE);
                    if (pageSizeParam != null) {
                        pageSize = Math.min(Integer.parseInt(pageSizeParam), MAX_PAGE_SIZE);
                    }
                }
                catch (Exception e) {
                    Logger.warn("Invalid search size offset; setting it to 0; " + pageSizeParam);
                }

                //let's not return nulls, so we can always use .size() and so on
                IndexSearchResult searchResult = new IndexSearchResult(new ArrayList<>());

                LuceneQueryConnection queryConnection = StorageFactory.getMainPageQueryConnection();
                org.apache.lucene.search.BooleanQuery pageQuery = new org.apache.lucene.search.BooleanQuery();

                pageQuery.add(new TermQuery(new Term(PageIndexEntry.Field.language.name(), locale.getLanguage())), BooleanClause.Occur.FILTER);

                //we give precedence to the query param (because it's more dynamic),
                //but if none is supplied, we allow an argument to be set on the search results import too
                String typeOf = getQueryParam(core.SEARCH_PARAM_TYPE);
                if (StringUtils.isEmpty(typeOf)) {
                    typeOf = this.config.get(core.SEARCH_BOX_TYPE_ARG);
                }

                RdfClass rdfClass = typeOf == null ? null : RdfFactory.getClassForResourceType(URI.create(typeOf));
                if (rdfClass != null) {
                    pageQuery.add(new TermQuery(new Term(PageIndexEntry.Field.typeOf.name(), rdfClass.getCurieName().toString())), BooleanClause.Occur.FILTER);
                }
                else {
                    if (!StringUtils.isEmpty(typeOf)) {
                        throw new java.text.ParseException("Can't seem to find type '"+typeOf+"' in attribute " + core.SEARCH_BOX_TYPE_ARG + " on this tag, this shouldn't happen", 0);
                    }
                }

                //filters entries with specific field-values
                Map<RdfProperty, List<String>> fieldFilters = this.parseFilters(getQueryParams(gen.com.beligum.blocks.core.constants.blocks.core.SEARCH_PARAM_FILTERS), pageQuery, locale);

                if (!StringUtils.isEmpty(searchTerm)) {
                    pageQuery.add(queryConnection.buildWildcardQuery(null, searchTerm, false), BooleanClause.Occur.MUST);
                }

                //this.searchResult = StorageFactory.getTriplestoreQueryConnection().search(rdfClass, searchTerm, new HashMap<RdfProperty, String>(), sortField, false, RESOURCES_ON_PAGE, selectedPage, R.i18nFactory().getOptimalLocale());
                searchResult = queryConnection.search(pageQuery, sortField, false, pageSize, pageIndex);

                //save the results format in the cached value so we can use it across different instances
                String resultsFormat = this.config.get(core.SEARCH_BOX_RESULTS_FORMAT);
                if (StringUtils.isEmpty(resultsFormat)) {
                    //let's default to a list
                    resultsFormat = core.SEARCH_RESULTS_FORMAT_LIST;
                }

                R.cacheManager().getRequestCache().put(SEARCH_REQUEST, new IndexSearchRequest(searchTerm, fieldFilters, sortField, resultsFormat));
                R.cacheManager().getRequestCache().put(SEARCH_RESULT, searchResult);
            }
            catch (Exception e) {
                Logger.error("Error while executing search query", e);
            }
        }
    }

    //-----PUBLIC METHODS-----
    public IndexSearchRequest getSearchRequest()
    {
        return (IndexSearchRequest) R.cacheManager().getRequestCache().get(SEARCH_REQUEST);
    }
    public IndexSearchResult getSearchResult()
    {
        return (IndexSearchResult) R.cacheManager().getRequestCache().get(SEARCH_RESULT);
    }

    //-----PROTECTED METHODS-----
    protected String getQueryParam(String name)
    {
        String retVal = null;
        List<String> query = R.requestContext().getJaxRsRequest().getUriInfo().getQueryParameters().get(name);
        if (query != null && query.size() > 0) {
            retVal = query.get(0).trim();
        }
        return retVal;
    }
    protected List<String> getQueryParams(String name)
    {
        List<String> retVal = R.requestContext().getJaxRsRequest().getUriInfo().getQueryParameters().get(name);
        return retVal == null ? new ArrayList<>() : retVal;
    }
    protected Map<RdfProperty, List<String>> parseFilters(List<String> filters, org.apache.lucene.search.BooleanQuery query, Locale locale) throws IOException
    {
        //TODO it probably makes sense to activate this (working code!) for some cases; eg if the filter-field is a boolean with value 'false', you may want to include the entries without such a field at all too
        boolean includeNonExisting = false;

        Map<RdfProperty, List<String>> retVal = new LinkedHashMap<>();

        if (filters != null) {
            for (String filter : filters) {
                if (StringUtils.isEmpty(filter)) {
                    String[] keyVal = filter.split(core.SEARCH_PARAM_DELIM);

                    if (keyVal.length == 2) {
                        RdfProperty key = (RdfProperty) RdfFactory.getForResourceType(URI.create(keyVal[0]));
                        if (key != null) {
                            Object val = key.prepareIndexValue(keyVal[1], locale);
                            String valStr = val == null ? null : val.toString();
                            if (!StringUtils.isEmpty(valStr)) {
                                if (includeNonExisting) {
                                    //the following is the Lucene logic for: if you find a field, it should match x, but if you don't find such a field, include it as well
                                    String fieldName = key.getCurieName().toString();
                                    org.apache.lucene.search.BooleanQuery subQuery = new org.apache.lucene.search.BooleanQuery();
                                    subQuery.add(new TermQuery(new Term(fieldName, valStr)), BooleanClause.Occur.SHOULD);

                                    //see https://kb.ucla.edu/articles/pure-negation-query-in-lucene
                                    //and https://wiki.apache.org/lucene-java/LuceneFAQ#How_does_one_determine_which_documents_do_not_have_a_certain_term.3F
                                    org.apache.lucene.search.BooleanQuery fakeNegationQuery = new org.apache.lucene.search.BooleanQuery();
                                    fakeNegationQuery.add(new MatchAllDocsQuery(), BooleanClause.Occur.MUST);
                                    fakeNegationQuery.add(new TermQuery(new Term(LucenePageIndexer.CUSTOM_FIELD_FIELDS, fieldName)), BooleanClause.Occur.MUST_NOT);
                                    subQuery.add(fakeNegationQuery, BooleanClause.Occur.SHOULD);

                                    query.add(subQuery, BooleanClause.Occur.FILTER);
                                }
                                else {
                                    query.add(new TermQuery(new Term(key.getCurieName().toString(), valStr)), BooleanClause.Occur.FILTER);
                                }

                                List<String> values = retVal.get(key);
                                if (values == null) {
                                    retVal.put(key, values = new ArrayList<String>());
                                }
                                values.add(valStr);
                            }
                        }
                        else {
                            Logger.warn("Encountered unknown RDF property in search filter; ignoring filter" + filter);
                        }
                    }
                    else {
                        Logger.warn("Encountered search filter value with a wrong syntax (not parsable to key/value); ignoring filter; " + filter);
                    }
                }
            }
        }

        return retVal;
    }

    //-----PRIVATE METHODS-----

}
