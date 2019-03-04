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

/**
 * This is the top-level interface for all RDF(S) related classes
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
     * The short, well formed (capitalized and/or camel-cased) name that needs to be appended to the context of this resource to get the full describing URI for this class.
     * Eg. WaterWell, sameAs, Class, XMLLiteral, ...
     */
    String getName();

    /**
     * Indicates whether this resource should be exposed to the end-users while administering pages.
     * Eg. added to public comboboxes and so on (eg. the ones in the UI on the client side)
     */
    @JsonIgnore
    boolean isPublic();


}
