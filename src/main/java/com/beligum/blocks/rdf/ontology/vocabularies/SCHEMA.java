package com.beligum.blocks.rdf.ontology.vocabularies;

import com.beligum.blocks.rdf.ifaces.RdfVocabulary;

import java.net.URI;

/**
 * Created by bram on 2/28/16.
 */
public final class SCHEMA extends AbstractRdfVocabulary
{
    //-----VARIABLES-----

    //-----SINGLETON-----
    public static final RdfVocabulary INSTANCE = new SCHEMA();
    private SCHEMA()
    {
        super(URI.create("http://schema.org/"), "schema");
    }

    //-----PUBLIC FUNCTIONS-----

    //-----ENTRIES-----
}
