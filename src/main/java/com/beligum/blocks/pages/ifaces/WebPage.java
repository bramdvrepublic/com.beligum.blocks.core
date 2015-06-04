package com.beligum.blocks.pages.ifaces;

import com.beligum.blocks.models.resources.interfaces.Node;
import com.beligum.blocks.models.resources.interfaces.Resource;

import java.util.Locale;
import java.util.Set;

/**
 * Created by wouter on 28/05/15.
 */
public interface WebPage extends Resource
{
    public String getBlockId();
    public String getTitle();
    public void setTitle(String title);

    public String getHtml();
    public void setHtml(String title);

    public Set<String> getResources();
    public void addResource(String resource);

    public Set<String> getLinks();
    public void addLink(String link);

    public Locale getLanguage();

}
