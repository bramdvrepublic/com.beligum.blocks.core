package com.beligum.blocks.pages;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.controllers.OrientResourceController;
import com.beligum.blocks.pages.ifaces.WebPage;
import com.beligum.blocks.pages.ifaces.WebPageController;
import com.beligum.blocks.routing.ORouteController;
import com.beligum.blocks.routing.Route;
import com.beligum.blocks.routing.ifaces.nodes.RouteController;
import com.beligum.blocks.routing.ifaces.nodes.WebNode;
import com.beligum.blocks.utils.RdfTools;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;

import java.net.URI;
import java.util.Locale;

/**
 * Created by wouter on 1/06/15.
 *
 * Contains all the logic to show, save or delete a page.
 *
 */
public class DefaultWebPageController implements WebPageController
{

    private static DefaultWebPageController instance;

    private DefaultWebPageController() {

    }

    public static DefaultWebPageController instance() {
        if (DefaultWebPageController.instance == null) {
            DefaultWebPageController.instance = new DefaultWebPageController();
        }
        return DefaultWebPageController.instance;
    }

    @Override
    public WebPage createPage(Locale locale)
    {
        URI id = RdfTools.createLocalResourceId(OWebPage.CLASS_NAME);
        return createPage(id, locale);
    }

    @Override
    public WebPage get(String id, Locale language)
    {
        return null;
    }

    @Override
    public WebPage createPage(URI id, Locale locale)
    {
        OrientGraph graph = OrientResourceController.instance().getGraph();
        Vertex v = graph.addVertex("class:" + OWebPage.CLASS_NAME);
        v.setProperty(OWebPage.ID, id);
        v.setProperty(OWebPage.LANGUAGE, locale.getLanguage());
        WebPage webPage = new OWebPage(v, locale);
        return webPage;
    }


    @Override
    public String render(URI uri)
    {
        return null;
    }

    @Override
    public String render(WebPage WebPage)
    {
        return null;
    }


    @Override
    public WebPage save(URI uri, String html) throws Exception
    {
        RouteController routeController = ORouteController.instance();
        Route route = new Route(uri, routeController);
        if (!route.exists()) {
            route.create();
        }
        WebNode webnode = route.getNode();
        WebPage webPage = null;

        // Get or create a webpage coupled to this node
        if (webnode.isRedirect()) {
            // resolve redirect
        }

        if (webnode.isNotFound()) {
            // create a new page
            webPage = createPage(route.getLocale());
            webnode.setPageUrl(webPage.getBlockId());
        } else if (webnode.isPage()) {
            webPage = get(webnode.getPageUrl(), route.getLocale());
        }

        WebPageParser webPageParser = null;
        try {
            webPageParser = new WebPageParser(webPage, uri, html, routeController);
            webPage = webPageParser.getWebPage();
        }
        catch (Exception e) {
            Logger.error(e);
            throw new Exception("An error was thrown during parsing of the html", e);
        }



        // TODO commit
        return null;
    }


    @Override
    public WebPage delete(WebPage webPage)
    {
        OrientGraph graph = OrientResourceController.instance().getGraph();
        // TODO commit
        return webPage;
    }


}
