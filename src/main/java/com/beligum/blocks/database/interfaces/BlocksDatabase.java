package com.beligum.blocks.database.interfaces;

import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.pages.OWebPage;
import com.beligum.blocks.pages.ifaces.MasterWebPage;
import com.beligum.blocks.resources.interfaces.Node;
import com.beligum.blocks.resources.interfaces.Resource;
import com.beligum.blocks.pages.ifaces.WebPage;
import com.beligum.blocks.routing.ifaces.WebNode;
import com.beligum.blocks.utils.RdfTools;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;

import java.net.URI;
import java.util.Locale;

/**
 * Created by wouter on 5/06/15.
 */
public interface BlocksDatabase
{

//    public WebPage createWebPage(Locale locale);

    public MasterWebPage createMasterWebPage(URI id);

    public WebPage createLocalizedPage(MasterWebPage masterWebPage, URI id, Locale locale);

    public MasterWebPage getMasterWebPage(URI id);

    public WebPage getLocalizedWebPage(URI id, Locale locale);

    public WebPage deleteWebPage(URI id, Locale locale);

    public WebPage saveWebPage(WebPage webPage, boolean doVersion);

    public WebNode createRootWebNode(String host);

    public WebNode getRootWebNode(String host);

    public WebNode createWebNode(WebNode from, String path, Locale locale);

    public Resource createResource(URI id, URI rdfType, Locale language);

    public Resource getResource(URI id, Locale language);

    public Resource saveResource(Resource resource);

    public Resource deleteResource(Resource resource);

    public Node createNode(Object value, Locale language);

}
