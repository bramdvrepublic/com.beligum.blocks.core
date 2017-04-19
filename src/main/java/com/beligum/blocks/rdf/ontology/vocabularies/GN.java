package com.beligum.blocks.rdf.ontology.vocabularies;

import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ifaces.RdfVocabulary;
import com.beligum.blocks.rdf.ontology.RdfClassImpl;
import com.beligum.blocks.rdf.ontology.RdfPropertyImpl;
import gen.com.beligum.blocks.core.messages.blocks.ontology;

import java.net.URI;

/**
 * Created by bram on 2/28/16.
 */
public final class GN extends AbstractRdfVocabulary
{
    //-----VARIABLES-----

    //-----SINGLETON-----
    public static final RdfVocabulary INSTANCE = new GN();
    private GN()
    {
        super(URI.create("http://www.geonames.org/ontology#"), "gn");
    }

    //-----PUBLIC FUNCTIONS-----

    //-----ENTRIES-----
    /**
     * The class of OWL individuals
     * Note: this is actually an OwlClass (that subclasses rdfs:Class)
     */
    public static final RdfClass Feature = new RdfClassImpl("Feature", INSTANCE, ontology.Entries.GN_title_Feature, ontology.Entries.GN_label_Feature, null);

    public static final RdfProperty name = new RdfPropertyImpl("name", INSTANCE, ontology.Entries.GN_title_name, ontology.Entries.GN_label_name, XSD.STRING);

    public static final RdfProperty officialName = new RdfPropertyImpl("officialName", INSTANCE, ontology.Entries.GN_title_officialName, ontology.Entries.GN_label_officialName, XSD.STRING);

    public static final RdfProperty alternateName = new RdfPropertyImpl("alternateName", INSTANCE, ontology.Entries.GN_title_alternateName, ontology.Entries.GN_label_alternateName, XSD.STRING);

}
