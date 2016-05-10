package com.beligum.blocks.fs.index.entries.pages;

import com.beligum.base.server.R;
import com.beligum.blocks.config.RdfFactory;
import com.beligum.blocks.fs.index.entries.IndexEntry;
import com.beligum.blocks.fs.index.entries.resources.ResourceIndexEntry;
import com.beligum.blocks.fs.pages.ifaces.Page;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.templating.blocks.HtmlAnalyzer;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;

/**
 * Created by bram on 2/13/16.
 */
public class SimplePageIndexEntry extends AbstractPageIndexEntry implements PageIndexEntry
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private URI resource;
    private RdfClass typeOf;
    private Locale language;
    private URI canonicalAddress;

    //-----CONSTRUCTORS-----
    public SimplePageIndexEntry(URI id, URI resource, RdfClass typeOf, String title, Locale language, URI canonicalAddress, String description, URI image) throws IOException
    {
        super(id);

        this.resource = resource;
        this.typeOf = typeOf;
        this.title = title;
        this.language = language;
        this.canonicalAddress = canonicalAddress;
        this.description = description;
        this.image = image;
    }
    public SimplePageIndexEntry(Page page) throws IOException
    {
        super(page.getPublicRelativeAddress());

        HtmlAnalyzer htmlAnalyzer = page.createAnalyzer();
        String typeOfCurie = htmlAnalyzer.getHtmlTypeof() == null ? null : htmlAnalyzer.getHtmlTypeof().value;
        RdfClass typeOf = typeOfCurie == null ? null : RdfFactory.getClassForResourceType(URI.create(typeOfCurie));
        if (typeOf == null) {
            throw new IOException("Trying to create an index entra from a page without a type, this shouldn't happen; " + page.getPublicRelativeAddress());
        }
        else {
            //don't use the canonical address as the id of the entry: it's not unique (will be the same for different languages)
            this.setResource(htmlAnalyzer.getHtmlAbout() == null ? null : htmlAnalyzer.getHtmlAbout().value);
            this.setTypeOf(typeOf.getCurieName().toString());
            this.setTitle(htmlAnalyzer.getTitle());
            this.setLanguage(htmlAnalyzer.getHtmlLanguage() == null ? null : htmlAnalyzer.getHtmlLanguage().getLanguage());
            this.setCanonicalAddress(page.getCanonicalAddress() == null ? null : page.getCanonicalAddress().toString());

            //note: the getResourceIndexer() never returns null (has a SimpleResourceIndexer as fallback)
            ResourceIndexEntry indexEntry = typeOf.getResourceIndexer().index(page.readRdfModel());
            //overwrite the title if the indexer found a better match (note that the indexer can generate any kind of title it wants, not just the page <title>)
            if (StringUtils.isBlank(this.getTitle()) && !StringUtils.isBlank(indexEntry.getTitle())) {
                this.setTitle(indexEntry.getTitle());
            }
            this.setDescription(indexEntry.getDescription());
            this.setImage(indexEntry.getImage() == null ? null : indexEntry.getImage().toString());
        }
    }
    private SimplePageIndexEntry(String id, String resource, String typeOfCurie, String title, String language, String canonicalAddress, String description, String image) throws IOException
    {
        super(URI.create(id));

        this.setResource(resource);
        this.setTypeOf(typeOfCurie);
        this.setTitle(title);
        this.setLanguage(language);
        this.setCanonicalAddress(canonicalAddress);
        this.setDescription(description);
        this.setImage(image);
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
            retVal.add(new StringField(PageIndexEntry.Field.resource.name(), entry.getResource() == null ? null : entry.getResource().toString(), org.apache.lucene.document.Field.Store.YES));
        }
        if (entry.getTypeOf() != null) {
            retVal.add(new StringField(PageIndexEntry.Field.typeOf.name(), entry.getTypeOf() == null ? null : entry.getTypeOf().getCurieName().toString(), org.apache.lucene.document.Field.Store.YES));
        }
        if (entry.getTitle() != null) {
            retVal.add(new TextField(IndexEntry.Field.title.name(), entry.getTitle(), org.apache.lucene.document.Field.Store.YES));
        }
        if (entry.getLanguage() != null) {
            retVal.add(new StringField(PageIndexEntry.Field.language.name(), entry.getLanguage() == null ? null : entry.getLanguage().getLanguage(), org.apache.lucene.document.Field.Store.YES));
        }
        if (entry.getCanonicalAddress() != null) {
            retVal.add(new StringField(PageIndexEntry.Field.canonicalAddress.name(), entry.getCanonicalAddress() == null ? null : entry.getCanonicalAddress().toString(),
                                       org.apache.lucene.document.Field.Store.YES));
        }
        if (entry.getDescription() != null) {
            retVal.add(new StringField(IndexEntry.Field.description.name(), entry.getDescription(), org.apache.lucene.document.Field.Store.YES));
        }
        if (entry.getImage() != null) {
            retVal.add(new StringField(IndexEntry.Field.image.name(), entry.getImage() == null ? null : entry.getImage().toString(), org.apache.lucene.document.Field.Store.YES));
        }

        return retVal;
    }
    public static SimplePageIndexEntry fromLuceneDoc(Document document) throws IOException
    {
        return new SimplePageIndexEntry(document.get(IndexEntry.Field.id.name()),
                                        document.get(PageIndexEntry.Field.resource.name()),
                                        document.get(PageIndexEntry.Field.typeOf.name()),
                                        document.get(IndexEntry.Field.title.name()),
                                        document.get(PageIndexEntry.Field.language.name()),
                                        document.get(PageIndexEntry.Field.canonicalAddress.name()),
                                        document.get(IndexEntry.Field.description.name()),
                                        document.get(IndexEntry.Field.image.name()));
    }

    //-----PUBLIC METHODS-----
    @Override
    public URI getResource()
    {
        return resource;
    }
    private void setResource(String resource)
    {
        this.resource = resource == null ? null : URI.create(resource);
    }
    @Override
    public URI getCanonicalAddress()
    {
        return canonicalAddress;
    }
    private void setCanonicalAddress(String canonicalAddress)
    {
        this.canonicalAddress = canonicalAddress == null ? null : URI.create(canonicalAddress);
    }
    @Override
    public RdfClass getTypeOf()
    {
        return typeOf;
    }
    private void setTypeOf(String typeOfCurie)
    {
        this.typeOf = typeOfCurie == null ? null : RdfFactory.getClassForResourceType(URI.create(typeOfCurie));
    }
    @Override
    public Locale getLanguage()
    {
        return language;
    }
    private void setLanguage(String language)
    {
        this.language = language == null ? null : R.configuration().getLocaleForLanguage(language);
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
               '}';
    }
}
