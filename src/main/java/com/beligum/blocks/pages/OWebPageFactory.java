package com.beligum.blocks.pages;

import com.beligum.base.templating.ifaces.Template;
import com.beligum.blocks.controllers.OrientResourceController;
import com.beligum.blocks.pages.ifaces.WebPage;
import com.beligum.blocks.pages.ifaces.WebPageFactory;
import com.beligum.blocks.utils.UrlTools;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import gen.com.beligum.blocks.core.fs.html.views.admin.main;

import java.util.Locale;

/**
 * Created by wouter on 28/05/15.
 */
public class OWebPageFactory implements WebPageFactory
{
    @Override
    public WebPage createPage(String html, Locale locale)
    {
        String id = UrlTools.createLocalResourceId(OWebPage.CLASS_NAME);
        return createPage(id, html, locale);
    }
    @Override
    public WebPage createPage(String id, String html, Locale locale)
    {
        OrientGraph graph = OrientResourceController.instance().getGraph();
        Vertex v = graph.addVertex("class:" + OWebPage.CLASS_NAME);
        v.setProperty(OWebPage.ID, id);
        v.setProperty(OWebPage.LANGUAGE, locale.getLanguage());
        WebPage webPage = new OWebPage(v);
        webPage.setHtml(html);
        return webPage;
    }


}
