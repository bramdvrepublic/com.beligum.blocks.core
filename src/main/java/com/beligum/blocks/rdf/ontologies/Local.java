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

import com.beligum.blocks.config.Settings;
import com.beligum.blocks.rdf.RdfOntologyImpl;

/**
 * Created by bram on 2/28/16.
 */
public class Local extends RdfOntologyImpl
{
    //-----VARIABLES-----

    //-----SINGLETON-----
    public static final Local INSTANCE = new Local();

    private Local()
    {
        super(Settings.instance().getRdfOntologyUri(), Settings.instance().getRdfOntologyPrefix());
    }

    //-----ENTRIES-----
}
