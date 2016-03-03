package com.beligum.blocks.rdf.ontology.vocabularies;

import com.beligum.blocks.rdf.ifaces.RdfVocabulary;

import java.net.URI;

/**
 * Created by bram on 2/28/16.
 */
public final class DBO extends AbstractRdfVocabulary
{
    //-----SINGLETON-----
    public static final RdfVocabulary INSTANCE = new DBO();
    private DBO()
    {
        super(URI.create("http://dbpedia.org/ontology/"), "dbo");
    }

    //-----ENTRIES-----

}
