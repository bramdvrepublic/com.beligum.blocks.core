package com.beligum.blocks.fs.index.entries;

import org.hibernate.search.annotations.*;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

/**
 * Created by bram on 2/13/16.
 */
@Indexed
public class PageIndexEntry extends AbstractIndexEntry
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    @Field(index = Index.YES, analyze = Analyze.NO, store = Store.YES)
    private URI resource;
    @Field(index = Index.YES, analyze = Analyze.YES, store = Store.YES)
    private String title;
    @Field(index = Index.YES, analyze = Analyze.YES, store = Store.YES)
    private String language;
    @Field(index = Index.YES, analyze = Analyze.NO, store = Store.YES)
    private URI parent;
    @IndexedEmbedded()
    private Map<String, URI> translations;

    //-----CONSTRUCTORS-----
    public PageIndexEntry() throws IOException
    {
        super(null);
    }

    //-----PUBLIC METHODS-----
    public URI getResource()
    {
        return resource;
    }
    public void setResource(URI resource)
    {
        this.resource = resource;
    }
    public String getTitle()
    {
        return title;
    }
    public void setTitle(String title)
    {
        this.title = title;
    }
    public String getLanguage()
    {
        return language;
    }
    public void setLanguage(String language)
    {
        this.language = language;
    }
    public URI getParent()
    {
        return parent;
    }
    public void setParent(URI parent)
    {
        this.parent = parent;
    }
    public Map<String, URI> getTranslations()
    {
        return translations;
    }
    public void setTranslations(Map<String, URI> translations)
    {
        this.translations = translations;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----
    @Override
    public String toString()
    {
        return "PageIndexEntry{" +
               "id='" + id + '\'' +
               ", title='" + title + '\'' +
               ", language='" + language + '\'' +
               ", parent=" + parent +
               ", translations=" + translations +
               '}';
    }
}
