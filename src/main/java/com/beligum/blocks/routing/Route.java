package com.beligum.blocks.routing;

import com.beligum.base.server.R;
import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.database.interfaces.BlocksDatabase;
import com.beligum.blocks.routing.ifaces.WebNode;
import com.beligum.blocks.routing.ifaces.WebPath;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
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


    private BlocksDatabase database;
    // simplePath is path without language e.g. /test
    private Path simplePath;
    // languagedPath is path with language e.g. /en/test
    private Path languagedPath;
    private Locale locale;
    private WebNode rootNode;
    private WebNode finalNode;


    public Route(URI uri, BlocksDatabase database)
    {
        this.database = database;

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

        this.rootNode = database.getRootWebNode(domain.getHost());
        if (this.rootNode == null) {
            this.rootNode = database.createRootWebNode(domain.getHost());
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

        this.finalNode = getNodeFromNodeWithPath(this.rootNode, this.simplePath, this.locale);

    }

    public boolean exists() {
        return this.finalNode != null;
    }

    public void create() {
        if (!this.exists()) {
            this.finalNode = addPathToNode(this.rootNode, this.getPath(), this.locale);
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

    public BlocksDatabase getBlocksDatabase() {
        return this.database;
    }

    private Locale getLanguageFromPath(Path path)
    {
        Locale retVal = null;
        if (path.getNameCount() > 0) {
            String lang = path.getName(0).toString();
            retVal = BlocksConfig.instance().getLanguages().get(lang);
            if (retVal == null) {
                retVal = Locale.ROOT;
            }
        } else {
            retVal= Locale.ROOT;
        }
        return retVal;
    }

    /*
    * returns the end node following a path starting from a source,
    * if no node is found return null
    *
    * @Param srcNode: node to start form
    * @Param path: path without a language
    * @Param locale: locale to search in
    * */
    public WebNode getNodeFromNodeWithPath(WebNode srcNode, Path path, Locale locale)
    {
        if (locale.equals(Locale.ROOT)) {
            locale = BlocksConfig.instance().getDefaultLanguage();
        }

        WebNode currentNode = srcNode;
        /*
        * Starting from the root node, find each sub path. First find 1 for the currnet language
        * If subpath is not found find a path with this name for the default language and without a path for the current language
        * */
        for (int i =0; i < path.getNameCount(); i++) {
            if (currentNode != null) {
                WebPath subPath = currentNode.getChildPath(path.getName(i).toString(), locale);
                if (subPath == null && !locale.equals(BlocksConfig.instance().getDefaultLanguage())) {
                    subPath = currentNode.getChildPath(path.getName(i).toString(), BlocksConfig.instance().getDefaultLanguage());

                }
                if (subPath != null) {
                    currentNode = subPath.getChildWebNode();
                } else {
                    currentNode = null;
                }
            } else {
                break;
            }
        }
        return currentNode;

    }

    /*
    * Create a path in the database starting from the given node
    *
    * @param srcNode    the node to start from
    * @param path       the path to create
    * @param locale     the language of the path
    * @return           returns the last node of the path
    * */
    public WebNode addPathToNode(WebNode srcNode, Path path, Locale locale)
    {
        if (locale.equals(Locale.ROOT)) {
            locale = BlocksConfig.instance().getDefaultLanguage();
        }

        WebNode retVal = srcNode;
        /*
        * Starting from the root node, find each sub path. First find 1 for the current language
        * If subpath is not found find a path with this name for the default language and without a path for the current language
        * */
        for (int i =0; i < path.getNameCount(); i++) {
            if (retVal != null) {
                WebNode prev = retVal;
                retVal = getNodeFromNodeWithPath(retVal, path.getName(i), locale);
                if (retVal == null) {
                    retVal = database.createNode(prev, path.getName(i).toString(), locale);
                }
            } else {
                break;
            }
        }
        return retVal;
    }



}
