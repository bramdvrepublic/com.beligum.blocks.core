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
import com.beligum.blocks.rdf.RdfNamespaceImpl;
import com.beligum.blocks.rdf.RdfOntologyImpl;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfNamespace;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ontologies.endpoints.LocalQueryEndpoint;
import gen.com.beligum.blocks.core.messages.blocks.ontology.Entries;

/**
 * Created by bram on 2/28/16.
 */
public class Stralo extends RdfOntologyImpl
{
    //-----CONSTANTS-----
    public static final RdfNamespace NAMESPACE = Settings.instance().getRdfStraloOntologyNamespace();

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
                  .title(Entries.propertyTitle_sameAs)
                  .label(Entries.propertyLabel_sameAs)
                  .dataType(XSD.anyURI)
                  .widgetType(InputType.InlineEditor);

        rdfFactory.register(created)
                  .title(Entries.propertyTitle_created)
                  .label(Entries.propertyLabel_created)
                  .dataType(XSD.dateTime)
                  .widgetType(InputType.DateTime);

        rdfFactory.register(creator)
                  .title(Entries.propertyTitle_creator)
                  .label(Entries.propertyLabel_creator)
                  .dataType(XSD.anyURI)
                  .widgetType(InputType.Resource);

        rdfFactory.register(modified)
                  .title(Entries.propertyTitle_modified)
                  .label(Entries.propertyLabel_modified)
                  .dataType(XSD.dateTime)
                  .widgetType(InputType.DateTime);

        rdfFactory.register(contributor)
                  .title(Entries.propertyTitle_contributor)
                  .label(Entries.propertyLabel_contributor)
                  .dataType(XSD.anyURI)
                  .widgetType(InputType.Resource);

        rdfFactory.register(aclRead)
                  .title(Entries.propertyTitle_aclRead)
                  .label(Entries.propertyLabel_aclRead)
                  .dataType(XSD.int_)
                  .widgetType(InputType.Number);

        rdfFactory.register(aclUpdate)
                  .title(Entries.propertyTitle_aclUpdate)
                  .label(Entries.propertyLabel_aclUpdate)
                  .dataType(XSD.int_)
                  .widgetType(InputType.Number);

        rdfFactory.register(aclDelete)
                  .title(Entries.propertyTitle_aclDelete)
                  .label(Entries.propertyLabel_aclDelete)
                  .dataType(XSD.int_)
                  .widgetType(InputType.Number);

        rdfFactory.register(aclManage)
                  .title(Entries.propertyTitle_aclManage)
                  .label(Entries.propertyLabel_aclManage)
                  .dataType(XSD.int_)
                  .widgetType(InputType.Number);

    }

    //-----PUBLIC METHODS-----
    @Override
    public RdfNamespace getNamespace()
    {
        return NAMESPACE;
    }
    @Override
    public boolean isPublic()
    {
        return false;
    }
}
