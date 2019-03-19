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
import com.beligum.blocks.rdf.RdfFactory;
import com.beligum.blocks.rdf.RdfNamespaceImpl;
import com.beligum.blocks.rdf.RdfOntologyImpl;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfDatatype;
import com.beligum.blocks.rdf.ifaces.RdfNamespace;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import gen.com.beligum.blocks.core.messages.blocks.ontology.Entries;

/**
 * RDFa Vocabulary for Term and Prefix Assignment, and for Processor Graph Reporting
 * <p>
 * This document describes the RDFa Vocabulary for Term and Prefix Assignment. The Vocabulary is used to modify RDFa 1.1 processing behavior.
 */
public final class RDFA extends RdfOntologyImpl
{
    //-----CONSTANTS-----
    public static final RdfNamespace NAMESPACE = new RdfNamespaceImpl("http://www.w3.org/ns/rdfa#", "rdfa");

    //-----MEMBERS-----
    /**
     * Is the top level class of the hierarchy
     */
    public static final RdfClass PGClass = RdfFactory.newProxyClass("PGClass");

    /**
     * Class to identify an (RDF) resource whose properties are to be copied to another resource
     */
    public static final RdfClass Pattern = RdfFactory.newProxyClass("Pattern");

    /**
     * Is the top level class for prefix or term mappings
     */
    public static final RdfClass PrefixOrTermMapping = RdfFactory.newProxyClass("PrefixOrTermMapping");

    /**
     * Error condition; to be used when the document fails to be fully processed as a result of non-conformant host language markup
     */
    public static final RdfClass DocumentError = RdfFactory.newProxyClass("DocumentError");

    /**
     * Is the class for all informations
     */
    public static final RdfClass Info = RdfFactory.newProxyClass("Info");

    /**
     * Warning; to be used when a prefix, either from the initial context or inherited from an ancestor node, is redefined in an element
     */
    public static final RdfClass PrefixRedefinition = RdfFactory.newProxyClass("PrefixRedefinition");

    /**
     * Warning; to be used when a CURIE prefix fails to be resolved
     */
    public static final RdfClass UnresolvedCURIE = RdfFactory.newProxyClass("UnresolvedCURIE");

    /**
     * Warning; to be used when a Term fails to be resolved
     */
    public static final RdfClass UnresolvedTerm = RdfFactory.newProxyClass("UnresolvedTerm");

    /**
     * Warning; to be used when the value of a @vocab attribute cannot be dereferenced, hence the vocabulary expansion cannot be completed
     */
    public static final RdfClass VocabReferenceError = RdfFactory.newProxyClass("VocabReferenceError");

    /**
     * Is the class for all error conditions
     */
    public static final RdfClass Error = RdfFactory.newProxyClass("Error");

    /**
     * Is the class for prefix mappings
     */
    public static final RdfClass PrefixMapping = RdfFactory.newProxyClass("PrefixMapping");

    /**
     * Is the class for term mappings
     */
    public static final RdfClass TermMapping = RdfFactory.newProxyClass("TermMapping");

    /**
     * Is the class for all warnings
     */
    public static final RdfClass Warning = RdfFactory.newProxyClass("Warning");

    /**
     * Provides extra context for the error, eg, http response, an XPointer/XPath information, or simply the URI that created the error
     */
    public static final RdfProperty context = RdfFactory.newProxyProperty("context");

    /**
     * Identifies the resource (i.e., pattern) whose properties and values should be copied to replace the current triple (retaining the subject of the triple).
     */
    public static final RdfProperty copy = RdfFactory.newProxyProperty("copy");

    /**
     * Defines a prefix mapping for a URI; the value is supposed to be a NMTOKEN
     */
    public static final RdfProperty prefix = RdfFactory.newProxyProperty("prefix");

    /**
     * Defines a term mapping for a URI; the value is supposed to be a NMTOKEN
     */
    public static final RdfProperty term = RdfFactory.newProxyProperty("term");

    /**
     * Defines the URI for either a prefix or a term mapping; the value is supposed to be an absolute URI
     */
    public static final RdfProperty uri = RdfFactory.newProxyProperty("uri");

    /**
     * Provides a relationship between the host document and a vocabulary defined using the @vocab facility of RDFa1.1
     */
    public static final RdfProperty usesVocabulary = RdfFactory.newProxyProperty("usesVocabulary");

    /**
     * Defines an absolute URI to be used as a default vocabulary; the value is can be any string; for documentation purposes it is advised to use the string 'true' or 'True'.
     */
    public static final RdfProperty vocabulary = RdfFactory.newProxyProperty("vocabulary");

    //-----CONSTRUCTORS-----
    @Override
    protected void create(RdfFactory rdfFactory) throws RdfInitializationException
    {
        rdfFactory.register(PGClass);
        rdfFactory.register(Pattern);
        rdfFactory.register(PrefixOrTermMapping);
        rdfFactory.register(DocumentError);
        rdfFactory.register(Info);
        rdfFactory.register(PrefixRedefinition);
        rdfFactory.register(UnresolvedCURIE);
        rdfFactory.register(UnresolvedTerm);
        rdfFactory.register(VocabReferenceError);
        rdfFactory.register(Error);
        rdfFactory.register(PrefixMapping);
        rdfFactory.register(TermMapping);
        rdfFactory.register(Warning);
        rdfFactory.register(context);
        rdfFactory.register(copy);
        rdfFactory.register(prefix);
        rdfFactory.register(term);
        rdfFactory.register(uri);
        rdfFactory.register(usesVocabulary);
        rdfFactory.register(vocabulary);
    }

    //-----PUBLIC METHODS-----
    @Override
    public RdfNamespace getNamespace()
    {
        return NAMESPACE;
    }
    @Override
    protected boolean isPublicOntology()
    {
        return false;
    }
}
