package com.beligum.blocks.controllers;

import com.beligum.base.i18n.I18nFactory;
import com.beligum.base.server.RequestContext;
import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.models.interfaces.WebPage;
import com.beligum.blocks.routing.Route;
import com.beligum.blocks.templating.blocks.DefaultTemplateController;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Created by wouter on 11/08/15.
 */
public class BreadcrumbController extends DefaultTemplateController
{
    @Override
    public void created()
    {

    }

    public List<HashMap<String, String>> breadcrumbs() throws IOException
    {
        List<HashMap<String, String>> retVal = new ArrayList<>();
        // get URI
        URI originalUri = RequestContext.getJaxRsRequest().getUriInfo().getRequestUri();
        Route originalRoute = new Route(originalUri, PersistenceControllerImpl.instance());

        for (int i = 0; i <= originalRoute.getPath().getNameCount(); i++) {
            String path = i > 0 ? originalRoute.getPath().subpath(0,i).toString() : "/";
            URI uri = UriBuilder.fromUri(originalUri).replacePath(path).build();
            Route route = new Route(uri, PersistenceControllerImpl.instance());

            // Find another path in the most relevant language
            if (!route.exists()) {
                route.getAlternateLocalPath();
            }

            if (route.exists()) {
                WebPage webPage = PersistenceControllerImpl.instance().getWebPage(route.getWebPath().getMasterPage(), route.getLocale());
                String title = webPage.getPageTitle();
                if (title == null) {
                    title = I18nFactory.instance().getResourceBundle(route.getLocale()).get("blocks.core.defaultPageTitle");
                }
                String url = route.getLanguagedPath().toString();
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("url", url);
                map.put("title", title);
                retVal.add(map);
            }

        }

        return retVal;
    }

}