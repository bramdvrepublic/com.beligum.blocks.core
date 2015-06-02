package com.beligum.blocks.pages;

import com.beligum.base.templating.ifaces.Template;
import com.beligum.blocks.controllers.OrientResourceController;
import com.beligum.blocks.pages.ifaces.WebPage;
import com.beligum.blocks.pages.ifaces.WebPageController;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import gen.com.beligum.blocks.core.fs.html.views.admin.main;

import java.util.Locale;

/**
 * Created by wouter on 1/06/15.
 *
 * Contains all the logic to show, save or delete a page.
 *
 */
public class DefaultWebPageController implements WebPageController
{
    @Override
    public String show(WebPage webPage)
    {
        return null;
    }


    @Override
    public WebPage save(WebPage webPage)
    {
        OrientGraph graph = OrientResourceController.instance().getGraph();
        // TODO commit
        return webPage;
    }


    @Override
    public WebPage delete(WebPage webPage)
    {
        OrientGraph graph = OrientResourceController.instance().getGraph();
        // TODO commit
        return webPage;
    }


}
