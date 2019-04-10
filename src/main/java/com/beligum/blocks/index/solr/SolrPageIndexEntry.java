package com.beligum.blocks.index.solr;

import com.beligum.blocks.index.entries.JsonField;
import com.beligum.blocks.index.entries.JsonPageIndexEntry;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
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
    protected SolrPageIndexEntry(String id, URI absolutePublicPageUri, URI absoluteRootResourceUri, Locale language, Model rdfModel, JsonPageIndexEntry parent) throws IOException
    {
        super(id, absolutePublicPageUri, absoluteRootResourceUri, language, rdfModel, parent);
    }
    protected SolrPageIndexEntry(String json) throws IOException
    {
        super(json);
    }
    /**
     * This is the only public entry point all the rest are recursive calls
     */
    public SolrPageIndexEntry(Page page) throws IOException
    {
        super(page);
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----
    /**
     * Overridden so this method returns an instance of SolrPageIndexEntry instead of JsonPageIndexEntry
     * Note that all other create() method (in the superclass) forward their calls here so we don't need to override the others.
     */
    @Override
    protected JsonPageIndexEntry create(String id, URI absolutePublicPageUri, URI absoluteRootResourceUri, Locale language, Model rdfModel, JsonPageIndexEntry parent) throws IOException
    {
        return new SolrPageIndexEntry(id, absolutePublicPageUri, absoluteRootResourceUri, language, rdfModel, parent);
    }
    @Override
    protected JsonField createField(RdfProperty property) throws IOException
    {
        return new SolrField(property);
    }

    //-----PRIVATE METHODS-----
}
