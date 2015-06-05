package com.beligum.blocks.database.interfaces;

import com.beligum.blocks.resources.interfaces.Node;
import com.beligum.blocks.resources.interfaces.Resource;
import com.beligum.blocks.pages.ifaces.WebPage;
import com.beligum.blocks.routing.ifaces.WebNode;

import java.net.URI;
import java.util.Locale;

/**
 * Created by wouter on 5/06/15.
 */
public interface BlocksDatabase
{

    public WebPage createWebPage(Locale locale);

    public WebPage createWebPage(URI id, Locale locale);

    public WebPage getWebPage(URI id, Locale locale);

    public WebPage deleteWebPage(URI id, Locale locale);

    public WebPage save(WebPage webPage);

    public WebNode createRootWebNode(String host);

    public WebNode getRootWebNode(String host);

    public WebNode createNode(WebNode from, String path, Locale locale);

    public Resource createResource(URI id, URI rdfType, Locale language);

    public Resource getResource(URI id, Locale language);

    public Resource saveResource(Resource resource);

    public Resource deleteResource(Resource resource);

    public Node createNode(Object value, Locale language);

}
