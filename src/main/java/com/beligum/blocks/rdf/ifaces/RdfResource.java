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

import com.beligum.base.database.models.ifaces.JsonObject;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.net.URI;

/**
 * This is the top-level interface for all RDF(S) related structures.
 *
 * Created by bram on 3/2/16.
 */
public interface RdfResource extends JsonObject
{
    enum Type
    {
        ONTOLOGY,
        CLASS,
        PROPERTY,
        DATATYPE,
    }

    /**
     * Return the type of this resource, see RdfResource.Type.
     * Since RdfProperty and RdfDataType are both subclasses of RdfClass,
     * this will allow us to detect pure classes (that are not a property or datatype).
     */
    Type getType();

    /**
     * Shortcut methods for the type above
     */
    boolean isOntology();
    boolean isClass();
    boolean isProperty();
    boolean isDatatype();

    /**
     * The short, well formed (capitalized and/or camel-cased) name that needs to be appended to the context of this resource to get the full describing URI for this class.
     * Eg. WaterWell, sameAs, Class, XMLLiteral, ...
     */
    String getName();

    /**
     * The full URI of this resource. Depending on the subclass, this returns the namespace URI (for ontologies) or the fullNameUri (for ontology members)
     */
    URI getUri();

    /**
     * The CURIE counterpart of the URI of this resource. Depending on the subclass, this (currently) returns null (for ontologies) or the curieNameUri (for ontology members)
     */
    URI getCurie();

    /**
     * Indicates whether this resource should be exposed to the end-users while administering pages.
     * Eg. added to public comboboxes and so on (eg. the ones in the UI on the client side)
     *
     * ----- Overview of rules -----
     *
     * Ontologies:
     *
     * - Only public ontologies are saved in the lookup maps of RdfFactory, but inside those public ontologies,
     *   references to members of other non-public ontologies will cause those non-public ontologies
     *   to be added to the lookup maps as well (because otherwise they can't be resolved).
     * - Modularized ontologies (ontologies configured from multiple classes) will be made public as soon as one of the
     *   modules configures the ontology as public.
     * - Only classes/members of public ontologies will be considered to be added to the schema of the index.
     * - All properties of public ontologies need to be configured with a datatype and a widgettype
     *
     * Classes:
     *
     * - Only public classes are exposed to the client as possible values for the @typeof attribute.
     * - Non-public classes are hidden from the user and for internal use only.
     * - Public classes need to follow our own name formatting rules (eg. begin with a capital letter)
     * - Public classes are automatically extended with all properties that are marked "default".
     *
     * Properties:
     *
     * - Only public properties are exposed to the client as possible types for the properties of the class they are requested for.
     * - Non-public properties are hidden from the user and for internal use only.
     * - Public properties need to follow our own name formatting rules (eg. begin with a lowercase letter)
     * - All public properties of all public ontologies are automatically added to the default class (eg. Page)
     *
     */
    @JsonIgnore
    boolean isPublic();

}
