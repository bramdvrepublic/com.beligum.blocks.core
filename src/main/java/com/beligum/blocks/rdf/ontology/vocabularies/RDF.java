package com.beligum.blocks.rdf.ontology.vocabularies;

import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfDataType;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ifaces.RdfVocabulary;
import com.beligum.blocks.rdf.ontology.RdfClassImpl;
import com.beligum.blocks.rdf.ontology.RdfDataTypeImpl;
import com.beligum.blocks.rdf.ontology.RdfPropertyImpl;
import gen.com.beligum.blocks.core.messages.blocks.ontology.Entries;

import java.net.URI;

/**
 * The RDF Concepts Vocabulary (RDF)
 *
 * This is the RDF Schema for the RDF vocabulary terms in the RDF Namespace, defined in RDF 1.1 Concepts.
 * ---------------------------------------------------------------------
 * Ontology as described in https://www.w3.org/1999/02/22-rdf-syntax-ns
 * <p/>
 * See http://rdf4j.org/doc/4/apidocs/index.html?org/openrdf/model/vocabulary/RDF.html
 * <p/>
 * And in general:
 * https://bitbucket.org/openrdf/sesame/src/d7606e88b0913a21ffc39c671b41844e79371d39/core/model/src/main/java/org/openrdf/model/vocabulary/?at=master
 * <p/>
 * Created by bram on 3/2/16.
 */
public final class RDF extends AbstractRdfVocabulary
{
    //-----SINGLETON-----
    public static final RdfVocabulary INSTANCE = new RDF();
    private RDF()
    {
        super(URI.create("http://www.w3.org/1999/02/22-rdf-syntax-ns#"), "rdf");
    }

    //-----ENTRIES-----
    /**
     * The subject is an instance of a class
     */
    public static final RdfProperty TYPE = new RdfPropertyImpl("type", INSTANCE, Entries.RDF_title_type, Entries.RDF_label_type, RDFS.CLASS);

    /**
     * The class of RDF properties
     */
    public static final RdfClass PROPERTY = new RdfClassImpl("Property", INSTANCE, Entries.RDF_title_Property, Entries.RDF_label_Property, null);

    /**
     * The datatype of XML literal values
     */
    public static final RdfDataType XMLLITERAL = new RdfDataTypeImpl("XMLLiteral", INSTANCE, Entries.RDF_title_XMLLiteral, Entries.RDF_label_XMLLiteral, null);

    /**
     * The subject of the subject RDF statement
     */
    public static final RdfProperty SUBJECT = new RdfPropertyImpl("subject", INSTANCE, Entries.RDF_title_subject, Entries.RDF_label_subject, RDFS.RESOURCE);

    /**
     * The predicate of the subject RDF statement
     */
    public static final RdfProperty PREDICATE = new RdfPropertyImpl("predicate", INSTANCE, Entries.RDF_title_predicate, Entries.RDF_label_predicate, RDFS.RESOURCE);

    /**
     * The object of the subject RDF statement
     */
    public static final RdfProperty OBJECT = new RdfPropertyImpl("object", INSTANCE, Entries.RDF_title_object, Entries.RDF_label_object, RDFS.RESOURCE);

    /**
     * The class of RDF statements
     */
    public static final RdfClass STATEMENT = new RdfClassImpl("Statement", INSTANCE, Entries.RDF_title_Statement, Entries.RDF_label_Statement, null);

    /**
     * The class of unordered containers
     */
    public static final RdfClass BAG = new RdfClassImpl("Bag", INSTANCE, Entries.RDF_title_Bag, Entries.RDF_label_Bag, null);

    /**
     * The class of containers of alternatives
     */
    public static final RdfClass ALT = new RdfClassImpl("Alt", INSTANCE, Entries.RDF_title_Alt, Entries.RDF_label_Alt, null);

    /**
     * The class of ordered containers
     */
    public static final RdfClass SEQ = new RdfClassImpl("Seq", INSTANCE, Entries.RDF_title_Seq, Entries.RDF_label_Seq, null);

    /**
     * Idiomatic property used for structured values
     */
    public static final RdfProperty VALUE = new RdfPropertyImpl("value", INSTANCE, Entries.RDF_title_value, Entries.RDF_label_value, RDFS.RESOURCE);

    /**
     * weird: this type doesn't seem to occur in https://www.w3.org/1999/02/22-rdf-syntax-ns# ??
     * See eg. here for more info: https://www.w3.org/TR/rdf-syntax-grammar/#section-Syntax-list-elements
     */
    public static final RdfProperty LI = new RdfPropertyImpl("li", INSTANCE, Entries.RDF_title_li, Entries.RDF_label_li, RDFS.RESOURCE);

    /**
     * The class of RDF Lists
     */
    public static final RdfClass LIST = new RdfClassImpl("List", INSTANCE, Entries.RDF_title_List, Entries.RDF_label_List, null);

    /**
     * The first item in the subject RDF list
     */
    public static final RdfProperty FIRST = new RdfPropertyImpl("first", INSTANCE, Entries.RDF_title_first, Entries.RDF_label_first, RDFS.RESOURCE);

    /**
     * The rest of the subject RDF list after the first item
     */
    public static final RdfProperty REST = new RdfPropertyImpl("rest", INSTANCE, Entries.RDF_title_rest, Entries.RDF_label_rest, RDF.LIST);

    /**
     * The empty list, with no items in it. If the rest of a list is nil then the list has no more items in it.
     * !!NOTE!! the datatype is actually an RDF List, not a Class, but that's not modeled in this class hierarchy yet...
     */
    public static final RdfClass NIL = new RdfClassImpl("nil", INSTANCE, Entries.RDF_title_nil, Entries.RDF_label_nil, null);

    //see https://github.com/RubenVerborgh/N3.js/issues/15:
    // "rdf:PlainLiteral was the datatype suggested (and standardised) by the OWL community before the RDF community created RDF-1.1:"
    //so I assume it's deprecated in v1.1 now? (also not in Sesame API)
    //Note: good explanation of this situation: http://stackoverflow.com/a/20472902
    // To sum up:
    // - What used to be plain literals will be have the same lexical form, and will have datatype xsd:string
    // - A language tagged string will have the datatype http://www.w3.org/1999/02/22-rdf-syntax-ns#langString
    // Also see https://www.w3.org/TR/rdf11-concepts/#section-Graph-Literal
    // --> "Please note that concrete syntaxes may support simple literals consisting of only a lexical form without any datatype IRI or language tag.
    //      Simple literals are syntactic sugar for abstract syntax literals with the datatype IRI http://www.w3.org/2001/XMLSchema#string."
    // Interesting read: https://www.w3.org/TR/rdf11-concepts/#xsd-datatypes
    // Also:
    // - http://answers.semanticweb.com/questions/19637/for-language-tag-should-i-use-xsd-string-or-plainliteral
    // - http://mail-archives.apache.org/mod_mbox/jena-dev/201310.mbox/%3C525A8428.1050207@apache.org%3E
    //public static final RdfClass PLAINLITERAL= new RdfClassImpl("PlainLiteral", INSTANCE, Entries.RDF_title_PlainLiteral, Entries.RDF_title_PlainLiteral, null);

    /**
     * The datatype of language-tagged string values
     *
     * A little bit more information after the string-language  'bug' discovered in 05/17:
     *
     * In RDFa parsing:
     *
     * Note: these were tested with https://www.w3.org/2012/pyRdfa/Overview.html#distill_by_input
     *       see notes below for details on / differences with our own Semargl parser
     *
     * 1) if a property doesn't have an explicit @datatype and it doesn't have an explicit @lang attribute,
     *    it inherits the general @lang attribute from the umbrella html/body tag and implicitly gets the xsd:string datatype
     *
     * 2) if a property doesn't have an explicit @datatype and it has an explicit @lang attribute,
     *    it's parsed as a language-suffixed string object and implicitly gets the rdf:langString datatype
     *
     * -- as a general remark for conclusions below: if the property has an explicit datatype set, the language seems to be ignored --
     *
     * 3) if a property is explicitly tagged with @datatype "xsd:string" and it doesn't have an explicit @lang attribute,
     *    it's parsed as a no-language value (yielding a triple with a string as object without any language suffix)
     *
     * 4) if a property is explicitly tagged with @datatype "xsd:string" and it has an explicit @lang attribute,
     *    it's still parsed as a no-language value
     *
     * 5) if a property is explicitly tagged with @datatype "rdf:langString" and it doesn't have an explicit @lang attribute,
     *    it's parsed as a no-language value
     *
     * 6) if a property is explicitly tagged with @datatype "rdf:langString" and it has an explicit @lang attribute,
     *    it's parsed as a no-language value
     *
     * => we concluded this rule of thumb from this behaviour: if you want to map a string-value that doesn't require (and will never have!) a specific language attached to it (eg. a "constant" string),
     *    map it to the xsd:string type. If it conceptually should have a language attached, map it to the rdf:langString and make sure there's some client-logic that will (possibly invisibly) inherit
     *    the general tag to the property, optionally allowing the user to select the proper language if that should ever deviate from the general language of that page.
     *    Note that this doesn't reflect the behavior of 1) and 2) above, but allows us to explicitly distinguish between the two and solve the results in the UI
     *
     * => note that this is only the case for properties with an explicit @datatype attribute (eg. like in <blocks-fact>), for general-purpose properties that don't have an explicit @datatype set,
     *    it doesn't really matter, since 1) and 2) result in the "same" behavior, but if they're translatable, they (stricto senso) should have rdf:langString as their datatype, not xsd:string.
     *
     * => this is more or less synchronous to the fact a rdf:HTML-datatyped string should also have an explicit @lang attribute set to annotate the triple with a language suffix;
     *    eg. see https://www.w3.org/TR/rdf11-concepts/#section-html
     *    However, the results vary; for the default Distiller/Parser, the @lang attribute is inherited to the first HTML tag inside that container tag.
     *    If there's no such inner tag, the result is a no-language string with the rdf:HTML datatype
     *
     * -- Comments for our RDFa parser (Semargl) --
     * Results are not them same as the "official" Distiller/Parser:
     * - if a rdf:langString datatype is explicitly set, the lang attribute (set or not set), is not picked up, resulting in an error: "datatype rdf:langString requires a language tag".
     *   This is because one of the two is passed to the context: the datatype or the language (where the datatype has precedence)
     * - if a xsd:string datatype is explicitly set, the @lang is just ignored, whether it's set or not
     *
     * ### General conclusion ###
     *
     * Don't set the datatype explicitly if the required datatype is xsd:string or rdf:langString; the absence of a specific @lang attribute will make the datatype result in either of both.
     * Don't count on the general html @lang language to be inherited though, make sure you set it specifically if you need it.
     * This doesn't mean we cannot specifically build our models choosing between one of both; if you want the string to be translatable, use rdf:langString,
     * if the value is language-agnostic, use xsd:string. We'll take care of this difference eg. in the <blocks-fact> client-side parsing.
     *
     * ##########################
     *
     */
    public static final RdfDataType LANGSTRING = new RdfDataTypeImpl("langString", INSTANCE, Entries.RDF_title_langString, Entries.RDF_label_langString, null);

    /**
     * The datatype of RDF literals storing fragments of HTML content
     */
    public static final RdfDataType HTML = new RdfDataTypeImpl("HTML", INSTANCE, Entries.RDF_title_HTML, Entries.RDF_label_HTML, null);

}
