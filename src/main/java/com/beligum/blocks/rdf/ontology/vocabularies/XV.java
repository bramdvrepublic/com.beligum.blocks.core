package com.beligum.blocks.rdf.ontology.vocabularies;

import com.beligum.blocks.rdf.ifaces.RdfVocabulary;

import java.net.URI;

/**
 * Extended VCard Vocabulary
 *
 * The Extended VCard Vocabulary defines a set of classes and properties to describe
 * with enough granularity concepts related to the vcard:Address class like Street, neighborhood, district, etc.
 *
 * Created by bram on 2/28/16.
 */
public final class XV extends AbstractRdfVocabulary
{
    //-----SINGLETON-----
    public static final RdfVocabulary INSTANCE = new XV();
    private XV()
    {
        //Note: don't know about the prefix; didn't find many public uses...
        super(URI.create("http://www.bcn.cat/data/v8y/xvcard/#"), "xv");
    }

    //-----ENTRIES-----

}
