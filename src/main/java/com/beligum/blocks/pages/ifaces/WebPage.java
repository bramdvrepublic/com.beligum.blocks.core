package com.beligum.blocks.pages.ifaces;

import java.util.Set;

/**
 * Created by wouter on 28/05/15.
 */
public interface WebPage
{
    public String getTitle();
    public void setTitle(String title);

    public String getHtml();
    public void setHtml(String title);

    public Set<String> getResources();
    public void addResource(String resource);

    public Set<String> getLinks();
    public void addLink(String link);
}
