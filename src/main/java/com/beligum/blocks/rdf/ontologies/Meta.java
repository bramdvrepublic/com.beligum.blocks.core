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
import com.beligum.blocks.rdf.ifaces.RdfNamespace;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import gen.com.beligum.blocks.core.messages.blocks.ontology.Entries;

/**
 * Created by bram on 2/28/16.
 */
public class Meta extends RdfOntologyImpl
{
    //-----CONSTANTS-----
    public static final RdfNamespace NAMESPACE = Settings.instance().getRdfMetaOntologyNamespace();

    //-----MEMBERS-----

    public static final RdfProperty sameAs = RdfFactory.newProxyProperty("sameAs");

    //note the difference between the LogEntry createdAt (which is modeled after our internal BasicModel interface)
    //and this (publicly accessible) term, which is modeled after the Dublin Core (and EBU Core)
    public static final RdfProperty created = RdfFactory.newProxyProperty("created");

    public static final RdfProperty creator = RdfFactory.newProxyProperty("creator");

    public static final RdfProperty modified = RdfFactory.newProxyProperty("modified");

    public static final RdfProperty contributor = RdfFactory.newProxyProperty("contributor");

    public static final RdfProperty aclRead = RdfFactory.newProxyProperty("aclRead");

    public static final RdfProperty aclUpdate = RdfFactory.newProxyProperty("aclUpdate");

    public static final RdfProperty aclDelete = RdfFactory.newProxyProperty("aclDelete");

    public static final RdfProperty aclManage = RdfFactory.newProxyProperty("aclManage");

    //-----CONSTRUCTORS-----
    @Override
    protected void create(RdfFactory rdfFactory) throws RdfInitializationException
    {
        rdfFactory.register(sameAs)
                  .label(Entries.meta_label_sameAs)
                  .dataType(XSD.anyURI)
                  .widgetType(InputType.InlineEditor)
                  .isDefault(true);

        rdfFactory.register(created)
                  .label(Entries.meta_label_created)
                  .dataType(XSD.dateTime)
                  .widgetType(InputType.DateTime)
                  .isDefault(true);

        rdfFactory.register(creator)
                  .label(Entries.meta_label_creator)
                  .dataType(XSD.anyURI)
                  .widgetType(InputType.Resource)
                  .isDefault(true);

        rdfFactory.register(modified)
                  .label(Entries.meta_label_modified)
                  .dataType(XSD.dateTime)
                  .widgetType(InputType.DateTime)
                  .isDefault(true);

        rdfFactory.register(contributor)
                  .label(Entries.meta_label_contributor)
                  .dataType(XSD.anyURI)
                  .widgetType(InputType.Resource)
                  .isDefault(true);

        rdfFactory.register(aclRead)
                  .label(Entries.meta_label_aclRead)
                  .dataType(XSD.int_)
                  .widgetType(InputType.Number)
                  .isDefault(true);

        rdfFactory.register(aclUpdate)
                  .label(Entries.meta_label_aclUpdate)
                  .dataType(XSD.int_)
                  .widgetType(InputType.Number)
                  .isDefault(true);

        rdfFactory.register(aclDelete)
                  .label(Entries.meta_label_aclDelete)
                  .dataType(XSD.int_)
                  .widgetType(InputType.Number)
                  .isDefault(true);

        rdfFactory.register(aclManage)
                  .label(Entries.meta_label_aclManage)
                  .dataType(XSD.int_)
                  .widgetType(InputType.Number)
                  .isDefault(true);

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
