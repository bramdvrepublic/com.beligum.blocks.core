package com.beligum.blocks.filesystem.index.entries.pages;

import com.beligum.blocks.config.RdfFactory;
import com.beligum.blocks.filesystem.index.entries.IndexEntry;
import com.beligum.blocks.filesystem.index.entries.resources.ResourceIndexer;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.templating.blocks.analyzer.HtmlAnalyzer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchema;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchemaLoader;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StoredField;
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
    private static ProtobufSchema PROTOBUF_SCHEMA;

    //-----VARIABLES-----
    private String resource;
    private String typeOf;
    private String language;
    private String canonicalAddress;

    //-----CONSTRUCTORS-----
    //for serialization
    private SimplePageIndexEntry() throws IOException
    {
        this((URI) null, (URI) null, (RdfClass) null, (String) null, (Locale) null, (URI) null, (String) null, (URI) null);
    }
    public SimplePageIndexEntry(URI id, URI resource, RdfClass typeOf, String title, Locale language, URI canonicalAddress, String description, URI image) throws IOException
    {
        this(id == null ? null : id.toString(),
             resource == null ? null : resource.toString(),
             typeOf == null ? null : typeOf.getCurieName().toString(),
             title,
             language == null ? null : language.getLanguage(),
             canonicalAddress == null ? null : canonicalAddress.toString(),
             description,
             image == null ? null : image.toString());
    }
    private SimplePageIndexEntry(String id, String resource, String typeOfCurie, String title, String language, String canonicalAddress, String description, String image) throws IOException
    {
        super(id);

        this.setResource(resource);
        this.setTypeOf(typeOfCurie);
        this.setTitle(title);
        this.setLanguage(language);
        this.setCanonicalAddress(canonicalAddress);
        this.setDescription(description);
        this.setImage(image);
    }
    public SimplePageIndexEntry(Page page) throws IOException
    {
        super(page.getPublicRelativeAddress().toString());

        HtmlAnalyzer htmlAnalyzer = page.createAnalyzer();
        String typeOfCurie = htmlAnalyzer.getHtmlTypeof() == null ? null : htmlAnalyzer.getHtmlTypeof().value;
        RdfClass typeOf = typeOfCurie == null ? null : RdfFactory.getClassForResourceType(URI.create(typeOfCurie));
        if (typeOf == null) {
            throw new IOException("Trying to create an index entry from a page without a type, this shouldn't happen; " + page.getPublicRelativeAddress());
        }
        else {
            //First, set the general properties, then add more using specific indexers and see if we need to override some general ones

            //don't use the canonical address as the id of the entry: it's not unique (will be the same for different languages)
            this.setResource(htmlAnalyzer.getHtmlAbout() == null ? null : htmlAnalyzer.getHtmlAbout().value);
            this.setTypeOf(typeOf.getCurieName().toString());
            this.setTitle(htmlAnalyzer.getTitle());
            this.setLanguage(htmlAnalyzer.getHtmlLanguage() == null ? null : htmlAnalyzer.getHtmlLanguage().getLanguage());
            this.setCanonicalAddress(page.getCanonicalAddress() == null ? null : page.getCanonicalAddress().toString());

            //note: the getResourceIndexer() never returns null (has a SimpleResourceIndexer as fallback)
            ResourceIndexer.IndexedResource indexEntry = typeOf.getResourceIndexer().index(page.readRdfModel());
            //overwrite the title if the indexer found a better match (note that the indexer can generate any kind of title it wants, not just the page <title>)
            if (StringUtils.isBlank(this.getTitle()) && !StringUtils.isBlank(indexEntry.getTitle())) {
                this.setTitle(indexEntry.getTitle());
            }
            this.setDescription(indexEntry.getDescription());
            this.setImage(indexEntry.getImage() == null ? null : indexEntry.getImage().toString());
        }
    }

    //-----STATIC METHODS-----
    public static PageIndexEntry fromLuceneDoc(Document document) throws IOException
    {
        return getProtobufMapper().readerFor(SimplePageIndexEntry.class).with(getProtobufSchema()).readValue(document.getBinaryValue(PageIndexEntry.Field.object.name()).bytes);
    }

    //-----PUBLIC METHODS-----
    //Note: never serialize this to the protobuf stored field
    @JsonIgnore
    public Document createLuceneDoc() throws IOException
    {
        Document retVal = new Document();

        //note: StringField = un-analyzed + indexed
        //      TextField = standard analyzed + indexed
        //      StoredField = not indexed at all

        //Note: we also need to insert the id of the doc even though it's an index
        retVal.add(new StringField(IndexEntry.Field.id.name(), this.getId(), org.apache.lucene.document.Field.Store.NO));

        //don't store it, we just add it to the index to be able to query the URI (again) more naturally
        retVal.add(new TextField(IndexEntry.Field.tokenisedId.name(), this.getId(), org.apache.lucene.document.Field.Store.NO));

        if (this.getResource() != null) {
            retVal.add(new StringField(PageIndexEntry.Field.resource.name(), this.getResource(), org.apache.lucene.document.Field.Store.NO));
        }
        if (this.getTypeOf() != null) {
            retVal.add(new StringField(PageIndexEntry.Field.typeOf.name(), this.getTypeOf(), org.apache.lucene.document.Field.Store.NO));
        }
        if (this.getTitle() != null) {
            retVal.add(new TextField(IndexEntry.Field.title.name(), this.getTitle(), org.apache.lucene.document.Field.Store.NO));
        }
        if (this.getLanguage() != null) {
            retVal.add(new StringField(PageIndexEntry.Field.language.name(), this.getLanguage(), org.apache.lucene.document.Field.Store.NO));
        }
        if (this.getCanonicalAddress() != null) {
            retVal.add(new StringField(PageIndexEntry.Field.canonicalAddress.name(), this.getCanonicalAddress(), org.apache.lucene.document.Field.Store.NO));
        }
        if (this.getDescription() != null) {
            retVal.add(new TextField(IndexEntry.Field.description.name(), this.getDescription(), org.apache.lucene.document.Field.Store.NO));
        }
        if (this.getImage() != null) {
            retVal.add(new StringField(IndexEntry.Field.image.name(), this.getImage(), org.apache.lucene.document.Field.Store.NO));
        }

        //stores the entire object in the index (using Protocol Buffers)
        //see https://github.com/FasterXML/jackson-dataformats-binary/tree/master/protobuf
        byte[] serializedObject = getProtobufMapper().writer(getProtobufSchema()).writeValueAsBytes(this);
        retVal.add(new StoredField(PageIndexEntry.Field.object.name(), serializedObject));
        //this is the old JSON-alternative
        //retVal.add(new StoredField(PageIndexEntry.Field.object.name(), Json.write(this)));

        return retVal;
    }
    @Override
    public String getResource()
    {
        return resource;
    }
    private void setResource(String resource)
    {
        this.resource = resource;
    }
    @Override
    public String getCanonicalAddress()
    {
        return canonicalAddress;
    }
    private void setCanonicalAddress(String canonicalAddress)
    {
        this.canonicalAddress = canonicalAddress;
    }
    @Override
    public String getTypeOf()
    {
        return typeOf;
    }
    private void setTypeOf(String typeOfCurie)
    {
        this.typeOf = typeOfCurie;
    }
    @Override
    public String getLanguage()
    {
        return language;
    }
    private void setLanguage(String language)
    {
        this.language = language;
    }

    //-----PROTECTED METHODS-----
    protected static ProtobufSchema getProtobufSchema() throws IOException
    {
        if (PROTOBUF_SCHEMA == null) {
            PROTOBUF_SCHEMA = ProtobufSchemaLoader.std.parse(createProtobufSchema(SimplePageIndexEntry.class));
        }

        return PROTOBUF_SCHEMA;
    }

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
