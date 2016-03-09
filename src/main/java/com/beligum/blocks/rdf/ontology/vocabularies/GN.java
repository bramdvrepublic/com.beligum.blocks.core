package com.beligum.blocks.rdf.ontology.vocabularies;

import com.beligum.blocks.rdf.ifaces.RdfVocabulary;

import java.net.URI;

/**
 * Created by bram on 2/28/16.
 */
public final class GN extends AbstractRdfVocabulary
{
    //-----SINGLETON-----
    public static final RdfVocabulary INSTANCE = new GN();
    private GN()
    {
        super(URI.create("http://www.geonames.org/ontology#"), "gn");
    }

    //-----ENTRIES-----

}
