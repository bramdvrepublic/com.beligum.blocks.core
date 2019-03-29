package com.beligum.blocks.filesystem.index.solr;

import com.beligum.blocks.filesystem.index.ifaces.IndexSearchRequest;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.params.SolrParams;

public class SolrRequestParser
{
    private final IndexSearchRequest request;
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private SolrParams cachedParams;

    //-----CONSTRUCTORS-----
    public SolrRequestParser(IndexSearchRequest request)
    {
        this.request = request;
    }

    //-----PUBLIC METHODS-----
    public SolrParams getSolrParams()
    {
        if (this.cachedParams == null) {
            this.cachedParams = this.parse(this.request);
        }

        return this.cachedParams;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private SolrParams parse(IndexSearchRequest request)
    {
        //    List<Filter> getFilters();
        //    RdfProperty getSortProperty();
        //    boolean isSortAscending();
        //    int getPageSize();
        //    int getPageOffset();
        //    Locale getLanguage();
        //    long getMaxResults();

        SolrQuery retVal = new SolrQuery();

        for (IndexSearchRequest.Filter filter : request.getFilters()) {

        }

        return retVal;
    }
}
