package com.beligum.blocks.rdf.ontology.vocabularies;

import com.beligum.blocks.rdf.ifaces.RdfVocabulary;

import java.net.URI;

/**
 * Friend of a Friend (FOAF) vocabulary
 *
 * The Friend of a Friend (FOAF) RDF vocabulary, described using W3C RDF Schema and the Web Ontology Language.
 *
 * Created by bram on 2/28/16.
 */
public final class FOAF extends AbstractRdfVocabulary
{
    //-----SINGLETON-----
    public static final RdfVocabulary INSTANCE = new FOAF();
    private FOAF()
    {
        super(URI.create("http://xmlns.com/foaf/0.1/"), "foaf");
    }

    //-----ENTRIES-----

}
