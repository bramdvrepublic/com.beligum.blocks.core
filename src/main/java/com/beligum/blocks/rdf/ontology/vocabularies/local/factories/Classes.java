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

package com.beligum.blocks.rdf.ontology.vocabularies.local.factories;

import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfResourceFactory;
import com.beligum.blocks.rdf.ontology.RdfClassImpl;
import com.beligum.blocks.rdf.ontology.vocabularies.LocalVocabulary;
import com.beligum.blocks.rdf.ontology.vocabularies.endpoints.LocalQueryEndpoint;
import gen.com.beligum.blocks.core.messages.blocks.ontology;

import java.net.URI;

/**
 * This are a few baked-in classes
 *
 * Created by bram on 2/25/16.
 */
public class Classes implements RdfResourceFactory
{
    //-----CONSTANTS-----
    public static final int AUTOCOMPLETE_MAX_RESULTS = 10;

    //-----ENTRIES-----
    /**
     * The standard class of a new page (when nothing more specific was entered)
     */
    public static final RdfClass Page = new RdfClassImpl("Page",
                                                         LocalVocabulary.INSTANCE,
                                                         ontology.Entries.classTitle_Page,
                                                         ontology.Entries.classLabel_Page,
                                                         new URI[] {
                                                                         //Note: these two are commented out because we moved the vocabularies to a separate project,
                                                                         // we might want to implement a setter to add sameAs URIs later on from that project
                                                                         // DBR.INSTANCE.resolve("Web_page"),
                                                                         // SCHEMA.INSTANCE.resolve("WebPage")
                                                         },
                                                         true,
                                                         new LocalQueryEndpoint());


    //-----CONFIGS-----

}
