package com.beligum.blocks.core.URLMapping;

import java.net.MalformedURLException;

/**
 * Created by bas on 03.03.15.
 */
public class SiteMap
{
    private String language;
    private SiteMapNode root;

    public SiteMap(SiteMapNode root, String language) throws MalformedURLException
    {
        this.root = root;
        this.language = language;
    }
    public String getLanguage()
    {
        return language;
    }
    public SiteMapNode getRoot()
    {
        return root;
    }
}
