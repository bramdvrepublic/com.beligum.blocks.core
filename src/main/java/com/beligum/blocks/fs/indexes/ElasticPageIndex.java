package com.beligum.blocks.fs.indexes;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.fs.indexes.ifaces.PageIndex;
import com.beligum.blocks.fs.pages.ifaces.Page;

import java.io.IOException;

/**
 * Created by bram on 1/26/16.
 *
 * Some interesting reads:
 *
 *   https://groups.google.com/forum/#!msg/elasticsearch/hPu1e7TrL40/K8ORXOoWWe4J
 *
 *
 */
public class ElasticPageIndex implements PageIndex
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    public void indexPage(Page page) throws IOException
    {
        Logger.info(page.getJsonLDNode().toString());
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
