package com.beligum.blocks.fs.indexes;

import com.beligum.blocks.config.Settings;
import com.beligum.blocks.fs.indexes.ifaces.PageIndex;
import com.beligum.blocks.fs.pages.ifaces.Page;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.tdb.TDBFactory;

import java.io.IOException;
import java.net.URI;

/**
 * Created by bram on 1/26/16.
 *
 * Some interesting reads:
 *
 * https://jena.apache.org/documentation/tdb/java_api.html
 *
 */
public class JenaPageIndex implements PageIndex
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private Dataset dataset;

    //-----CONSTRUCTORS-----
    public JenaPageIndex()
    {
        this.dataset = TDBFactory.createDataset(Settings.instance().getPageTripleStoreFolder().getAbsolutePath());
    }

    //-----PUBLIC METHODS-----
    public void writeModel(URI modelName, Model model)
    {
        boolean success = false;
        try {
            dataset.begin(ReadWrite.WRITE);

            dataset.addNamedModel(modelName.toString(), model);

            success = true;
        }
        finally {
            if (success) {
                dataset.commit();
            }
            else {
                dataset.abort();
            }
        }
    }
    @Override
    public void indexPage(Page page) throws IOException
    {
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
