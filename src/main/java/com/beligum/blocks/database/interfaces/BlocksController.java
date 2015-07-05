package com.beligum.blocks.database.interfaces;

import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.resources.interfaces.Node;
import com.beligum.blocks.resources.interfaces.Resource;
import com.beligum.blocks.pages.ifaces.WebPage;
import com.beligum.blocks.routing.ifaces.WebNode;
import com.beligum.blocks.routing.ifaces.WebPath;
import com.beligum.blocks.utils.RdfTools;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Created by wouter on 5/06/15.
 */
public interface BlocksController
{
    public static final String resource = "resource";
    public static final String webpage = "webpage";
    public static final String path = "path";

    public WebPage createWebPage(URI masterWebPage, URI id, Locale locale);

    public WebPage getWebPage(URI masterWebPage, Locale locale) throws IOException;

    public WebPage getWebPage(URI id);
;
    public void deleteWebPage(URI masterPage) throws Exception;;

    public WebPage saveWebPage(WebPage webPage, boolean doVersion) throws Exception;;


    public Resource createResource(URI id, URI rdfType, Locale language);

    public Resource getResource(URI id, Locale language) throws Exception;

    public Resource saveResource(Resource resource) throws Exception;

    public Resource deleteResource(Resource resource) throws Exception;;

    public Node createNode(Object value, Locale language);

    // returns a webnode (containing all urls and redirects for this page)
    public WebPath getPath(URI masterPage, Locale locale);

    // returns a webnode (containing all urls and redirects for this page)
    public WebPath getPath(Path path, Locale locale);

    public WebPath createPath(URI masterPage, Path path, Locale locale) throws Exception;

    public WebPath savePath(WebPath path) throws Exception;

}
