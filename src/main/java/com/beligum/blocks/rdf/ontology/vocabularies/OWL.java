package com.beligum.blocks.rdf.ontology.vocabularies;

import com.beligum.blocks.rdf.ifaces.RdfVocabulary;

import java.net.URI;

/**
 * Created by bram on 2/28/16.
 */
public final class OWL extends AbstractRdfVocabulary
{
    //-----SINGLETON-----
    public static final RdfVocabulary INSTANCE = new OWL();
    private OWL()
    {
        super(URI.create("http://www.w3.org/2002/07/owl#"), "owl");
    }

    //-----ENTRIES-----

}
