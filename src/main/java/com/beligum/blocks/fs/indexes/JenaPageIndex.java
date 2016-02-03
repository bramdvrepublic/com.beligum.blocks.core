package com.beligum.blocks.fs.indexes;

import com.beligum.blocks.config.Settings;
import com.beligum.blocks.fs.indexes.ifaces.PageIndex;
import com.beligum.blocks.fs.pages.ifaces.Page;
import org.apache.jena.query.Dataset;
import org.apache.jena.tdb.TDBFactory;

import java.io.IOException;

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

    //-----CONSTRUCTORS-----
    public JenaPageIndex()
    {
        Dataset dataset = TDBFactory.createDataset(Settings.instance().getPageTripleStoreFolder().getAbsolutePath());
    }

    //-----PUBLIC METHODS-----
    @Override
    public void indexPage(Page page) throws IOException
    {

    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
