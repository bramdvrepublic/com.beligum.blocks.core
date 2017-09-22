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

package com.beligum.blocks.rdf.ontology.vocabularies;

import com.beligum.blocks.rdf.ifaces.RdfVocabulary;

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
public final class VCARD extends AbstractRdfVocabulary
{
    //-----SINGLETON-----
    public static final RdfVocabulary INSTANCE = new VCARD();
    private VCARD()
    {
        super(URI.create("http://www.w3.org/2006/vcard/ns#"), "vcard");
    }

    //-----ENTRIES-----

}
