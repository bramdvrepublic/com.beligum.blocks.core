package com.beligum.blocks.routing;

import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.controllers.OrientResourceController;
import com.beligum.blocks.routing.ifaces.nodes.RouteController;
import com.beligum.blocks.routing.ifaces.nodes.WebNode;
import com.beligum.blocks.routing.ifaces.nodes.WebPath;
import com.beligum.blocks.routing.nodes.OWebNode;
import com.beligum.blocks.routing.nodes.OWebPath;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;

import java.net.URI;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Created by wouter on 28/05/15.
 */
public class ORouteController implements RouteController
{

    // The name of the path. This field should be prepended with the language code e.g. nl_name, fr_name
    // this is the name of the property in the database
    public final static String NAME_FIELD = "name";

    // the verb property name in the OrientDB
    public final static String STATUS_FIELD = "statuscode";

    // The object property in the OrientDB
    public final static String PAGE_FIELD = "page";

    public final static String ROOT_HOST_NAME = "hostname";

    // The classname of a node in the OrientDB
    public final static String NODE_CLASS = "WebNode";

    // The classname of a root node in the OrientDB
    public final static String ROOT_NODE_CLASS = "WebRootNode";

    // the classname for the path in the Orient DB
    public final static String PATH_CLASS_NAME = "WebPath";

    public static ORouteController instance;

    private ORouteController() {

    }

    public static ORouteController instance() {
        if (ORouteController.instance == null) {
            ORouteController.instance = new ORouteController();
        }
        return ORouteController.instance;
    }

    @Override
    public WebNode getRootNode(URI uri)
    {
        WebNode retVal= null;

        Vertex vertex = null;
        String host = uri.getHost();
        OrientGraph graph = OrientResourceController.instance().getGraph();
        Iterable<Vertex> vertices = graph.command(new OSQLSynchQuery("select from " + ROOT_NODE_CLASS + " WHERE " + ROOT_HOST_NAME + " = '" + host + "' fetchplan *:0")).execute();
        for (Vertex v: vertices) {
            vertex = v;
            break;
        }
        if (vertex != null) {
            retVal = new OWebNode(vertex);
        }
        return retVal;
    }

    @Override
    public WebNode createRootNode(URI uri)
    {
        WebNode retVal;

        String host = uri.getHost();
        Vertex vertex = null;
        OrientGraph graph = OrientResourceController.instance().getGraph();

        Iterable<Vertex> vertices = graph.command(new OSQLSynchQuery("select from " + ROOT_NODE_CLASS + " WHERE " + ROOT_HOST_NAME + " = '" + host + "' fetchplan *:0")).execute();

        for (Vertex v: vertices) {
            vertex = v;
            break;
        }
        if (vertex == null) {
            vertex = graph.addVertex("class:" + ROOT_NODE_CLASS);
            vertex.setProperty(ROOT_HOST_NAME, uri.getHost());
            retVal = new OWebNode(vertex);
        } else {
            retVal = new OWebNode(vertex);
        }
        retVal.setStatusCode(404);
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

    @Override
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
                    if (subPath.getName(locale) != null) {
                        subPath = null;
                    }
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
    @Override
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
                retVal = getNodeFromNodeWithPath(retVal, path.getName(i), locale);
                if (retVal == null) {
                    retVal = createNode(retVal, path.getName(i).toString(), locale);
                }
            } else {
               break;
            }
        }
        return retVal;
    }

    /*
    * Create a new node in the database with a single path, starting from a node
    * @param from   the node to start from
    * @param path   a string that defines a single connection (path) between two nodes
    * @param locale the locale from the path
    * */
    public WebNode createNode(WebNode from, String path, Locale locale) {
        OrientGraph graph = OrientResourceController.instance().getGraph();
        Vertex v = graph.addVertex("class:" + NODE_CLASS);
        OWebNode retVal = new OWebNode(v);
        retVal.setStatusCode(404);
        Edge e = graph.addEdge(null, ((OWebNode)from).getVertex(), v, ORouteController.PATH_CLASS_NAME);
        WebPath webPath = new OWebPath(e);
        webPath.setName(path, locale);
        return retVal;
    }


    public static String getLocalizedNameField(Locale locale) {
        String retVal = NAME_FIELD;
        if (!locale.equals(Locale.ROOT)) {
            retVal = locale.getLanguage() + "_" + NAME_FIELD;
        }
        return retVal;
    }

}
