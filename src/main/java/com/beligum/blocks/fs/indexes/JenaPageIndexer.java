package com.beligum.blocks.fs.indexes;

import com.beligum.base.server.R;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.fs.indexes.ifaces.PageIndexer;
import com.beligum.blocks.fs.pages.ifaces.Page;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.tdb.TDBFactory;

import java.io.IOException;

/**
 * Created by bram on 1/26/16.
 * <p/>
 * Some interesting reads:
 * <p/>
 * https://jena.apache.org/documentation/tdb/java_api.html
 */
public class JenaPageIndexer implements PageIndexer
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
    public void indexPage(Page page) throws IOException
    {
        Dataset dataset = this.getRDFDataset();

        //dataset.addNamedModel(modelName.toString(), model);
        dataset.getDefaultModel().add(page.getRDFModel());

        //just testing...
        //            String qs1 = "SELECT * {?s ?p ?o} LIMIT 10" ;
        //            try(QueryExecution qExec = QueryExecutionFactory.create(qs1, dataset)) {
        //                ResultSet rs = qExec.execSelect() ;
        //                ResultSetFormatter.out(rs) ;
        //            }
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
