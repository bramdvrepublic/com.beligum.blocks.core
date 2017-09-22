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
 * Created by bram on 2/28/16.
 */
public final class DC extends AbstractRdfVocabulary
{
    //-----SINGLETON-----
    public static final RdfVocabulary INSTANCE = new DC();
    private DC()
    {
        super(URI.create("http://purl.org/dc/terms/"), "dc");
    }

    //-----ENTRIES-----

}
