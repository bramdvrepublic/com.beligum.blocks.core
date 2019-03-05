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

package com.beligum.blocks.rdf.ifaces;

import com.beligum.blocks.rdf.RdfFactory;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.net.URI;
import java.util.Collection;
import java.util.Map;

/**
 * Created by bram on 2/28/16.
 */
public interface RdfOntology extends RdfResource
{
    /**
     * The namespace of this vocabulary, consisting of a full URI and a shorthand prefix
     */
    RdfNamespace getNamespace();

    /**
     * Resolves the supplied suffix against the full namespace of this vocabulary
     */
    @JsonIgnore
    URI resolve(String suffix);

    /**
     * Resolves the supplied suffix against the curie namespace of this vocabulary
     */
    @JsonIgnore
    URI resolveCurie(String suffix);

    /**
     * Returns all typed members (classes, properties and dataTypes) in this vocabulary
     */
    //avoids infinite recursion
    @JsonIgnore
    Map<URI, RdfOntologyMember> getAllMembers();

    /**
     * Returns all classes in this vocabulary
     */
    //avoids infinite recursion
    @JsonIgnore
    Map<URI, RdfClass> getAllClasses();

    /**
     * Returns all classes in this vocabulary that are selectable from the client-side page-type-dropdown
     */
    //avoids infinite recursion
    @JsonIgnore
    Map<URI, RdfClass> getPublicClasses();

    /**
     * Returns all properties in this vocabulary
     */
    //avoids infinite recursion
    @JsonIgnore
    Map<URI, RdfDataType> getAllDataTypes();

    /**
     * Returns all properties in this vocabulary that are accessible from the client-side UI
     */
    //avoids infinite recursion
    @JsonIgnore
    Map<URI, RdfDataType> getPublicDataTypes();

    /**
     * Returns all properties in this vocabulary
     */
    //avoids infinite recursion
    @JsonIgnore
    Map<URI, RdfProperty> getAllProperties();

    /**
     * Returns all properties in this vocabulary that are accessible from the client-side UI
     */
    //avoids infinite recursion
    @JsonIgnore
    Map<URI, RdfProperty> getPublicProperties();

    /**
     * Registers an ontology member into this ontology; don't use this manually, it's for internal use only (hence the factory dependency)
     */
    @JsonIgnore
    void register(RdfFactory rdfFactory, RdfOntologyMember member);

}
