package com.beligum.blocks.models.interfaces;

import org.joda.time.LocalDateTime;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Created by wouter on 28/05/15.
 */
public interface WebPage extends Resource
{

    public String getPageTitle(boolean fallback);

    public String getPageTitle(Locale locale, boolean fallback);

    public void setPageTitle(String title);

    public void setPageTitle(String title, Locale locale);

    public String getParsedHtml(boolean fallback);

    public String getParsedHtml(Locale locale, boolean fallback);

    public void setParsedHtml(String parsedHtml);

    public void setParsedHtml(String parsedHtml, Locale locale);

    public String getPageTemplate();

    public void setPageTemplate(String template);

    public String getPageTemplate(Locale locale);

    public void setPageTemplate(String template, Locale locale);

    public String getText();

    public void setText(String text);

    public String getText(Locale locale);

    public void setText(String text, Locale locale);

    public Set<String> getTemplates();

    public void setTemplates(Set<String> templates);

    public Set<String> getTemplates(Locale locale);

    public void setTemplates(Set<String> templates, Locale locale);

    public Set<String> getResources();

    public void setResources(Set<String> resources);

    public Set<String> getResources(Locale locale);

    public void setResources(Set<String> resources, Locale locale);

    public Set<Map<String, String>> getLinks();

    public void setLinks(Set<Map<String, String>> links);

    public Set<Map<String, String>> getLinks(Locale locale);

    public void setLinks(Set<Map<String, String>> links, Locale locale);

    public String getCreatedBy(Locale locale);

    public LocalDateTime getCreatedAt(Locale locale);

    public String getUpdatedBy(Locale locale);

    public LocalDateTime getUpdatedAt(Locale locale);

    public void setCreatedBy(String user, Locale locale);

    public void setCreatedAt(LocalDateTime date, Locale locale);

    public void setUpdatedBy(String user, Locale locale);

    public void setUpdatedAt(LocalDateTime date, Locale locale);

    public Set<Locale> getLanguages();

}
