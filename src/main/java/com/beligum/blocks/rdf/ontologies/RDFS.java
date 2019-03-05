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

import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfDatatype;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ifaces.RdfOntology;
import com.beligum.blocks.rdf.RdfClassImpl;
import com.beligum.blocks.rdf.RdfDatatypeImpl;
import com.beligum.blocks.rdf.RdfOntologyImpl;
import com.beligum.blocks.rdf.RdfPropertyImpl;
import gen.com.beligum.blocks.core.messages.blocks.ontology.Entries;

import java.net.URI;

/**
 * The RDF Schema vocabulary (RDFS)
 *
 * Created by bram on 3/2/16.
 */
public final class RDFS extends RdfOntologyImpl
{
    //-----SINGLETON-----
    public static final RdfOntology INSTANCE = new RDFS();

    private RDFS()
    {
        super(URI.create("http://www.w3.org/2000/01/rdf-schema#"), "rdfs");
    }

    public enum Entry {

    }

    //-----ENTRIES-----
    //note that many of the entries here are actually represented by the java Rdf* classes...

    /**
     * The class resource, everything
     */
    public static final RdfClass RESOURCE = new RdfClassImpl("Resource", INSTANCE, Entries.RDFS_title_Resource, Entries.RDFS_label_Resource);

    /**
     * The class of literal values, eg. textual strings and integers
     */
    public static final RdfDatatype LITERAL = new RdfDatatypeImpl("Literal", INSTANCE, Entries.RDFS_title_Literal, Entries.RDFS_label_Literal);

    /**
     * The class of classes
     */
    public static final RdfClass CLASS = new RdfClassImpl("Class", INSTANCE, Entries.RDFS_title_Class, Entries.RDFS_label_Class);

    /**
     * The subject is a subclass of a class
     */
    public static final RdfProperty SUBCLASSOF = new RdfPropertyImpl("subClassOf", INSTANCE, Entries.RDFS_title_subClassOf, Entries.RDFS_label_subClassOf, RDFS.CLASS);

    /**
     * The subject is a subproperty of a property
     */
    public static final RdfProperty SUBPROPERTYOF = new RdfPropertyImpl("subPropertyOf", INSTANCE, Entries.RDFS_title_subPropertyOf, Entries.RDFS_label_subPropertyOf, RDF.PROPERTY);

    /**
     * A domain of the subject property
     */
    public static final RdfProperty DOMAIN = new RdfPropertyImpl("domain", INSTANCE, Entries.RDFS_title_domain, Entries.RDFS_label_domain, RDFS.CLASS);

    /**
     * A range of the subject property
     */
    public static final RdfProperty RANGE = new RdfPropertyImpl("range", INSTANCE, Entries.RDFS_title_range, Entries.RDFS_label_range, RDFS.CLASS);

    /**
     * A description of the subject resource
     */
    public static final RdfProperty COMMENT = new RdfPropertyImpl("comment", INSTANCE, Entries.RDFS_title_comment, Entries.RDFS_label_comment, RDFS.LITERAL);

    /**
     * A human-readable name for the subject
     */
    public static final RdfProperty LABEL = new RdfPropertyImpl("label", INSTANCE, Entries.RDFS_title_label, Entries.RDFS_label_label, RDFS.LITERAL);

    /**
     * The class of RDF datatypes
     */
    public static final RdfClass DATATYPE = new RdfClassImpl("Datatype", INSTANCE, Entries.RDFS_title_Datatype, Entries.RDFS_label_Datatype);

    /**
     * The class of RDF containers
     */
    public static final RdfClass CONTAINER = new RdfClassImpl("Container", INSTANCE, Entries.RDFS_title_Container, Entries.RDFS_label_Container);

    /**
     * A member of the subject resource
     */
    public static final RdfProperty MEMBER = new RdfPropertyImpl("member", INSTANCE, Entries.RDFS_title_member, Entries.RDFS_label_member, RDFS.RESOURCE);

    /**
     * The defininition of the subject resource
     */
    public static final RdfProperty ISDEFINEDBY = new RdfPropertyImpl("isDefinedBy", INSTANCE, Entries.RDFS_title_isDefinedBy, Entries.RDFS_label_isDefinedBy, RDFS.RESOURCE);

    /**
     * Further information about the subject resource
     */
    public static final RdfProperty SEEALSO = new RdfPropertyImpl("seeAlso", INSTANCE, Entries.RDFS_title_seeAlso, Entries.RDFS_label_seeAlso, RDFS.RESOURCE);

    /**
     * The class of container membership properties, rdf:_1, rdf:_2, ..., all of which are sub-properties of 'member'
     */
    public static final RdfClass CONTAINERMEMBERSHIPPROPERTY = new RdfClassImpl("ContainerMembershipProperty", INSTANCE, Entries.RDFS_title_ContainerMembershipProperty, Entries.RDFS_label_ContainerMembershipProperty);
}
