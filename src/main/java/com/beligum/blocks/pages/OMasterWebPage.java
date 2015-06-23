package com.beligum.blocks.pages;

import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.database.OBlocksDatabase;
import com.beligum.blocks.pages.ifaces.MasterWebPage;
import com.beligum.blocks.pages.ifaces.WebPage;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.*;

/**
 * Created by wouter on 19/06/15.
 */
public class OMasterWebPage implements MasterWebPage
{
    private Vertex vertex;

    public OMasterWebPage(Vertex vertex) {
        this.vertex = vertex;
    }

    @Override
    public URI getBlockId() {
        URI retVal = null;
        String value = vertex.getProperty(ParserConstants.JSONLD_ID);
        if (value != null) {
            retVal = UriBuilder.fromUri(value).build();
        }
        return retVal;
    }

    public Set<Locale> getLanguages() {
        return new HashSet<>();
    }

    public Locale getDefaultLanguage() {
        String value = vertex.getProperty(ParserConstants.JSONLD_LANGUAGE);
        return BlocksConfig.instance().getLocaleForLanguage(value);
    }

    public WebPage getPageForLocale(Locale locale) {
        Iterable<Edge> edges = this.vertex.query().direction(Direction.OUT).labels(OBlocksDatabase.WEB_PAGE_LOCALIZED_CLASS).has(ParserConstants.JSONLD_LANGUAGE, locale.getLanguage()).edges();
        WebPage retVal = null;
        for (Edge edge: edges) {
            retVal = new OWebPage(edge.getVertex(Direction.IN), locale);
            break;
        }
        return retVal;
    }

    public Vertex getVertex() {
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
