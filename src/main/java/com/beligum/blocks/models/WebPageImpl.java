package com.beligum.blocks.models;

import com.beligum.blocks.config.Settings;
import com.beligum.blocks.models.interfaces.WebPage;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Created by wouter on 28/05/15.
 */
public class WebPageImpl extends ResourceImpl implements WebPage
{

    private Map<Locale, String> parsedHtml;
    private Map<Locale, String> pageTemplate = new HashMap<>();
    private Map<Locale, String> text = new HashMap<>();
    private Map<Locale, String> pageTitle = new HashMap<>();
    private Map<Locale, Set<String>> templates = new HashMap<>();
    private Map<Locale, Set<String>> resources = new HashMap<>();
    private Map<Locale, Set<Map<String, String>>> links = new HashMap<>();
    private Map<Locale, LocalDateTime> updatedAt = new HashMap<>();
    private Map<Locale, String> updatedBy = new HashMap<>();
    private Map<Locale, LocalDateTime> createdAt = new HashMap<>();
    private Map<Locale, String> createdBy = new HashMap<>();
    private Set<Locale> languages = new HashSet<Locale>();

    public WebPageImpl()
    {

    }

    public WebPageImpl(URI id, Locale locale)
    {
        super(locale);
        this.setBlockId(id);
        this.language = locale;
        this.getLanguages().add(locale);
    }

    @Override
    public String getParsedHtml(boolean fallback)
    {
        return getParsedHtml(this.getLanguage(), fallback);
    }

    @Override
    public String getParsedHtml(Locale locale, boolean fallback)
    {
        this.getLanguages().add(locale);
        this.parsedHtml = ensure(this.parsedHtml);
        return this.getDefaultValue(this.parsedHtml, locale);
    }

    @Override
    public void setParsedHtml(String html)
    {
        setParsedHtml(html, this.getLanguage());
    }
    @Override
    public void setParsedHtml(String parsedHtml, Locale locale)
    {
        this.getLanguages().add(locale);
        this.parsedHtml = ensure(this.parsedHtml);
        this.parsedHtml.put(locale, parsedHtml);
    }

    @Override
    public String getPageTemplate()
    {
        return getPageTemplate(this.getLanguage());
    }

    @Override
    public String getPageTemplate(Locale locale)
    {
        this.getLanguages().add(locale);
        this.pageTemplate = ensure(this.pageTemplate);
        return this.pageTemplate.get(locale);
    }

    @Override
    public void setPageTemplate(String template)
    {
        setPageTemplate(template, this.getLanguage());
    }

    @Override
    public void setPageTemplate(String template, Locale locale)
    {
        this.getLanguages().add(locale);
        this.pageTemplate = ensure(this.pageTemplate);
        this.pageTemplate.put(locale, template);
    }

    @Override
    public String getPageTitle(boolean fallback)
    {
        return getPageTitle(this.getLanguage(), fallback);
    }
    @Override
    public String getPageTitle(Locale locale, boolean fallback)
    {
        String retVal = null;
        this.getLanguages().add(locale);
        this.pageTitle = ensure(this.pageTitle);
        if (fallback) {
            retVal = this.getDefaultValue(this.pageTitle, locale);
        }
        else {
            retVal = this.pageTitle.get(locale);
        }
        return retVal;
    }

    @Override
    public void setPageTitle(String pageTitle)
    {
        this.setPageTitle(pageTitle, this.getLanguage());
    }
    @Override
    public void setPageTitle(String title, Locale locale)
    {
        this.getLanguages().add(locale);
        this.pageTitle = ensure(this.pageTitle);
        this.pageTitle.put(locale, title);
    }

    @Override
    public String getText()
    {
        return getText(this.getLanguage());
    }

    @Override
    public void setText(String html)
    {
        setText(html, this.getLanguage());
    }
    @Override
    public String getText(Locale locale)
    {
        this.text = ensure(this.text);
        return this.text.get(locale);
    }
    @Override
    public void setText(String text, Locale locale)
    {
        this.getLanguages().add(locale);
        this.text = ensure(this.text);
        this.text.put(locale, text);
    }

    @Override
    public Set<String> getTemplates()
    {
        return getTemplates(this.getLanguage());
    }

    @Override
    public void setTemplates(Set<String> templates)
    {
        setTemplates(templates, this.getLanguage());
    }

    @Override
    public Set<String> getTemplates(Locale locale)
    {
        Set<String> retVal = new HashSet();
        if (this.templates != null) {
            if (this.templates.containsKey(locale)) {
                retVal = this.templates.get(locale);
            }
        }
        this.getLanguages().add(locale);
        return retVal;
    }
    @Override
    public void setTemplates(Set<String> templates, Locale locale)
    {
        if (this.templates == null) {
            this.templates = new HashMap<Locale, Set<String>>();
        }
        this.getLanguages().add(locale);
        this.templates.put(locale, templates);
    }

    @Override
    public Set<String> getResources()
    {
        return getResources(this.getLanguage());
    }

    @Override
    public void setResources(Set<String> resources)
    {
        setResources(resources, this.getLanguage());
    }

    @Override
    public Set<String> getResources(Locale locale)
    {
        Set<String> retVal = new HashSet();
        if (this.templates != null) {
            if (this.templates.containsKey(locale)) {
                retVal = this.templates.get(locale);
            }
        }
        this.getLanguages().add(locale);
        return retVal;
    }
    @Override
    public void setResources(Set<String> resources, Locale locale)
    {
        if (this.resources == null) {
            this.resources = new HashMap<Locale, Set<String>>();
        }
        this.getLanguages().add(locale);
        this.resources.put(locale, resources);
    }

    @Override
    public Set<Map<String, String>> getLinks()
    {
        return getLinks(this.getLanguage());
    }

    @Override
    public void setLinks(Set<Map<String, String>> links)
    {
        setLinks(links, this.getLanguage());
    }

    @Override
    public Set<Map<String, String>> getLinks(Locale locale)
    {
        Set<Map<String, String>> retVal = new HashSet<Map<String, String>>();
        if (this.links == null) {
            this.links = new HashMap<Locale, Set<Map<String, String>>>();
        }
        if (this.links.get(locale) != null) {
            retVal = this.links.get(locale);
        }
        this.getLanguages().add(locale);
        return retVal;
    }

    @Override
    public void setLinks(Set<Map<String, String>> links, Locale locale)
    {
        if (this.links == null) {
            this.links = new HashMap<Locale, Set<Map<String, String>>>();
        }
        this.getLanguages().add(locale);
        this.links.put(locale, links);
    }

    @Override
    public void setCreatedAt(LocalDateTime date)
    {
        setCreatedAt(date, this.getLanguage());
    }

    @Override
    public void setCreatedAt(LocalDateTime date, Locale locale)
    {
        if (this.createdAt == null) {
            this.createdAt = new HashMap<Locale, LocalDateTime>();
        }
        this.getLanguages().add(locale);
        this.createdAt.put(locale, date);
    }

    @Override
    public LocalDateTime getCreatedAt(Locale locale)
    {
        if (this.createdAt == null) {
            this.createdAt = new HashMap<Locale, LocalDateTime>();
        }
        return this.createdAt.get(locale);

    }

    @Override
    public void setCreatedBy(String user, Locale locale)
    {
        if (this.createdBy == null) {
            this.createdBy = new HashMap<Locale, String>();
        }
        this.getLanguages().add(locale);
        this.createdBy.put(locale, user);
    }

    @Override
    public String getCreatedBy(Locale locale)
    {
        if (this.createdBy == null) {
            this.createdBy = new HashMap<Locale, String>();
        }
        return this.createdBy.get(locale);
    }

    @Override
    public void setUpdatedAt(LocalDateTime date, Locale locale)
    {
        if (this.updatedAt == null) {
            this.updatedAt = new HashMap<Locale, LocalDateTime>();
        }
        this.getLanguages().add(locale);
        this.updatedAt.put(locale, date);
    }

    @Override
    public LocalDateTime getUpdatedAt(Locale locale)
    {
        if (this.updatedAt == null) {
            this.updatedAt = new HashMap<Locale, LocalDateTime>();
        }
        return this.updatedAt.get(locale);

    }

    @Override
    public void setUpdatedBy(String user, Locale locale)
    {
        if (this.updatedBy == null) {
            this.updatedBy = new HashMap<Locale, String>();
        }
        this.getLanguages().add(locale);
        this.updatedBy.put(locale, user);
    }

    @Override
    public String getUpdatedBy(Locale locale)
    {
        if (this.updatedBy == null) {
            this.updatedBy = new HashMap<Locale, String>();
        }
        return this.updatedBy.get(locale);
    }

    @Override
    public Set<Locale> getLanguages()
    {
        if (this.languages == null) {
            this.languages = new HashSet<>();
        }
        if (this.languages.contains(Locale.ROOT)) {
            this.languages.remove(Locale.ROOT);
        }
        return this.languages;
    }

    // ------- PROTECTED METHODS ----------

    protected Map<Locale, String> ensure(Map<Locale, String> value)
    {
        Map<Locale, String> retVal = value;
        if (retVal == null) {
            retVal = new HashMap<>();
        }
        return retVal;
    }

    /*
    * If no value is found for the give locale we fall back:
    *  - to a not localized value
    *  - to the default language
    *
    *  if force == true we return any value if a value is available and the fallback did not work
    * */
    protected String getDefaultValue(Map<Locale, String> value, Locale locale)
    {
        String retVal = value.get(locale);
        if (retVal == null) {
            if (value.containsKey(Locale.ROOT)) {
                retVal = value.get(Locale.ROOT);
            }
            else if (value.containsKey(Settings.instance().getDefaultLanguage())) {
                retVal = value.get(Settings.instance().getDefaultLanguage());
            }
            else if (value.containsKey(this.getLanguage())) {
                retVal = value.get(this.getLanguage());
            }
            else if (value.size() > 0) {
                retVal = value.values().iterator().next();
            }
        }

        return retVal;
    }

}
