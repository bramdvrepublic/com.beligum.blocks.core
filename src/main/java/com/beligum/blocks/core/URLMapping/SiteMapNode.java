package com.beligum.blocks.core.URLMapping;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bas on 04.03.15.
 */
public class SiteMapNode{
    private URL url;
    /**true if this site map node points to an actual entity, false otherwise*/
    private boolean hasEntity;
    private List<SiteMapNode> children = new ArrayList<>();

    public SiteMapNode(URL url, boolean hasEntity){
        this.url = url;
        this.hasEntity = hasEntity;
    }
    public URL getUrl()
    {
        return url;
    }
    public boolean isHasEntity()
    {
        return hasEntity;
    }
    public List<SiteMapNode> getChildren()
    {
        return children;
    }
    public void addChild(SiteMapNode child){
        this.children.add(child);
    }
}
