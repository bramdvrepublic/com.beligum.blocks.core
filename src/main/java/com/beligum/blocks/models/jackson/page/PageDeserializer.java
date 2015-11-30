package com.beligum.blocks.models.jackson.page;

import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.models.WebPageImpl;
import com.beligum.blocks.models.interfaces.Resource;
import com.beligum.blocks.models.interfaces.WebPage;
import com.beligum.blocks.models.jackson.NodeDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.joda.time.LocalDateTime;

import java.util.*;

/**
 * Created by wouter on 30/06/15.
 */
public class PageDeserializer<T extends WebPage> extends NodeDeserializer
{

    // ---------- PRIVATE METHDOS --------------

    @Override
    protected Resource createNewResource(JsonNode node)
    {
        if (isWebPage(node)) {
            return new WebPageImpl();
        }
        else {
            return super.createNewResource(node);
        }
    }

    @Override
    protected void parseSpecialFields(JsonNode node, Resource resource)
    {
        node = getPageNode(node);

        if (isWebPage(node)) {
            WebPage webPage = (WebPage) resource;

            /*
             * Start filling the fields of the web page. Those fields were not parser by the resource
             * */
            String value = node.get(ParserConstants.PAGE_PROPERTY_HTML).asText();
            webPage.setParsedHtml(value, Locale.ROOT);

            JsonNode o = getLocalValues(node, ParserConstants.PAGE_PROPERTY_HTML);
            if (o != null) {
                Iterator<String> fieldnames = o.fieldNames();
                while (fieldnames.hasNext()) {
                    String lang = fieldnames.next();
                    Locale locale = BlocksConfig.instance().getLocaleForLanguage(lang);
                    if (locale != null) {
                        webPage.setParsedHtml(o.get(lang).asText(), locale);
                    }
                }
            }

            if (node.has(ParserConstants.PAGE_PROPERTY_PAGETEMPLATE)) {
                value = node.get(ParserConstants.PAGE_PROPERTY_PAGETEMPLATE).asText();
                webPage.setPageTemplate(value, Locale.ROOT);
            }

            o = getLocalValues(node, ParserConstants.PAGE_PROPERTY_PAGETEMPLATE);
            if (o != null) {
                Iterator<String> fieldnames = o.fieldNames();
                while (fieldnames.hasNext()) {
                    String lang = fieldnames.next();
                    Locale locale = BlocksConfig.instance().getLocaleForLanguage(lang);
                    if (locale != null) {
                        webPage.setPageTemplate(o.get(lang).asText(), locale);
                    }
                }
            }

            if (node.has(ParserConstants.PAGE_PROPERTY_PAGETITLE)) {
                value = node.get(ParserConstants.PAGE_PROPERTY_PAGETITLE).asText();
                webPage.setPageTitle(value, Locale.ROOT);
            }

            o = getLocalValues(node, ParserConstants.PAGE_PROPERTY_PAGETITLE);
            if (o != null) {
                Iterator<String> fieldnames = o.fieldNames();
                while (fieldnames.hasNext()) {
                    String lang = fieldnames.next();
                    Locale locale = BlocksConfig.instance().getLocaleForLanguage(lang);
                    if (locale != null) {
                        webPage.setPageTitle(o.get(lang).asText(), locale);
                    }
                }
            }

            if (node.has(ParserConstants.PAGE_PROPERTY_TEXT)) {
                value = node.get(ParserConstants.PAGE_PROPERTY_TEXT).asText();
                webPage.setText(value, Locale.ROOT);
            }

            o = getLocalValues(node, ParserConstants.PAGE_PROPERTY_TEXT);
            if (o != null) {
                Iterator<String> fieldnames = o.fieldNames();
                while (fieldnames.hasNext()) {
                    String lang = fieldnames.next();
                    Locale locale = BlocksConfig.instance().getLocaleForLanguage(lang);
                    if (locale != null) {
                        webPage.setText(o.get(lang).asText(), locale);
                    }
                }
            }

            if (node.has(ParserConstants.PAGE_PROPERTY_UPDATED_AT)) {
                LocalDateTime date = new LocalDateTime(node.get(ParserConstants.PAGE_PROPERTY_UPDATED_AT).asLong());
                webPage.setUpdatedAt(date, Locale.ROOT);
            }

            o = getLocalValues(node, ParserConstants.PAGE_PROPERTY_UPDATED_AT);
            if (o != null) {
                Iterator<String> fieldnames = o.fieldNames();
                while (fieldnames.hasNext()) {
                    String lang = fieldnames.next();
                    Locale locale = BlocksConfig.instance().getLocaleForLanguage(lang);
                    if (locale != null) {
                        webPage.setUpdatedAt(new LocalDateTime(o.get(lang).asLong()), locale);
                    }
                }
            }

            if (node.has(ParserConstants.PAGE_PROPERTY_CREATED_AT)) {
                LocalDateTime date = new LocalDateTime(node.get(ParserConstants.PAGE_PROPERTY_CREATED_AT).asLong());
                webPage.setCreatedAt(date, Locale.ROOT);
            }

            o = getLocalValues(node, ParserConstants.PAGE_PROPERTY_CREATED_AT);
            if (o != null) {
                Iterator<String> fieldnames = o.fieldNames();
                while (fieldnames.hasNext()) {
                    String lang = fieldnames.next();
                    Locale locale = BlocksConfig.instance().getLocaleForLanguage(lang);
                    if (locale != null) {
                        webPage.setCreatedAt(new LocalDateTime(o.get(lang).asLong()), locale);
                    }
                }
            }

            if (node.has(ParserConstants.PAGE_PROPERTY_CREATED_BY)) {
                value = node.get(ParserConstants.PAGE_PROPERTY_CREATED_BY).asText();
                webPage.setCreatedBy(value, Locale.ROOT);
            }

            o = getLocalValues(node, ParserConstants.PAGE_PROPERTY_CREATED_BY);
            if (o != null) {
                Iterator<String> fieldnames = o.fieldNames();
                while (fieldnames.hasNext()) {
                    String lang = fieldnames.next();
                    Locale locale = BlocksConfig.instance().getLocaleForLanguage(lang);
                    if (locale != null) {
                        webPage.setCreatedBy(o.get(lang).asText(), locale);
                    }
                }
            }

            if (node.has(ParserConstants.PAGE_PROPERTY_UPDATED_BY)) {
                value = node.get(ParserConstants.PAGE_PROPERTY_UPDATED_BY).asText();
                webPage.setUpdatedBy(value, Locale.ROOT);
            }

            o = getLocalValues(node, ParserConstants.PAGE_PROPERTY_UPDATED_BY);
            if (o != null) {
                Iterator<String> fieldnames = o.fieldNames();
                while (fieldnames.hasNext()) {
                    String lang = fieldnames.next();
                    Locale locale = BlocksConfig.instance().getLocaleForLanguage(lang);
                    if (locale != null) {
                        webPage.setUpdatedBy(o.get(lang).asText(), locale);
                    }
                }
            }

            // Add templates

            Set<String> templates = new HashSet<String>();
            if (node.get(ParserConstants.PAGE_PROPERTY_TEMPLATES).isArray()) {
                Iterator<JsonNode> iterator = node.get(ParserConstants.PAGE_PROPERTY_TEMPLATES).iterator();
                while (iterator.hasNext()) {
                    templates.add(iterator.next().asText());
                }
            }
            webPage.setTemplates(templates, Locale.ROOT);

            if (getLocalValues(node, ParserConstants.PAGE_PROPERTY_TEMPLATES) != null && getLocalValues(node, ParserConstants.PAGE_PROPERTY_TEMPLATES).isObject()) {
                o = getLocalValues(node, ParserConstants.PAGE_PROPERTY_TEMPLATES);
                if (o != null) {
                    Iterator<String> fieldnames = o.fieldNames();
                    while (fieldnames.hasNext()) {
                        String lang = fieldnames.next();
                        Locale locale = BlocksConfig.instance().getLocaleForLanguage(lang);
                        if (isArray(node, lang)) {
                            templates = new HashSet<String>();
                            Iterator<JsonNode> iterator = node.get(lang).iterator();
                            while (iterator.hasNext()) {
                                templates.add(iterator.next().asText());
                            }
                            webPage.setTemplates(templates, locale);
                        }
                    }
                }
            }

            // Add resources

            Set<String> resources = new HashSet<String>();
            if (isArray(node, ParserConstants.PAGE_PROPERTY_RESOURCES)) {
                Iterator<JsonNode> iterator = node.get(ParserConstants.PAGE_PROPERTY_RESOURCES).iterator();
                while (iterator.hasNext()) {
                    resources.add(iterator.next().asText());
                }
            }
            webPage.setTemplates(templates, Locale.ROOT);

            if (isLocalObject(node, ParserConstants.PAGE_PROPERTY_RESOURCES)) {
                o = getLocalValues(node, ParserConstants.PAGE_PROPERTY_RESOURCES);
                if (o != null) {
                    Iterator<String> fieldnames = o.fieldNames();
                    while (fieldnames.hasNext()) {
                        String lang = fieldnames.next();
                        Locale locale = BlocksConfig.instance().getLocaleForLanguage(lang);
                        if (isArray(node, lang)) {
                            resources = new HashSet<String>();
                            Iterator<JsonNode> iterator = node.get(lang).iterator();
                            while (iterator.hasNext()) {
                                templates.add(iterator.next().asText());
                            }
                            webPage.setTemplates(resources, locale);
                        }
                    }
                }
            }

            Set<Map<String, String>> links = new HashSet<Map<String, String>>();
            if (isArray(node, ParserConstants.PAGE_PROPERTY_LINKS)) {
                Iterator<JsonNode> iterator = node.get(ParserConstants.PAGE_PROPERTY_LINKS).iterator();
                while (iterator.hasNext()) {
                    JsonNode next = iterator.next();
                    if (next.isObject()) {
                        HashMap<String, String> link = new HashMap<>();
                        link.put(ParserConstants.PAGE_PROPERTY_ABSOLUTE, next.get(ParserConstants.PAGE_PROPERTY_ABSOLUTE).asText());
                        link.put(ParserConstants.PAGE_PROPERTY_HTML, next.get(ParserConstants.PAGE_PROPERTY_HTML).asText());
                        link.put(ParserConstants.PAGE_PROPERTY_REFERENCED_PAGE, next.get(ParserConstants.PAGE_PROPERTY_REFERENCED_PAGE).asText());
                        links.add(link);
                    }
                }
            }
            webPage.setLinks(links, Locale.ROOT);

            if (isLocalObject(node, ParserConstants.PAGE_PROPERTY_LINKS)) {
                o = getLocalValues(node, ParserConstants.PAGE_PROPERTY_LINKS);
                if (o != null) {
                    Iterator<String> fieldnames = o.fieldNames();
                    while (fieldnames.hasNext()) {
                        String lang = fieldnames.next();
                        Locale locale = BlocksConfig.instance().getLocaleForLanguage(lang);
                        if (isArray(node, lang)) {
                            resources = new HashSet<String>();
                            Iterator<JsonNode> iterator = node.get(lang).iterator();
                            while (iterator.hasNext()) {
                                JsonNode next = iterator.next();
                                if (next.isObject()) {
                                    HashMap<String, String> link = new HashMap<>();
                                    link.put(ParserConstants.PAGE_PROPERTY_ABSOLUTE, next.get(ParserConstants.PAGE_PROPERTY_ABSOLUTE).asText());
                                    link.put(ParserConstants.PAGE_PROPERTY_HTML, next.get(ParserConstants.PAGE_PROPERTY_HTML).asText());
                                    link.put(ParserConstants.PAGE_PROPERTY_REFERENCED_PAGE, next.get(ParserConstants.PAGE_PROPERTY_REFERENCED_PAGE).asText());
                                    links.add(link);
                                }
                            }
                            webPage.setLinks(links, locale);
                        }
                    }
                }
            }

            webPage.setLinks(links);

        }

    }

    // ---------- PRIVATE METHDOS --------------
    private boolean isWebPage(JsonNode node)
    {
        boolean retVal = false;
        if (node.isObject() && node.has(ParserConstants.PAGE_PROPERTY_HTML)) {
            retVal = true;
        }
        else if (node.isObject() && node.has(ParserConstants.PAGE_PROPERTY) && node.get(ParserConstants.PAGE_PROPERTY).has(ParserConstants.PAGE_PROPERTY_HTML)) {
            retVal = true;
        }
        return retVal;
    }

    private JsonNode getPageNode(JsonNode node)
    {
        JsonNode retVal = node;
        if (node.isObject() && node.has(ParserConstants.PAGE_PROPERTY) && isWebPage(node.get(ParserConstants.PAGE_PROPERTY))) {
            retVal = node.get(ParserConstants.PAGE_PROPERTY);
        }
        return retVal;
    }

    private JsonNode getLocalValues(JsonNode node, String property)
    {
        JsonNode retVal = null;
        if (node.has(property + ParserConstants.LOCALIZED_PROPERTY) && node.get(property + ParserConstants.LOCALIZED_PROPERTY).isObject()) {
            retVal = node.get(property + ParserConstants.LOCALIZED_PROPERTY);
        }
        return retVal;
    }

    private boolean isLocalObject(JsonNode node, String property)
    {
        return getLocalValues(node, property) != null && getLocalValues(node, property).isObject();
    }

    private boolean isLocalArray(JsonNode node, String property)
    {
        return getLocalValues(node, property) != null && getLocalValues(node, property).isArray();
    }

    private boolean isArray(JsonNode node, String property)
    {
        return node.has(property) && node.get(property).isArray();
    }

}