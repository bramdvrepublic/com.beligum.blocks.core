package com.beligum.blocks.fs.index.entries;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

import java.io.IOException;
import java.net.URI;

/**
 * Created by bram on 2/13/16.
 */
public class SimplePageIndexEntry extends AbstractIndexEntry implements PageIndexEntry
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private String resource;
    private String title;
    private String language;
    private String parent;

    //-----CONSTRUCTORS-----
    public SimplePageIndexEntry(URI id) throws IOException
    {
        super(id);
    }

    //-----STATIC METHODS-----
    public static Document toLuceneDoc(PageIndexEntry entry)
    {
        Document retVal = new Document();

        //note: StringField = un-analyzed + indexed
        //      TextField = standard analyzed + indexed
        //      StoredField = not indexed at all

        //Note: we also need to insert the id of the doc even though it's an index
        retVal.add(new StringField(AbstractIndexEntry.Field.id.name(), entry.getId().toString(), org.apache.lucene.document.Field.Store.YES));
        if (entry.getResource() != null) {
            retVal.add(new StringField(PageIndexEntry.Field.resource.name(), entry.getResource(), org.apache.lucene.document.Field.Store.YES));
        }
        if (entry.getTitle() != null) {
            retVal.add(new TextField(PageIndexEntry.Field.title.name(), entry.getTitle(), org.apache.lucene.document.Field.Store.YES));
        }
        if (entry.getLanguage() != null) {
            retVal.add(new StringField(PageIndexEntry.Field.language.name(), entry.getLanguage(), org.apache.lucene.document.Field.Store.YES));
        }
        if (entry.getParent() != null) {
            retVal.add(new StringField(PageIndexEntry.Field.parent.name(), entry.getParent(), org.apache.lucene.document.Field.Store.YES));
        }

        return retVal;
    }
    public static SimplePageIndexEntry fromLuceneDoc(Document document) throws IOException
    {
        SimplePageIndexEntry retVal = new SimplePageIndexEntry(URI.create(document.get(AbstractIndexEntry.Field.id.name())));

        retVal.setResource(document.get(PageIndexEntry.Field.resource.name()));
        retVal.setTitle(document.get(PageIndexEntry.Field.title.name()));
        retVal.setLanguage(document.get(PageIndexEntry.Field.language.name()));
        retVal.setParent(document.get(PageIndexEntry.Field.parent.name()));

        return retVal;
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getResource()
    {
        return resource;
    }
    public void setResource(String resource)
    {
        this.resource = resource;
    }
    @Override
    public String getTitle()
    {
        return title;
    }
    public void setTitle(String title)
    {
        this.title = title;
    }
    @Override
    public String getLanguage()
    {
        return language;
    }
    public void setLanguage(String language)
    {
        this.language = language;
    }
    @Override
    public String getParent()
    {
        return parent;
    }
    public void setParent(String parent)
    {
        this.parent = parent;
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
               '}';
    }
}
