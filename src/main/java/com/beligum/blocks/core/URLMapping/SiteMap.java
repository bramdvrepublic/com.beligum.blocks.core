package com.beligum.blocks.core.URLMapping;

import com.beligum.blocks.core.config.BlocksConfig;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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


    private class SiteMapNode{
        List<URL> translations = new ArrayList<>();
        List<SiteMapNode> children = new ArrayList<>();

        public SiteMapNode(URL url){
            this.translations.add(url);
        }

        public List<URL> getTranslations()
        {
            return translations;
        }
        public List<SiteMapNode> getChildren()
        {
            return children;
        }
        public void addChild(SiteMapNode child){
            this.children.add(child);
        }
    }
}
