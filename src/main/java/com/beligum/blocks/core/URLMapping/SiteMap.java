package com.beligum.blocks.core.URLMapping;

import com.beligum.blocks.core.config.BlocksConfig;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by bas on 03.03.15.
 */
public class SiteMap
{
    private SiteMapNode root;

    public SiteMap() throws MalformedURLException
    {
        this.root = new SiteMapNode(new URL(BlocksConfig.getSiteDomain() + "/" + BlocksConfig.getDefaultLanguage()));
    }

    public SiteMapNode getRoot()
    {
        return root;
    }
}
