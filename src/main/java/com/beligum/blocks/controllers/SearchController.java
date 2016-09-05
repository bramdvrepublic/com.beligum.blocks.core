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
import static gen.com.beligum.blocks.core.constants.blocks.core.SEARCH_RESULTS_FORMAT_LIST;

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
        IndexSearchRequest searchRequest = this.getSearchRequest();

        if (searchRequest.getSearchTerm() == null) {
            searchRequest.setSearchTerm(getQueryParam(core.SEARCH_PARAM_QUERY));
        }

        if (searchRequest.getFieldFilters() == null) {
            searchRequest.setFieldFilters(getQueryParams(core.SEARCH_PARAM_FILTERS));
        }

        if (searchRequest.getTypeOf() == null) {
            RdfClass typeOf = null;
            String typeOfParam = null;
            try {
                typeOfParam = this.getParam(core.SEARCH_PARAM_TYPE, core.SEARCH_BOX_TYPE_ARG, null);
                if (!StringUtils.isEmpty(typeOfParam)) {
                    typeOf = RdfFactory.getClassForResourceType(URI.create(typeOfParam));
                }
            }
            catch (Exception e) {
                Logger.warn("Invalid type of class; " + typeOfParam);
            }

            searchRequest.setTypeOf(typeOf);
        }

        if (searchRequest.getSortField() == null) {
            RdfProperty sortField = null;
            String sortParam = getQueryParam(core.SEARCH_PARAM_SORT);
            if (!StringUtils.isEmpty(sortParam)) {
                sortField = (RdfProperty) RdfFactory.getForResourceType(URI.create(sortParam));
            }

            searchRequest.setSortField(sortField);
        }

        if (searchRequest.getPageIndex() == null) {
            Integer pageIndex = null;
            String pageIndexParam = null;
            try {
                pageIndexParam = this.getParam(core.SEARCH_PARAM_INDEX, core.SEARCH_RESULTS_INDEX_ARG, null);
                if (!StringUtils.isEmpty(pageIndexParam)) {
                    pageIndex = Math.max(Integer.parseInt(pageIndexParam), FIRST_PAGE_INDEX);
                }
            }
            catch (Exception e) {
                Logger.warn("Invalid page index; " + pageIndexParam);
            }

            searchRequest.setPageIndex(pageIndex);
        }

        if (searchRequest.getPageSize() == null) {
            Integer pageSize = null;
            String pageSizeParam = null;
            try {
                pageSizeParam = this.getParam(core.SEARCH_PARAM_SIZE, core.SEARCH_RESULTS_SIZE_ARG, null);
                if (!StringUtils.isEmpty(pageSizeParam)) {
                    pageSize = Math.min(Integer.parseInt(pageSizeParam), MAX_PAGE_SIZE);
                }
            }
            catch (Exception e) {
                Logger.warn("Invalid page size; " + pageSizeParam);
            }

            searchRequest.setPageSize(pageSize);
        }

        if (searchRequest.getFormat() == null) {
            String format = null;
            String resultsFormatConfig = this.config.get(core.SEARCH_RESULTS_FORMAT_ARG);
            if (!StringUtils.isEmpty(resultsFormatConfig)) {
                format = resultsFormatConfig;
            }

            searchRequest.setFormat(format);
        }
    }

    //-----PUBLIC METHODS-----
    public IndexSearchRequest getSearchRequest()
    {
        if (!R.cacheManager().getRequestCache().containsKey(SEARCH_REQUEST)) {
            R.cacheManager().getRequestCache().put(SEARCH_REQUEST, new IndexSearchRequest());
        }

        return (IndexSearchRequest) R.cacheManager().getRequestCache().get(SEARCH_REQUEST);
    }
    /**
     * Note: the concept for the lazy loaded results is to postpone the use (the effective query) of the request object as long as possible
     * up until the moment it's actually requested (from the template engine), and use the constructor of each controller instance to
     * add more config variables to the request object, so we effectively 'merge' the configs of the search-blocks on a page.
     */
    public IndexSearchResult getSearchResult()
    {
        if (!R.cacheManager().getRequestCache().containsKey(SEARCH_RESULT)) {

            //let's not return nulls, so we can always use .size() and so on
            IndexSearchResult searchResult = new IndexSearchResult(new ArrayList<>());
            try {
                IndexSearchRequest searchRequest = this.getSearchRequest();
                Locale locale = R.i18nFactory().getOptimalLocale();

                //set some defaults if still empty..
                if (searchRequest.getPageIndex() == null) {
                    searchRequest.setPageIndex(FIRST_PAGE_INDEX);
                }
                if (searchRequest.getPageSize() == null) {
                    searchRequest.setPageSize(DEFAULT_PAGE_SIZE);
                }
                if (searchRequest.getFormat() == null) {
                    searchRequest.setFormat(SEARCH_RESULTS_FORMAT_LIST);
                }

                LuceneQueryConnection queryConnection = StorageFactory.getMainPageQueryConnection();

                org.apache.lucene.search.BooleanQuery pageQuery = new org.apache.lucene.search.BooleanQuery();

                pageQuery.add(new TermQuery(new Term(PageIndexEntry.Field.language.name(), locale.getLanguage())), BooleanClause.Occur.FILTER);

                if (searchRequest.getTypeOf() != null) {
                    pageQuery.add(new TermQuery(new Term(PageIndexEntry.Field.typeOf.name(), searchRequest.getTypeOf().getCurieName().toString())), BooleanClause.Occur.FILTER);
                }

                this.addFieldFilters(searchRequest.getFieldFilters(), pageQuery, locale);

                if (!StringUtils.isEmpty(searchRequest.getSearchTerm())) {
                    pageQuery.add(queryConnection.buildWildcardQuery(null, searchRequest.getSearchTerm(), false), BooleanClause.Occur.MUST);
                }

                //this.searchResult = StorageFactory.getTriplestoreQueryConnection().search(rdfClass, searchTerm, new HashMap<RdfProperty, String>(), sortField, false, RESOURCES_ON_PAGE, selectedPage, R.i18nFactory().getOptimalLocale());
                searchResult = queryConnection.search(pageQuery, searchRequest.getSortField(), false, searchRequest.getPageSize(), searchRequest.getPageIndex());
            }
            catch (Exception e) {
                Logger.error("Error while executing search query", e);
            }

            R.cacheManager().getRequestCache().put(SEARCH_RESULT, searchResult);
        }

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
    protected String getParam(String queryParam, String configParam, String defaultValue)
    {
        //first, we try the query param
        String retVal = this.getQueryParam(queryParam);

        //then the config value in the html
        //Note: an empty value is also a value, so don't check for empty values here
        if (retVal == null) {
            retVal = this.config.get(configParam);
        }

        if (retVal == null) {
            retVal = defaultValue;
        }

        return retVal;
    }
    protected Map<RdfProperty, List<String>> addFieldFilters(List<String> filters, org.apache.lucene.search.BooleanQuery query, Locale locale) throws IOException
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
