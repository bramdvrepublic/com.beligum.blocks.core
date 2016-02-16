package com.beligum.blocks.fs.indexes.stubs;

import com.beligum.blocks.fs.pages.ifaces.Page;
import org.hibernate.search.annotations.Indexed;

import java.io.IOException;

/**
 * Created by bram on 2/13/16.
 */
@Indexed
public class PageStub extends AbstractStub
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
//    @Field(index = Index.YES, analyze = Analyze.YES, store = Store.YES)
//    private final String title;
//    @Field(index = Index.YES, analyze = Analyze.YES, store = Store.YES)
//    private final String language;
//    @Field(index = Index.YES, analyze = Analyze.YES, store = Store.YES)
//    private final URI parent;
//    @Field(index = Index.YES, analyze = Analyze.YES, store = Store.YES)
//    private final Map<String, URI> translations;

    //-----CONSTRUCTORS-----
    public PageStub() throws IOException
    {
        this(null);
    }
    public PageStub(Page page) throws IOException
    {
        //the ID of th stub in the public URI
        super(page.getPathInfo().getUri());

//        if (page.getSource()==null) {
//            throw new IOException("We can't create a page indexer stub without an attached source; this code should only be called during saving of the page; "+page);
//        }
//
//        this.title = page.getSource().getTitle();
//        this.language = page.getSource().getHtmlLocale().getLanguage();
//        this.parent = null;
//        this.translations = new LinkedHashMap<>();
//        if (page.getSource().getTranslations()!=null) {
//            for (Map.Entry<URI, Locale> e : page.getSource().getTranslations().entrySet()) {
//                this.translations.put(e.getValue().getLanguage(), e.getKey());
//            }
//        }
    }

    //-----PUBLIC METHODS-----


    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----
    @Override
    public String toString()
    {
        return "PageStub{" +
               "id='" + getId() + '\'' +
               '}';
    }
}
