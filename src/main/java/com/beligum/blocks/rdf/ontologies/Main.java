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

import com.beligum.blocks.config.InputType;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.exceptions.RdfInitializationException;
import com.beligum.blocks.rdf.RdfFactory;
import com.beligum.blocks.rdf.RdfOntologyImpl;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfNamespace;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ontologies.endpoints.LocalQueryEndpoint;
import gen.com.beligum.blocks.core.messages.blocks.ontology.Entries;

/**
 * Created by bram on 2/28/16.
 */
public class Main extends RdfOntologyImpl
{
    //-----CONSTANTS-----
    public static final RdfNamespace NAMESPACE = Settings.instance().getRdfMainOntologyNamespace();

    //-----MEMBERS-----
    public static final RdfClass Page = RdfFactory.newProxyClass("Page");

    //-----CONSTRUCTORS-----
    @Override
    protected void create(RdfFactory rdfFactory) throws RdfInitializationException
    {
        rdfFactory.register(Page)
                  .isPublic(true)
                  .label(Entries.main_label_Page)
                  .endpoint(new LocalQueryEndpoint());
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
        return true;
    }
}
