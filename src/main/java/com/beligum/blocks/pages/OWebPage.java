package com.beligum.blocks.pages;

import com.beligum.blocks.pages.ifaces.WebPage;
import com.tinkerpop.blueprints.Vertex;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by wouter on 28/05/15.
 */
public class OWebPage implements WebPage
{
    public static String LANGUAGE = "@language";
    public static String ID = "@id";
    public static String CLASS_NAME = "WebPage";
    public static String TITLE = "title";
    public static String HTML = "html";
    public static String RESOURCES = "resources";
    public static String LINKS = "links";

    private Vertex vertex;

    public OWebPage(Vertex vertex) {
        this.vertex = vertex;
    }

    @Override
    public String getTitle()
    {
        return (String)vertex.getProperty(TITLE);
    }

    @Override
    public void setTitle(String title)
    {
        vertex.setProperty(TITLE, title);
    }

    @Override
    public String getHtml()
    {
        return (String)vertex.getProperty(HTML);
    }

    @Override
    public void setHtml(String html)
    {
        vertex.setProperty(HTML, html);
    }

    @Override
    public Set<String> getResources()
    {
        Set<String> retVal = vertex.getProperty(RESOURCES);
        if (retVal == null) {
            retVal = new HashSet<String>();
        }
        return retVal;
    }

    @Override
    public void addResource(String resource)
    {
        Set<String> retVal = getResources();
        if (retVal == null) {
            retVal = new HashSet<String>();
        }
        retVal.add(resource);
        this.vertex.setProperty(RESOURCES, retVal);
    }

    @Override
    public Set<String> getLinks()
    {
        Set<String> retVal = vertex.getProperty(LINKS);
        if (retVal == null) {
            retVal = new HashSet<String>();
        }
        return retVal;
    }

    @Override
    public void addLink(String link)
    {
        Set<String> retVal = getLinks();
        if (retVal == null) {
            retVal = new HashSet<String>();
        }
        retVal.add(link);
        this.vertex.setProperty(LINKS, retVal);
    }
}
