package com.beligum.blocks.fs.index.entries;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

import java.io.IOException;
import java.net.URI;

/**
 * Created by bram on 2/13/16.
 */
public class PageIndexEntry extends AbstractIndexEntry
{
    //-----CONSTANTS-----
    //note: sync these with the variable names below
    public enum Field
    {
        resource,
        title,
        language,
        parent
    }

    //-----VARIABLES-----
    private String resource;
    private String title;
    private String language;
    private String parent;

    //-----CONSTRUCTORS-----
    public PageIndexEntry(URI id) throws IOException
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

        //this is the id of the entry, not a value
        //retVal.add(new StringField(Field.id.name(), entry.id.toString(), org.apache.lucene.document.Field.Store.YES));

        retVal.add(new StringField(Field.resource.name(), entry.resource, org.apache.lucene.document.Field.Store.YES));
        retVal.add(new TextField(Field.title.name(), entry.title, org.apache.lucene.document.Field.Store.YES));
        retVal.add(new StringField(Field.language.name(), entry.language, org.apache.lucene.document.Field.Store.YES));
        retVal.add(new StringField(Field.parent.name(), entry.parent, org.apache.lucene.document.Field.Store.YES));

        return retVal;
    }
    public static PageIndexEntry fromLuceneDoc(Document document) throws IOException
    {
        PageIndexEntry retVal = new PageIndexEntry(URI.create(document.get(AbstractIndexEntry.Field.id.name())));

        retVal.setResource(document.get(Field.resource.name()));
        retVal.setTitle(document.get(Field.title.name()));
        retVal.setLanguage(document.get(Field.language.name()));
        retVal.setParent(document.get(Field.parent.name()));

        return retVal;
    }

    //-----PUBLIC METHODS-----
    public String getResource()
    {
        return resource;
    }
    public void setResource(String resource)
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
