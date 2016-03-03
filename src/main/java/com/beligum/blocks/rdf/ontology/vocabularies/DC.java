package com.beligum.blocks.rdf.ontology.vocabularies;

import com.beligum.blocks.rdf.ifaces.RdfVocabulary;

import java.net.URI;

/**
 * Created by bram on 2/28/16.
 */
public final class DC extends AbstractRdfVocabulary
{
    //-----SINGLETON-----
    public static final RdfVocabulary INSTANCE = new DC();
    private DC()
    {
        super(URI.create("http://purl.org/dc/terms/"), "dc");
    }

    //-----ENTRIES-----

}
