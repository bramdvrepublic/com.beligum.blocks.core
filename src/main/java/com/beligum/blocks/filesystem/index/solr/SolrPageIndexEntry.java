package com.beligum.blocks.filesystem.index.solr;

import com.beligum.blocks.filesystem.index.entries.pages.AbstractPageIndexEntry;
import com.beligum.blocks.filesystem.index.entries.pages.JsonPageIndexEntry;
import com.beligum.blocks.filesystem.index.ifaces.IndexEntry;
import com.beligum.blocks.filesystem.index.ifaces.IndexEntryField;
import com.beligum.blocks.filesystem.index.ifaces.PageIndexEntry;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import org.eclipse.rdf4j.model.Model;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
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
    protected SolrPageIndexEntry(URI id, URI absolutePublicPageUri, URI absoluteRootResourceUri, URI canonicalAddress, Locale language, Model rdfModel, JsonPageIndexEntry parent) throws IOException
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
    public JsonPageIndexEntry create(URI id, URI absolutePublicPageUri, URI absoluteRootResourceUri, URI canonicalAddress, Locale language, Model rdfModel, JsonPageIndexEntry parent) throws IOException
    {
        return new SolrPageIndexEntry(id, absolutePublicPageUri, absoluteRootResourceUri, canonicalAddress, language, rdfModel, parent);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
