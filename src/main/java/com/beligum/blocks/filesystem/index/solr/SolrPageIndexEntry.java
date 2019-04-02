package com.beligum.blocks.filesystem.index.solr;

import com.beligum.blocks.filesystem.index.entries.JsonPageIndexEntry;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import org.eclipse.rdf4j.model.Model;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;

public class SolrPageIndexEntry extends JsonPageIndexEntry
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    /**
     * Private constructor: only for serialization
     */
    protected SolrPageIndexEntry()
    {
        super();
    }
    /**
     * This is the entry point of the create() function below
     */
    protected SolrPageIndexEntry(String id, URI absolutePublicPageUri, URI absoluteRootResourceUri, URI canonicalAddress, Locale language, Model rdfModel, JsonPageIndexEntry parent) throws IOException
    {
        super(id, absolutePublicPageUri, absoluteRootResourceUri, canonicalAddress, language, rdfModel, parent);
    }
    /**
     * This is the only public entry point all the rest are recursive calls
     */
    public SolrPageIndexEntry(Page page) throws IOException
    {
        super(page);
    }
    protected SolrPageIndexEntry(String json) throws IOException
    {
        super(json);
    }

    //-----PUBLIC METHODS-----
    /**
     * Overridden so this method returns an instance of SolrPageIndexEntry instead of JsonPageIndexEntry
     * Note that all other create() method (in the superclass) forward their calls here so we don't need to override the others.
     */
    @Override
    public JsonPageIndexEntry create(String id, URI absolutePublicPageUri, URI absoluteRootResourceUri, URI canonicalAddress, Locale language, Model rdfModel, JsonPageIndexEntry parent) throws IOException
    {
        return new SolrPageIndexEntry(id, absolutePublicPageUri, absoluteRootResourceUri, canonicalAddress, language, rdfModel, parent);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
