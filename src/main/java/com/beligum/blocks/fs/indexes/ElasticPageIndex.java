package com.beligum.blocks.fs.indexes;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.fs.indexes.ifaces.PageIndex;
import com.beligum.blocks.fs.pages.ifaces.Page;

/**
 * Created by bram on 1/26/16.
 */
public class ElasticPageIndex implements PageIndex
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    public void indexPage(Page page)
    {
        Logger.info(page.getJsonLDNode().toString());
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
