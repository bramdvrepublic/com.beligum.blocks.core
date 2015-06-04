package com.beligum.blocks.pages;

import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.models.resources.AbstractResource;
import com.beligum.blocks.models.resources.interfaces.Node;
import com.beligum.blocks.models.resources.interfaces.ResourceController;
import com.beligum.blocks.models.resources.orient.OrientResourceController;
import com.beligum.blocks.pages.ifaces.WebPage;
import com.tinkerpop.blueprints.Vertex;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.*;

/**
 * Created by wouter on 28/05/15.
 */
public class OWebPage extends AbstractResource implements WebPage
{
    public static String LANGUAGE = "@language";
    public static String ID = "@id";
    public static String CLASS_NAME = "WebPage";
    public static String TITLE = "title";
    public static String HTML = "html";
    public static String RESOURCES = "resources";
    public static String LINKS = "links";
    public static String PROPERTIES = "properties";

    private Vertex vertex;
    private Locale locale;

    public OWebPage(Vertex vertex, Locale locale) {
        this.vertex = vertex;
        this.vertex.setProperty(LANGUAGE, locale.getLanguage());
        this.locale = locale;
    }

    @Override
    public Object getDBId()
    {
        return vertex.getId();
    }
    @Override
    public String getBlockId() {
        return vertex.getProperty(ID);
    }

    @Override
    public void setFieldDirect(String key, Object value, Locale locale)
    {
        HashMap<String, Object> properties = (HashMap<String, Object>)vertex.getProperty(PROPERTIES);
        if (properties == null) properties = new HashMap<String, Object>();
        if (value instanceof Iterable) {
            properties.put(key, new ArrayList());
            for (Object v: (Iterable)value) {
                addFieldDirect(key, getResourceController().asNode(v, locale));
            }
        } else {
            properties.put(key, value);
        }
    }

    @Override
    public Node getFieldDirect(String key)
    {
        Node retVal = null;
        HashMap<String, Object> properties = (HashMap<String, Object>)vertex.getProperty(PROPERTIES);
        if (properties == null) properties = new HashMap<String, Object>();
        if (properties.containsKey(key)) {
            retVal = getResourceController().asNode(properties.get(key), getLanguage());
        } else {
            retVal = getResourceController().asNode(null, getLanguage());
        }
        return retVal;
    }

    @Override
    public void addFieldDirect(String key, Node node)
    {
        HashMap<String, Object> properties = (HashMap<String, Object>)vertex.getProperty(PROPERTIES);
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
        HashMap<String, Object> properties = (HashMap<String, Object>)vertex.getProperty(PROPERTIES);
        if (properties == null) properties = new HashMap<String, Object>();
        return getResourceController().asNode(properties.remove(key), this.getLanguage());
    }

    @Override
    public String getTitle()
    {
        return (String)vertex.getProperty(TITLE);
    }

    @Override
    public void setTitle(String title)
    {
        vertex.setProperty(TITLE, title);
    }

    @Override
    public String getHtml()
    {
        return (String)vertex.getProperty(HTML);
    }

    @Override
    public void setHtml(String html)
    {
        vertex.setProperty(HTML, html);
    }

    @Override
    public Locale getLanguage() {
        Locale retVal = this.locale;
        if (retVal == null) {
            String language = vertex.getProperty(LANGUAGE);
            retVal = BlocksConfig.instance().getLocaleForLanguage(language);
            this.locale = retVal;
        }
        return retVal;
    }
    @Override
    public ResourceController getResourceController()
    {
        return null;
    }


    @Override
    public Set<String> getResources()
    {
        Set<String> retVal = vertex.getProperty(RESOURCES);
        if (retVal == null) {
            retVal = new HashSet<String>();
        }
        return retVal;
    }

    @Override
    public void addResource(String resource)
    {
        Set<String> retVal = getResources();
        if (retVal == null) {
            retVal = new HashSet<String>();
        }
        retVal.add(resource);
        this.vertex.setProperty(RESOURCES, retVal);
    }

    @Override
    public Set<String> getLinks()
    {
        Set<String> retVal = vertex.getProperty(LINKS);
        if (retVal == null) {
            retVal = new HashSet<String>();
        }
        return retVal;
    }

    @Override
    public void addLink(String link)
    {
        Set<String> retVal = getLinks();
        if (retVal == null) {
            retVal = new HashSet<String>();
        }
        retVal.add(link);
        this.vertex.setProperty(LINKS, retVal);
    }


    @Override
    public Set<URI> getFields()
    {
        HashMap<String, Object> properties = vertex.getProperty(PROPERTIES);
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

    @Override
    public void setCreatedAt(Date date)
    {
        this.vertex.setProperty(OrientResourceController.CREATED_AT, date);
    }
    @Override
    public Calendar getCreatedAt()
    {
        return this.vertex.getProperty(OrientResourceController.CREATED_AT);
    }

    @Override
    public void setCreatedBy(String user)
    {
        this.vertex.setProperty(OrientResourceController.CREATED_BY, user);
    }

    @Override
    public String getCreatedBy()
    {
        return this.vertex.getProperty(OrientResourceController.CREATED_BY);
    }

    @Override
    public void setUpdatedAt(Date date)
    {
        this.vertex.setProperty(OrientResourceController.UPDATED_AT, date);
    }

    @Override
    public Calendar getUpdatedAt()
    {
        return this.vertex.getProperty(OrientResourceController.UPDATED_AT);
    }

    @Override
    public void setUpdatedBy(String user)
    {
        this.vertex.setProperty(OrientResourceController.UPDATED_BY, user);
    }

    @Override
    public String getUpdatedBy()
    {
        return this.vertex.getProperty(OrientResourceController.UPDATED_BY);
    }

}
