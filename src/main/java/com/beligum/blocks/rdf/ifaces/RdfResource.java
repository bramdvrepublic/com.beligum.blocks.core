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
    /**
     * Indicates whether this resource should be added to public comboboxes and so on (eg. the ones in the UI on the client side)
     */
    @JsonIgnore
    boolean isPublic();
}
