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

import java.net.URI;

/**
 * The RDF Schema vocabulary (RDFS)
 *
 * Created by bram on 3/2/16.
 */
public final class RDFS extends RdfOntologyImpl
{
    //-----CONSTANTS-----
    public static final RdfNamespace NAMESPACE = new RdfNamespaceImpl("http://www.w3.org/2000/01/rdf-schema#", "rdfs");

    //-----MEMBERS-----
    //note that many of the entries here are actually represented by the java Rdf* classes...

    /**
     * The class resource, everything
     */
    public static final RdfClass Resource = RdfFactory.newProxyClass("Resource");

    /**
     * The class of literal values, eg. textual strings and integers
     */
    public static final RdfDatatype Literal = RdfFactory.newProxyDatatype("Literal");

    /**
     * The class of classes
     */
    public static final RdfClass Class = RdfFactory.newProxyClass("Class");

    /**
     * The subject is a subclass of a class
     */
    public static final RdfProperty subClassOf = RdfFactory.newProxyProperty("subClassOf");

    /**
     * The subject is a subproperty of a property
     */
    public static final RdfProperty subPropertyOf = RdfFactory.newProxyProperty("subPropertyOf");

    /**
     * A domain of the subject property
     */
    public static final RdfProperty domain = RdfFactory.newProxyProperty("domain");

    /**
     * A range of the subject property
     */
    public static final RdfProperty range = RdfFactory.newProxyProperty("range");

    /**
     * A description of the subject resource
     */
    public static final RdfProperty comment = RdfFactory.newProxyProperty("comment");

    /**
     * A human-readable name for the subject
     */
    public static final RdfProperty label = RdfFactory.newProxyProperty("label");

    /**
     * The class of RDF datatypes
     */
    public static final RdfClass Datatype = RdfFactory.newProxyClass("Datatype");

    /**
     * The class of RDF containers
     */
    public static final RdfClass Container = RdfFactory.newProxyClass("Container");

    /**
     * A member of the subject resource
     */
    public static final RdfProperty member = RdfFactory.newProxyProperty("member");

    /**
     * The defininition of the subject resource
     */
    public static final RdfProperty isDefinedBy = RdfFactory.newProxyProperty("isDefinedBy");

    /**
     * Further information about the subject resource
     */
    public static final RdfProperty seeAlso = RdfFactory.newProxyProperty("seeAlso");

    /**
     * The class of container membership properties, rdf:_1, rdf:_2, ..., all of which are sub-properties of 'member'
     */
    public static final RdfClass ContainerMembershipProperty = RdfFactory.newProxyClass("ContainerMembershipProperty");

    //-----CONSTRUCTORS-----
    @Override
    protected void create(RdfFactory rdfFactory) throws RdfInitializationException
    {
        rdfFactory.register(Resource)
                  .label(Entries.RDFS_label_Resource);

        rdfFactory.register(Literal)
                  .label(Entries.RDFS_label_Literal);

        rdfFactory.register(Class)
                  .label(Entries.RDFS_label_Class);

        rdfFactory.register(subClassOf)
                  .label(Entries.RDFS_label_subClassOf)
                  .dataType(RDFS.Class);

        rdfFactory.register(subPropertyOf)
                  .label(Entries.RDFS_label_subPropertyOf)
                  .dataType(RDF.Property);

        rdfFactory.register(domain)
                  .label(Entries.RDFS_label_domain)
                  .dataType(RDFS.Class);

        rdfFactory.register(range)
                  .label(Entries.RDFS_label_range)
                  .dataType(RDFS.Class);

        rdfFactory.register(comment)
                  .label(Entries.RDFS_label_comment)
                  .dataType(RDFS.Literal);

        rdfFactory.register(label)
                  .label(Entries.RDFS_label_label)
                  .dataType(RDFS.Literal);

        rdfFactory.register(Datatype)
                  .label(Entries.RDFS_label_Datatype);

        rdfFactory.register(Container)
                  .label(Entries.RDFS_label_Container);

        rdfFactory.register(member)
                  .label(Entries.RDFS_label_member)
                  .dataType(RDFS.Resource);

        rdfFactory.register(isDefinedBy)
                  .label(Entries.RDFS_label_isDefinedBy)
                  .dataType(RDFS.Resource);

        rdfFactory.register(seeAlso)
                  .label(Entries.RDFS_label_seeAlso)
                  .dataType(RDFS.Resource);

        rdfFactory.register(ContainerMembershipProperty)
                  .label(Entries.RDFS_label_ContainerMembershipProperty);
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
