package com.beligum.blocks.routing;

import com.beligum.base.server.R;
import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.routing.ifaces.nodes.RouteController;
import com.beligum.blocks.routing.ifaces.nodes.WebNode;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Created by wouter on 1/06/15.
 *
 * Defines an internal Blocks route based on a uri
 * this is: the language, the rootNode, the final node (node where the route ends that contains the info for the router )
 *
 */
public class Route
{
    private URI uri;


    private RouteController routeController;
    // simplePath is path without language e.g. /test
    private Path simplePath;
    // languagedPath is path with language e.g. /en/test
    private Path languagedPath;
    private Locale locale;
    private WebNode rootNode;
    private WebNode finalNode;


    public Route(URI uri, RouteController controller)
    {
        this.routeController = controller;

        if (uri == null) {
            uri = BlocksConfig.instance().getSiteDomain();
        }
        this.uri = uri;

        java.nio.file.Path currentPath = Paths.get(uri.getPath());

        // If not in production set the domain manually (overwrite localhost)
        URI domain = uri;
        if (!R.configuration().getProduction()) {
            domain = BlocksConfig.instance().getSiteDomain();
        }

        this.rootNode = routeController.getRootNode(domain);
        if (this.rootNode == null) {
            this.rootNode = routeController.createRootNode(domain);
        }

        this.locale = getLanguageFromPath(currentPath);
        if (locale.equals(Locale.ROOT)) {
            this.simplePath = currentPath;
            this.languagedPath = Paths.get("/").resolve(BlocksConfig.instance().getDefaultLanguage().getLanguage()).resolve(this.simplePath).normalize();
            this.uri = UriBuilder.fromUri("").scheme(this.uri.getScheme()).userInfo(this.uri.getUserInfo()).host(this.uri.getHost()).port(this.uri.getPort()).path(this.languagedPath.toString()).replaceQuery(this.uri.getQuery()).fragment(this.uri.getFragment()).build();
        } else {
            this.languagedPath = currentPath;
            this.simplePath = currentPath.subpath(1, currentPath.getNameCount());
        }

        this.finalNode = routeController.getNodeFromNodeWithPath(this.rootNode, this.simplePath, this.locale);

    }

    public boolean exists() {
        return this.finalNode != null;
    }

    public void create() {
        if (!this.exists()) {
            this.finalNode = routeController.addPathToNode(this.rootNode, this.getPath(), this.locale);
        }
    }

    // Path without language
    public Path getPath() {
        return this.simplePath;
    }

    // Path with language
    public Path getLanguagedPath() {
        return this.languagedPath;
    }

    public Locale getLocale() {
        return this.locale;
    }


    public URI getURI() {
        return this.uri;
    }

    public WebNode getNode() {
        return this.finalNode;
    }

    private Locale getLanguageFromPath(Path path)
    {
        Locale retVal = null;
        if (path.getNameCount() > 0) {
            String lang = path.getName(0).toString();
            retVal = BlocksConfig.instance().getLanguages().get(lang);
            if (retVal == null) {
                try {
                    retVal = new Locale(lang);
                } catch (Exception e) {
                    retVal = Locale.ROOT;
                }
            }
        } else {
            retVal= Locale.ROOT;
        }
        return retVal;
    }


}
