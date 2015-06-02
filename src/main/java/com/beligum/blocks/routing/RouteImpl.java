package com.beligum.blocks.routing;

import com.beligum.base.server.R;
import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.routing.ifaces.Route;
import com.beligum.blocks.routing.ifaces.nodes.RouteNodeFactory;
import com.beligum.blocks.routing.ifaces.nodes.WebNode;
import com.beligum.blocks.routing.nodes.ORouteNodeFactory;

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
public class RouteImpl implements Route
{
    private URI uri;

    // simplePath without language
    private RouteNodeFactory routeNodeFactory;
    private Path simplePath;
    private Path languagedPath;
    private Locale locale;
    private WebNode rootNode;
    private WebNode finalNode;


    public RouteImpl(URI uri, RouteNodeFactory factory) throws URISyntaxException
    {

        if (uri == null) {
            uri = BlocksConfig.instance().getSiteDomain();
        }
        this.uri = uri;

        java.nio.file.Path currentPath = Paths.get(uri.getPath());
        RouteNodeFactory routing = ORouteNodeFactory.instance();

        // If not in production set the domain manually (overwrite localhost)
        URI domain = uri;
        if (!R.configuration().getProduction()) {
            domain = BlocksConfig.instance().getSiteDomain();
        }

        this.rootNode = routing.getRootNode(domain);
        if (this.rootNode == null) {
            this.rootNode = routing.createRootNode(domain);
        }

        this.locale = getLanguageFromPath(currentPath);
        if (locale.equals(Locale.ROOT)) {
            this.simplePath = currentPath;
            this.languagedPath = Paths.get("/").resolve(BlocksConfig.instance().getDefaultLanguage().getLanguage()).resolve(this.simplePath).normalize();
            this.uri = new URI(this.uri.getScheme(), this.uri.getUserInfo(), this.uri.getHost(), this.uri.getPort(), this.languagedPath.toString(), this.uri.getQuery(), this.uri.getFragment());

        } else {
            this.languagedPath = currentPath;
            this.simplePath = currentPath.subpath(1, currentPath.getNameCount());
        }

        this.finalNode = routeNodeFactory.getNodeFromNodeWithPath(this.rootNode, this.simplePath, this.locale);

    }

    @Override
    public boolean exists() {
        return this.finalNode != null;
    }

    @Override
    public void create() {
        if (!this.exists()) {
            this.finalNode = routeNodeFactory.addPathToNode(this.rootNode, this.getPath(), this.locale);
        }
    }

    // Path without language
    @Override
    public Path getPath() {
        return this.simplePath;
    }

    // Path with language
    @Override
    public Path getLanguagedPath() {
        return this.languagedPath;
    }

    @Override
    public Locale getLocale() {
        return this.locale;
    }

    @Override
    public URI getURI() {
        return this.uri;
    }

    @Override
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
        }
        return retVal;
    }


}
