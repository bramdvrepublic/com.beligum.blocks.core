package com.beligum.blocks.fs.index;

import com.beligum.base.server.R;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.fs.index.entries.PageIndexEntry;
import com.beligum.blocks.fs.index.ifaces.PageIndexer;
import com.beligum.blocks.fs.pages.ifaces.Page;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.query.*;
import org.apache.jena.tdb.TDBFactory;

import java.io.IOException;
import java.net.URI;

/**
 * Created by bram on 1/26/16.
 * <p/>
 * Some interesting reads:
 * <p/>
 * https://jena.apache.org/documentation/tdb/java_api.html
 */
public class JenaPageIndexer implements PageIndexer<SelectBuilder, Query, QueryExecution>
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private Object datasetLock;

    //-----CONSTRUCTORS-----
    public JenaPageIndexer()
    {
        this.datasetLock = new Object();
    }

    //-----PUBLIC METHODS-----
    @Override
    public PageIndexEntry get(URI key) throws IOException
    {
        return null;
    }
    @Override
    public void delete(Page page) throws IOException
    {
        //TODO
    }
    @Override
    public void indexPage(Page page) throws IOException
    {
        Dataset dataset = this.getRDFDataset();

        //dataset.addNamedModel(modelName.toString(), model);
//        dataset.getDefaultModel().add(page.getRDFModel());

        //just testing...
        //            String qs1 = "SELECT * {?s ?p ?o} LIMIT 10" ;
        //            try(QueryExecution qExec = QueryExecutionFactory.create(qs1, dataset)) {
        //                ResultSet rs = qExec.execSelect() ;
        //                ResultSetFormatter.out(rs) ;
        //            }
    }
    @Override
    public SelectBuilder getNewQueryBuilder() throws IOException
    {
        return new SelectBuilder();
    }
    /**
     * Note that this returns an autoclosable!!!
     * @param query
     * @return
     * @throws IOException
     */
    @Override
    public QueryExecution executeQuery(Query query) throws IOException
    {
        return QueryExecutionFactory.create(query, this.getRDFDataset());
    }
    @Override
    public void beginTransaction() throws IOException
    {
        this.getRDFDataset().begin(ReadWrite.WRITE);
    }
    @Override
    public void commitTransaction() throws IOException
    {
        Dataset dataset = this.getRDFDataset();
        if (dataset.isInTransaction()) {
            dataset.commit();
            dataset.end();
        }
    }
    @Override
    public void rollbackTransaction() throws IOException
    {
        Dataset dataset = this.getRDFDataset();
        if (dataset.isInTransaction()) {
            dataset.abort();
            dataset.end();
        }
    }
    @Override
    public void shutdown()
    {
        if (R.cacheManager().getApplicationCache().containsKey(CacheKeys.RDF_DATASET)) {
            this.getRDFDataset().close();
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private Dataset getRDFDataset()
    {
        synchronized (this.datasetLock) {
            if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.RDF_DATASET)) {
                Dataset dataset = TDBFactory.createDataset(Settings.instance().getPageTripleStoreFolder().getAbsolutePath());
                R.cacheManager().getApplicationCache().put(CacheKeys.RDF_DATASET, dataset);
            }

            return (Dataset) R.cacheManager().getApplicationCache().get(CacheKeys.RDF_DATASET);
        }
    }
}
