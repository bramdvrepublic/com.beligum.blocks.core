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

import java.util.Collection;

/**
 * A special kind of RDF resource that can hold other resources (eg. vocabularies and classes)
 *
 * Created by bram on 2/3/19.
 */
public interface RdfCompositeResource extends RdfResource
{
    /**
     * Returns all members of this composite parent resource.
     */
    Collection<RdfResource> getMembers();
}
