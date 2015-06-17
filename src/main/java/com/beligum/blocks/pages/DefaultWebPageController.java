package com.beligum.blocks.pages;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.database.interfaces.BlocksDatabase;
import com.beligum.blocks.pages.ifaces.WebPage;
import com.beligum.blocks.routing.RouteController;
import com.beligum.blocks.routing.Route;
import com.beligum.blocks.routing.ifaces.WebNode;

import java.net.URI;

/**
 * Created by wouter on 1/06/15.
 *
 * Contains all the logic to show, save or delete a page.
 *
 */
public class DefaultWebPageController
{


    public WebPage save(URI uri, String html, BlocksDatabase database) throws Exception
    {
        RouteController routeController = RouteController.instance();
        Route route = new Route(uri, database);
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
            webPage = database.createWebPage(route.getLocale());
            webnode.setPageOk(webPage.getBlockId());
        } else if (webnode.isPage()) {
            webPage = database.getWebPage(webnode.getPageUrl(), route.getLocale());
        }

        WebPageParser webPageParser = null;
        try {
            webPageParser = new WebPageParser(webPage, uri, html, database);
            webPage = webPageParser.getWebPage();
        }
        catch (Exception e) {
            Logger.error(e);
            throw new Exception("An error was thrown during parsing of the html", e);
        }



        // TODO commit
        return null;
    }


}
