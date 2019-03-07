/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.beligum.blocks.rdf.ontologies;

import com.beligum.blocks.exceptions.RdfInitializationException;
import com.beligum.blocks.rdf.*;
import com.beligum.blocks.rdf.ifaces.*;
import gen.com.beligum.blocks.core.messages.blocks.ontology.Entries;

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
public final class RDF extends RdfOntologyImpl
{
    //-----CONSTANTS-----
    public static final RdfNamespace NAMESPACE = new RdfNamespaceImpl("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf");

    //-----MEMBERS-----
    /**
     * The subject is an instance of a class
     */
    public static final RdfProperty type = RdfFactory.newProxyProperty("type");

    /**
     * The class of RDF properties
     */
    public static final RdfClass Property = RdfFactory.newProxyClass("Property");

    /**
     * The datatype of XML literal values
     */
    public static final RdfDatatype XMLLiteral = RdfFactory.newProxyDatatype("XMLLiteral");

    /**
     * The subject of the subject RDF statement
     */
    public static final RdfProperty subject = RdfFactory.newProxyProperty("subject");

    /**
     * The predicate of the subject RDF statement
     */
    public static final RdfProperty predicate = RdfFactory.newProxyProperty("predicate");

    /**
     * The object of the subject RDF statement
     */
    public static final RdfProperty object = RdfFactory.newProxyProperty("object");

    /**
     * The class of RDF statements
     */
    public static final RdfClass Statement = RdfFactory.newProxyClass("Statement");

    /**
     * The class of unordered containers
     */
    public static final RdfClass Bag = RdfFactory.newProxyClass("Bag");

    /**
     * The class of containers of alternatives
     */
    public static final RdfClass Alt = RdfFactory.newProxyClass("Alt");

    /**
     * The class of ordered containers
     */
    public static final RdfClass Seq = RdfFactory.newProxyClass("Seq");

    /**
     * Idiomatic property used for structured values
     */
    public static final RdfProperty value = RdfFactory.newProxyProperty("value");

    /**
     * weird: this type doesn't seem to occur in https://www.w3.org/1999/02/22-rdf-syntax-ns# ??
     * See eg. here for more info: https://www.w3.org/TR/rdf-syntax-grammar/#section-Syntax-list-elements
     */
    public static final RdfProperty li = RdfFactory.newProxyProperty("li");

    /**
     * The class of RDF Lists
     */
    public static final RdfClass List = RdfFactory.newProxyClass("List");

    /**
     * The first item in the subject RDF list
     */
    public static final RdfProperty first = RdfFactory.newProxyProperty("first");

    /**
     * The rest of the subject RDF list after the first item
     */
    public static final RdfProperty rest = RdfFactory.newProxyProperty("rest");

    /**
     * The empty list, with no items in it. If the rest of a list is nil then the list has no more items in it.
     * !!NOTE!! the datatype is actually an RDF List, not a Class, but that's not modeled in this class hierarchy yet...
     */
    public static final RdfClass nil = RdfFactory.newProxyClass("nil");

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
     *    it's parsed as a no-language value (yielding a triple with a string as object without any language suffix, but with an explicit data type)
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
     * - Don't set the datatype explicitly if the required datatype is xsd:string or rdf:langString and you need a language;
     *   it seems to override the value of the @lang attribute.
     * - Don't count on the general html @lang language to be inherited. Make sure you set it specifically if you need it.
     *
     * This doesn't mean we cannot specifically build our models choosing between one of both; if you want the string to be translatable,
     * set the @lang attribute and omit the datatype. This will result in an implicit rdf:langString datatype.
     * If the value is explicitly language-agnostic, explicitly set xsd:string; this will 'override' any language (explicit or implicit).
     * We'll take care of this difference eg. in the <blocks-fact> client-side parsing.
     * Note that in doubt between these two string datatypes, choose the rdf:langString, because it offers the same (eg. SPARQL) functionality as
     * xsd:string, but has the added bonus to know in what language it was written afterwards. Only explicitly choose xsd:string if you have a good
     * reason for it (eg. for identifier strings, email addresses, etc). But be aware that an rdf:langString *always* needs a language set!
     *
     * ##########################
     *
     */
    public static final RdfDatatype langString = RdfFactory.newProxyDatatype("langString");

    /**
     * The datatype of RDF literals storing fragments of HTML content
     */
    public static final RdfDatatype HTML = RdfFactory.newProxyDatatype("HTML");

    //-----CONSTRUCTORS-----
    @Override
    protected void create(RdfFactory rdfFactory) throws RdfInitializationException
    {
        rdfFactory.register(type)
                  .label(Entries.RDF_label_type)
                  .dataType(RDFS.Class);

        rdfFactory.register(Property)
                  .label(Entries.RDF_label_Property);

        rdfFactory.register(XMLLiteral)
                  .label(Entries.RDF_label_XMLLiteral);

        rdfFactory.register(subject)
                  .label(Entries.RDF_label_subject)
                  .dataType(RDFS.Resource);

        rdfFactory.register(predicate)
                  .label(Entries.RDF_label_predicate)
                  .dataType(RDFS.Resource);

        rdfFactory.register(object)
                  .label(Entries.RDF_label_object)
                  .dataType(RDFS.Resource);

        rdfFactory.register(Statement)
                  .label(Entries.RDF_label_Statement);

        rdfFactory.register(Bag)
                  .label(Entries.RDF_label_Bag);

        rdfFactory.register(Alt)
                  .label(Entries.RDF_label_Alt);

        rdfFactory.register(Seq)
                  .label(Entries.RDF_label_Seq);

        rdfFactory.register(value)
                  .label(Entries.RDF_label_value)
                  .dataType(RDFS.Resource);

        rdfFactory.register(li)
                  .label(Entries.RDF_label_li)
                  .dataType(RDFS.Resource);

        rdfFactory.register(List)
                  .label(Entries.RDF_label_List);

        rdfFactory.register(first)
                  .label(Entries.RDF_label_first)
                  .dataType(RDFS.Resource);

        rdfFactory.register(rest)
                  .label(Entries.RDF_label_rest)
                  .dataType(List);

        rdfFactory.register(nil)
                  .label(Entries.RDF_label_nil);

        rdfFactory.register(langString)
                  .label(Entries.RDF_label_langString);

        rdfFactory.register(HTML)
                  .label(Entries.RDF_label_HTML);
    }

    //-----PUBLIC METHODS-----
    @Override
    public RdfNamespace getNamespace()
    {
        return NAMESPACE;
    }
    @Override
    public boolean isPublic()
    {
        return false;
    }
}
