package com.beligum.blocks.fs.indexes;

import com.beligum.blocks.config.Settings;
import com.beligum.blocks.fs.indexes.ifaces.PageIndexer;
import com.beligum.blocks.fs.pages.ifaces.Page;
import org.apache.jena.query.*;

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
    private Dataset dataset;

    //-----CONSTRUCTORS-----
    public JenaPageIndexer()
    {
        //note: it's closed on onServerStopped(), this is just a reference
        this.dataset = Settings.instance().getRDFDataset();
    }

    //-----PUBLIC METHODS-----
    @Override
    public void indexPage(Page page) throws IOException
    {
        try {
            dataset.begin(ReadWrite.WRITE);

            //dataset.addNamedModel(modelName.toString(), model);
            dataset.getDefaultModel().add(page.getRDFModel());

            //just testing...
            //            String qs1 = "SELECT * {?s ?p ?o} LIMIT 10" ;
            //            try(QueryExecution qExec = QueryExecutionFactory.create(qs1, dataset)) {
            //                ResultSet rs = qExec.execSelect() ;
            //                ResultSetFormatter.out(rs) ;
            //            }

            dataset.commit();
        }
        finally {
            dataset.end();
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
