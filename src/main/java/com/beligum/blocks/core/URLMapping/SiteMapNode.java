package com.beligum.blocks.core.URLMapping;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bas on 04.03.15.
 */
public class SiteMapNode{
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
