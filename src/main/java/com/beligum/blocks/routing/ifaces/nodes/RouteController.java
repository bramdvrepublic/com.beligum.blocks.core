package com.beligum.blocks.routing.ifaces.nodes;

import java.net.URI;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Created by wouter on 28/05/15.
 */
public interface RouteController
{
    public WebNode getRootNode(URI uri);

    public WebNode createRootNode(URI uri);

    public WebNode getNodeFromNodeWithPath(WebNode srcNode, Path path, Locale locale);

    public WebNode addPathToNode(WebNode srcNode, Path path, Locale locale);


}
