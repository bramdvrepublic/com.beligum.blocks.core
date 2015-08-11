package com.beligum.blocks.models;

import com.beligum.blocks.models.interfaces.WebPage;
import com.beligum.blocks.models.jackson.page.PageDeserializer;
import com.beligum.blocks.models.jackson.page.PageSerializer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.joda.time.LocalDateTime;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.*;

/**
* Created by wouter on 28/05/15.
*/
public class WebPageImpl extends ResourceImpl implements WebPage
{

    public static final ObjectMapper pageMapper = new ObjectMapper().registerModule(new SimpleModule().addSerializer(WebPage.class, new PageSerializer<>()).addDeserializer(WebPage.class, new PageDeserializer()));

    private String masterPage;
    private String parsedHtml;
    private String pageTemplate;
    private String text;
    private String pageTitle;
    private Set<String> templates;
    private Set<String> resources;
    Set<HashMap<String, String>> links;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private LocalDateTime createdAt;
    private String createdBy;
    private Locale locale;



    public WebPageImpl(URI masterPage, URI id, Locale locale) {
        super(new HashMap<String, Object>(), new HashMap<String, Object>(), locale);
        this.setBlockId(id);
        this.masterPage = masterPage.toString();
        this.locale = locale;
    }


    @Override
    public String getParsedHtml()
    {
        return parsedHtml;
    }

    @Override
    public void setParsedHtml(String html)
    {
        parsedHtml = html;
    }

    @Override
    public void setPageTemplate(String template)
    {
        pageTemplate = template;
    }
    @Override
    public String getPageTemplate()
    {
        return pageTemplate;
    }

    @Override
    public String getPageTitle() {
        return this.pageTitle;
    }

    @Override
    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    @Override
    public String getText()
    {
        return text;
    }

    @Override
    public void setText(String html)
    {
        text = html;
    }

    @Override
    public URI getMasterpageId() {
        return UriBuilder.fromUri(this.masterPage).build();
    }

    public void setLanguage(Locale locale)
    {
        this.locale = locale;
    }

    public Locale getLanguage()
    {
        return this.locale;
    }

    @Override
    public Set<String> getTemplates()
    {
        Set<String> retVal = this.templates;
        if (retVal == null) {
            retVal = new HashSet<String>();
        }
        return retVal;
    }

    @Override
    public void setTemplates(Set<String> templates)
    {
        if (templates == null) {
            templates = new HashSet<String>();
        }
        this.templates = templates;
    }

    @Override
    public void addTemplate(String template)
    {
        Set<String> retVal = getTemplates();
        retVal.add(template);
    }

    @Override
    public Set<String> getResources()
    {
        Set<String> retVal = this.resources;
        if (retVal == null) {
            retVal = new HashSet<String>();
        }
        return retVal;
    }

    @Override
    public void setResources(Set<String> resources)
    {
        if (resources == null) {
            resources = new HashSet<String>();
        }
        this.resources = resources;
    }

    @Override
    public void addResource(String resource)
    {
        Set<String> retVal = getResources();
        retVal.add(resource);
    }

    @Override
    public Set<HashMap<String, String>> getLinks()
    {
        Set<HashMap<String, String>> retVal = this.links;
        if (retVal == null) {
            retVal = new HashSet<HashMap<String, String>>();
        }
        return retVal;
    }

    @Override
    public void setLinks(Set<HashMap<String, String>> links)
    {
        this.links = links;
    }

    @Override
    public void addLink(HashMap<String, String> link)
    {
        Set<HashMap<String, String>> retVal = getLinks();
        if (retVal == null) {
            retVal = new HashSet<HashMap<String, String>>();
        }
        retVal.add(link);
    }

    @Override
    public void setCreatedAt(LocalDateTime date)
    {
        this.createdAt = date;
    }

    @Override
    public LocalDateTime getCreatedAt()
    {
        return this.createdAt;

    }

    @Override
    public void setCreatedBy(String user)
    {
        this.createdBy = user;
    }

    @Override
    public String getCreatedBy()
    {
        return this.createdBy;
    }

    @Override
    public void setUpdatedAt(LocalDateTime date)
    {
        this.updatedAt = date;
    }

    @Override
    public LocalDateTime getUpdatedAt()
    {
        return this.updatedAt;

    }

    @Override
    public void setUpdatedBy(String user)
    {
        this.updatedBy = user;
    }

    @Override
    public String getUpdatedBy()
    {
        return this.updatedBy;
    }

    @Override
    public String toJson() throws JsonProcessingException
    {
        return WebPageImpl.pageMapper.writeValueAsString(this);
    }

}
