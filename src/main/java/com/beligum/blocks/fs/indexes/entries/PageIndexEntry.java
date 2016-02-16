package com.beligum.blocks.fs.indexes.entries;

import com.beligum.blocks.fs.pages.ifaces.Page;
import com.beligum.blocks.templating.blocks.HtmlAnalyzer;
import org.hibernate.search.annotations.*;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by bram on 2/13/16.
 */
@Indexed
public class PageIndexEntry extends AbstractIndexEntry
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    @Field(index = Index.YES, analyze = Analyze.YES, store = Store.YES)
    private final String title;
    @Field(index = Index.YES, analyze = Analyze.YES, store = Store.YES)
    private final String language;
    @Field(index = Index.YES, analyze = Analyze.YES, store = Store.YES)
    private final URI parent;
    @IndexedEmbedded()
    private final Map<String, URI> translations;

    //-----CONSTRUCTORS-----
    public PageIndexEntry() throws IOException
    {
        this(null);
    }
    public PageIndexEntry(Page page) throws IOException
    {
        //the ID of the stub is the public URI
        super(page.buildAddress());

        HtmlAnalyzer htmlAnalyzer = page.createAnalyzer();

        this.title = htmlAnalyzer.getTitle();
        this.language = htmlAnalyzer.getHtmlLanguage().getLanguage();
        this.parent = htmlAnalyzer.getParent() == null ? null : htmlAnalyzer.getParent().parentUri;
        this.translations = new LinkedHashMap<>();
        if (htmlAnalyzer.getTranslations() != null) {
            for (Map.Entry<URI, HtmlAnalyzer.TranslationRef> e : htmlAnalyzer.getTranslations().entrySet()) {
                this.translations.put(e.getValue().locale.getLanguage(), e.getKey());
            }
        }
    }

    //-----PUBLIC METHODS-----
    public String getTitle()
    {
        return title;
    }
    public String getLanguage()
    {
        return language;
    }
    public URI getParent()
    {
        return parent;
    }
    public Map<String, URI> getTranslations()
    {
        return translations;
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
