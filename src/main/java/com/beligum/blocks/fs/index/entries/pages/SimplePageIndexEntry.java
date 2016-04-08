package com.beligum.blocks.fs.index.entries.pages;

import com.beligum.blocks.fs.index.entries.IndexEntry;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

import java.io.IOException;
import java.net.URI;

/**
 * Created by bram on 2/13/16.
 */
public class SimplePageIndexEntry extends AbstractPageIndexEntry implements PageIndexEntry
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private String resource;
    private String typeOf;
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
        retVal.add(new StringField(IndexEntry.Field.id.name(), entry.getId().toString(), org.apache.lucene.document.Field.Store.YES));
        //don't store it, we just add it to the index to be able to query the URI (again) more naturally
        retVal.add(new TextField(IndexEntry.Field.tokenisedId.name(), entry.getId().toString(), org.apache.lucene.document.Field.Store.NO));
        if (entry.getResource() != null) {
            retVal.add(new StringField(PageIndexEntry.Field.resource.name(), entry.getResource(), org.apache.lucene.document.Field.Store.YES));
        }
        if (entry.getTypeOf() != null) {
            retVal.add(new StringField(PageIndexEntry.Field.typeOf.name(), entry.getTypeOf(), org.apache.lucene.document.Field.Store.YES));
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
        SimplePageIndexEntry retVal = new SimplePageIndexEntry(URI.create(document.get(IndexEntry.Field.id.name())));

        retVal.setResource(document.get(PageIndexEntry.Field.resource.name()));
        retVal.setTypeOf(document.get(PageIndexEntry.Field.typeOf.name()));
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
    public String getTypeOf()
    {
        return typeOf;
    }
    public void setTypeOf(String typeOf)
    {
        this.typeOf = typeOf;
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
