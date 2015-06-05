package com.beligum.blocks.pages.ifaces;

import com.beligum.blocks.resources.interfaces.Resource;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

/**
 * Created by wouter on 28/05/15.
 */
public interface WebPage extends Resource
{
    public URI getBlockId();

    public String getHtml();
    public void setHtml(String title);

    public Set<String> getResources();
    public void addResource(String resource);

    public Set<String> getLinks();
    public void addLink(String link);

    public Locale getLanguage();

}
