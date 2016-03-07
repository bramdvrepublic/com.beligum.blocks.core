package com.beligum.blocks.rdf.ontology.vocabularies;

import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ifaces.RdfVocabulary;
import com.beligum.blocks.rdf.ontology.RdfClassImpl;
import com.beligum.blocks.rdf.ontology.RdfPropertyImpl;
import gen.com.beligum.blocks.core.messages.blocks.ontology.Entries;

import java.net.URI;

/**
 * The OWL 2 Schema vocabulary (OWL 2)
 *
 * This ontology partially describes the built-in classes and
 * properties that together form the basis of the RDF/XML syntax of OWL 2.
 * The content of this ontology is based on Tables 6.1 and 6.2
 * in Section 6.4 of the OWL 2 RDF-Based Semantics specification,
 * available at http://www.w3.org/TR/owl2-rdf-based-semantics/.
 * Please note that those tables do not include the different annotations
 * (labels, comments and rdfs:isDefinedBy links) used in this file.
 * Also note that the descriptions provided in this ontology do not
 * provide a complete and correct formal description of either the syntax
 * or the semantics of the introduced terms (please see the OWL 2
 * recommendations for the complete and normative specifications).
 * Furthermore, the information provided by this ontology may be
 * misleading if not used with care. This ontology SHOULD NOT be imported
 * into OWL ontologies. Importing this file into an OWL 2 DL ontology
 * will cause it to become an OWL 2 Full ontology and may have other,
 * unexpected, consequences.
 *
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

    /**
     * The class of OWL classes
     */
    public static final RdfClass CLASS = new RdfClassImpl("Class", INSTANCE, Entries.OWL_title_Class, Entries.OWL_label_Class, null);

    /**
     * The class of OWL individuals
     * Note: this is actually an OwlClass (that subclasses rdfs:Class)
     */
    public static final RdfClass THING = new RdfClassImpl("Thing", INSTANCE, Entries.OWL_title_Thing, Entries.OWL_label_Thing, null);

    /**
     * The property that determines that two given individuals are equal
     */
    public static final RdfProperty SAMEAS = new RdfPropertyImpl("sameAs", INSTANCE, Entries.OWL_title_sameAs, Entries.OWL_label_sameAs, OWL.THING);
}
