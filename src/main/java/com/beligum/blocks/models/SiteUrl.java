package com.beligum.blocks.models;


import com.beligum.base.models.BasicModelImpl;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Created by wouter on 18/04/15.
 */

@Entity
@Table(name="url")
public class SiteUrl extends BasicModelImpl
{
    public static final String DEFAULT = "DEFAULT";

    private String url;
    private String verb;
    private String viewUrl;
    private String resourceUrl;
    private String language;

    public SiteUrl() {

    }

    public SiteUrl(String url, String viewUrl, String resourceUrl, String language) {
        this.url = url;
        this.viewUrl = viewUrl;
        this.resourceUrl = resourceUrl;
        this.language = language;
        this.verb = SiteUrl.DEFAULT;
    }

    public String getUrl()
    {
        return url;
    }
    public void setUrl(String url)
    {
        this.url = url;
    }
    public String getVerb()
    {
        return verb;
    }
    public void setVerb(String verb)
    {
        this.verb = verb;
    }
    public String getLanguage()
    {
        return language;
    }
    public void setLanguage(String language)
    {
        this.language = language;
    }

    public String getViewUrl()
    {
        return viewUrl;
    }
    public void setViewUrl(String viewUrl)
    {
        this.viewUrl = viewUrl;
    }
    public String getResourceUrl()
    {
        return resourceUrl;
    }
    public void setResourceUrl(String resourceUrl)
    {
        this.resourceUrl = resourceUrl;
    }
}
