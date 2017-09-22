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

package com.beligum.blocks.rdf.ontology.factories;

import com.beligum.blocks.rdf.ifaces.RdfResourceFactory;
import com.google.common.collect.Sets;

/**
 * Created by bram on 3/23/16.
 */
public class Mappings implements RdfResourceFactory
{
    //-----ENTRIES-----
    static {
        Classes.Person.setProperties(Sets.newHashSet(Terms.givenName,
                                                     Terms.familyName,
                                                     Terms.name,
                                                     Terms.image,
                                                     Terms.title,
                                                     Terms.sameAs));

        Classes.Organization.setProperties(Sets.newHashSet(Terms.name,
                                                           Terms.role,
                                                           Terms.streetAddress,
                                                           Terms.city,
                                                           Terms.image,
                                                           Terms.title,
                                                           Terms.sameAs));

        Classes.LogEntry.setProperties(Sets.newHashSet(Terms.createdAt,
                                                       Terms.createdBy,
                                                       Terms.name,
                                                       Terms.image,
                                                       Terms.title,
                                                       Terms.sameAs));

    }
}
