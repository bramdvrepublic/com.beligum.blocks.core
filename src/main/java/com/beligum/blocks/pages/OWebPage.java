package com.beligum.blocks.pages;

import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.database.OBlocksDatabase;
import com.beligum.blocks.database.interfaces.BlocksDatabase;
import com.beligum.blocks.pages.ifaces.WebPage;
import com.beligum.blocks.resources.AbstractResource;
import com.beligum.blocks.resources.interfaces.Node;
import com.tinkerpop.blueprints.Vertex;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.*;

/**
 * Created by wouter on 28/05/15.
 */
public class OWebPage extends AbstractResource implements WebPage
{

    private boolean hasChanged = false;
    private Vertex vertex;
    private Locale locale;

    public OWebPage(Vertex vertex, Locale locale) {
        this.vertex = vertex;
        this.vertex.setProperty(ParserConstants.JSONLD_LANGUAGE, locale.getLanguage());
        this.locale = locale;
    }

    @Override
    public Object getDBId()
    {
        return vertex.getId();
    }
    @Override
    public URI getBlockId() {
        return UriBuilder.fromUri((String)vertex.getProperty(ParserConstants.JSONLD_ID)).build();
    }

    @Override
    public void setFieldDirect(String key, Object value, Locale locale)
    {
        HashMap<String, Object> properties = (HashMap<String, Object>)vertex.getProperty(OBlocksDatabase.WEB_PAGE_PROPERTIES);
        if (properties == null) properties = new HashMap<String, Object>();
        if (value instanceof Iterable) {
            properties.put(key, new ArrayList());
            for (Object v: (Iterable)value) {
                addFieldDirect(key, OBlocksDatabase.instance().createNode(v, locale));
            }
        } else {
            properties.put(key, value);
        }
    }

    @Override
    public Node getFieldDirect(String key)
    {
        Node retVal = null;
        HashMap<String, Object> properties = (HashMap<String, Object>)vertex.getProperty(OBlocksDatabase.WEB_PAGE_PROPERTIES);
        if (properties == null) properties = new HashMap<String, Object>();
        if (properties.containsKey(key)) {
            retVal = OBlocksDatabase.instance().createNode(properties.get(key), getLanguage());
        } else {
            retVal = OBlocksDatabase.instance().createNode(null, getLanguage());
        }
        return retVal;
    }

    @Override
    public void addFieldDirect(String key, Node node)
    {
        HashMap<String, Object> properties = (HashMap<String, Object>)vertex.getProperty(OBlocksDatabase.WEB_PAGE_PROPERTIES);
        if (properties == null) properties = new HashMap<String, Object>();
        if (node.isIterable()) {
            for (Node value: node) {
                addFieldDirect(key, value);
            }
        } else if (!properties.containsKey(key)) {
            properties.put(key, node.getValue());
        } else if (!(properties.get(key) instanceof List)) {
            Object value = properties.get(key);
            List list = new ArrayList();
            list.add(value);
            list.add(node.getValue());
            properties.put(key, value);
        } else {
            ((List)properties.get(key)).add(node.getValue());
        }
    }

    @Override
    public Node removeFieldDirect(String key)
    {
        HashMap<String, Object> properties = (HashMap<String, Object>)vertex.getProperty(OBlocksDatabase.WEB_PAGE_PROPERTIES);
        if (properties == null) properties = new HashMap<String, Object>();
        return OBlocksDatabase.instance().createNode(properties.remove(key), this.getLanguage());
    }

    @Override
    public String getParsedHtml()
    {
        return (String)vertex.getProperty(OBlocksDatabase.WEB_PAGE_HTML);
    }

    @Override
    public void setParsedHtml(String html)
    {
        vertex.setProperty(OBlocksDatabase.WEB_PAGE_HTML, html);
    }

    @Override
    public void setPageTemplate(String template)
    {
        vertex.setProperty(OBlocksDatabase.WEB_PAGE_TEMPLATE, template);
    }
    @Override
    public String getPageTemplate()
    {
        return vertex.getProperty(OBlocksDatabase.WEB_PAGE_TEMPLATE);
    }

    @Override
    public String getText()
    {
        return (String)vertex.getProperty(OBlocksDatabase.WEB_PAGE_TEXT);
    }

    @Override
    public void setText(String html)
    {
        vertex.setProperty(OBlocksDatabase.WEB_PAGE_TEXT, html);
    }

    @Override
    public Locale getLanguage() {
        Locale retVal = this.locale;
        if (retVal == null) {
            String language = vertex.getProperty(ParserConstants.JSONLD_LANGUAGE);
            retVal = BlocksConfig.instance().getLocaleForLanguage(language);
            this.locale = retVal;
        }
        return retVal;
    }
    @Override
    public BlocksDatabase getDatabase()
    {
        return null;
    }


    @Override
    public Set<String> getTemplates()
    {
        Set<String> retVal = vertex.getProperty(OBlocksDatabase.WEB_PAGE_TEMPLATES);
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
        vertex.setProperty(OBlocksDatabase.WEB_PAGE_TEMPLATES, templates);
    }

    @Override
    public void addTemplate(String template)
    {
        Set<String> retVal = getResources();
        retVal.add(template);
        this.vertex.setProperty(OBlocksDatabase.WEB_PAGE_TEMPLATES, retVal);
    }

    @Override
    public Set<String> getResources()
    {
        Set<String> retVal = vertex.getProperty(OBlocksDatabase.WEB_PAGE_RESOURCES);
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
        vertex.setProperty(OBlocksDatabase.WEB_PAGE_RESOURCES, resources);
    }

    @Override
    public void addResource(String resource)
    {
        Set<String> retVal = getResources();
        retVal.add(resource);
        this.vertex.setProperty(OBlocksDatabase.WEB_PAGE_RESOURCES, retVal);
    }

    @Override
    public Set<HashMap<String, String>> getLinks()
    {
        Set<HashMap<String, String>> retVal = vertex.getProperty(OBlocksDatabase.WEB_PAGE_LINKS);
        if (retVal == null) {
            retVal = new HashSet<HashMap<String, String>>();
        }
        return retVal;
    }

    @Override
    public void setLinks(Set<HashMap<String, String>> links)
    {
        this.vertex.setProperty(OBlocksDatabase.WEB_PAGE_LINKS, links);
    }

    @Override
    public void addLink(HashMap<String, String> link)
    {
        Set<HashMap<String, String>> retVal = getLinks();
        if (retVal == null) {
            retVal = new HashSet<HashMap<String, String>>();
        }
        retVal.add(link);
        this.vertex.setProperty(OBlocksDatabase.WEB_PAGE_LINKS, retVal);
    }

    @Override
    public Set<URI> getFields()
    {
        HashMap<String, Object> properties = vertex.getProperty(OBlocksDatabase.WEB_PAGE_PROPERTIES);
        if (properties == null) properties = new HashMap<String, Object>();
        Set<URI> fields = new HashSet();
        for (String key: properties.keySet()) {
            String field = this.getContext().get(key);
            if (field != null) {
                fields.add(UriBuilder.fromUri(field).build());
            }
        }

        return fields;
    }

    public Vertex getVertex() {
        return this.vertex;
    }

    public void setChanged() {
        this.hasChanged = true;
    }

    public boolean hasChanged() {
        return this.hasChanged;
    }

    @Override
    public Object getValue() {
        return this.vertex;
    }

    @Override
    public void setCreatedAt(Calendar date)
    {
        this.vertex.setProperty(OBlocksDatabase.RESOURCE_CREATED_AT, date.getTime());
    }
    @Override
    public Calendar getCreatedAt()
    {
        Calendar retVal = null;
        Date date = this.vertex.getProperty(OBlocksDatabase.RESOURCE_CREATED_AT);
        if (date != null) {
            retVal = Calendar.getInstance();
            retVal.setTime(date);
        }
        return retVal;

    }

    @Override
    public void setCreatedBy(String user)
    {
        this.vertex.setProperty(OBlocksDatabase.RESOURCE_CREATED_BY, user);
    }

    @Override
    public String getCreatedBy()
    {
        return this.vertex.getProperty(OBlocksDatabase.RESOURCE_CREATED_BY);
    }

    @Override
    public void setUpdatedAt(Calendar date)
    {
        this.vertex.setProperty(OBlocksDatabase.RESOURCE_UPDATED_AT, date.getTime());
    }

    @Override
    public Calendar getUpdatedAt()
    {
        Calendar retVal = null;
        Date date = this.vertex.getProperty(OBlocksDatabase.RESOURCE_UPDATED_AT);
        if (date != null) {
            retVal = Calendar.getInstance();
            retVal.setTime(date);
        }
        return retVal;

    }

    @Override
    public void setUpdatedBy(String user)
    {
        this.vertex.setProperty(OBlocksDatabase.RESOURCE_UPDATED_BY, user);
    }

    @Override
    public String getUpdatedBy()
    {
        return this.vertex.getProperty(OBlocksDatabase.RESOURCE_UPDATED_BY);
    }

}
