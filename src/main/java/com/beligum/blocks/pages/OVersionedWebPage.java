package com.beligum.blocks.pages;

import com.beligum.blocks.database.OBlocksDatabase;
import com.beligum.blocks.pages.ifaces.VersionedWebPage;
import com.beligum.blocks.pages.ifaces.WebPage;
import com.tinkerpop.blueprints.Vertex;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by wouter on 19/06/15.
 */
public class OVersionedWebPage implements VersionedWebPage
{
    private Vertex vertex;

    public OVersionedWebPage(Vertex vertex, WebPage webPage) {
        this.vertex = vertex;
        StringBuilder sb = new StringBuilder();
        sb.append("<").append(webPage.getPageTemplate()).append(">").append(webPage.getParsedHtml()).append("</").append(webPage.getPageTemplate()).append(">");
        this.setHtml(sb.toString());
        this.setCreatedBy(webPage.getCreatedBy());
        this.setCreatedAt(webPage.getCreatedAt());
        this.setUpdatedAt(webPage.getUpdatedAt());
        this.setUpdatedBy(webPage.getUpdatedBy());
    }

    @Override
    public String getHtml()
    {
        return vertex.getProperty(OBlocksDatabase.WEB_PAGE_HTML);
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


    // ---------- PRIVATE METHODS ------------

    private void setHtml(String html)
    {
        vertex.setProperty(OBlocksDatabase.WEB_PAGE_HTML, html);
    }
}
