package com.beligum.blocks.models.interfaces;

import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;

/**
 * Created by wouter on 28/05/15.
 */
public interface WebPage extends Resource
{
    public URI getBlockId();

    public URI getMasterpageId();

    public String getPageTitle();
    public void setPageTitle(String title);

    public String getParsedHtml();
    public void setParsedHtml(String parsedHtml);

    public void setPageTemplate(String template);
    public String getPageTemplate();

    public String getText();
    public void setText(String text);

    public Set<String> getTemplates();
    public void setTemplates(Set<String> templates);
    public void addTemplate(String template);

    public Set<String> getResources();
    public void setResources(Set<String> resources);
    public void addResource(String resource);

    public Set<HashMap<String, String>> getLinks();
    public void setLinks(Set<HashMap<String, String>> links);
    public void addLink(HashMap<String, String> link);

    public Locale getLanguage();
    public void setLanguage(Locale locale);


}
