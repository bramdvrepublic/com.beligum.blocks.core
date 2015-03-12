package com.beligum.blocks.core.URLMapping;

import com.beligum.blocks.core.exceptions.LanguageException;
import com.beligum.blocks.core.internationalization.Languages;

import java.net.MalformedURLException;
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
    public String getAbsolutePath() throws LanguageException, MalformedURLException
    {
        String unlanguagedUrl = Languages.translateUrl(this.url.toString(), Languages.NO_LANGUAGE)[0];
        URL url = new URL(unlanguagedUrl);
        return url.getPath();
    }
    public String getRelativePath(){
        String path = this.url.getPath();
        String[] pathParts = path.split("/");
        if(pathParts.length>0){
            return pathParts[pathParts.length-1];
        }
        else{
            return "";
        }
    }
    public List<SiteMapNode> getChildren()
    {
        return children;
    }
    public void addChild(SiteMapNode child){
        this.children.add(child);
    }
}