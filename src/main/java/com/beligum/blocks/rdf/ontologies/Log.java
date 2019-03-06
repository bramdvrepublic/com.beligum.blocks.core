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
public class Log extends RdfOntologyImpl
{
    //-----CONSTANTS-----

    //-----MEMBERS-----
    //instead of adding the LogEntry to the general ontology class, we decided to put it here,
    // because it has little to do with the mapping of the general/public ontologies,
    // it simply uses our same API.

    public static final RdfClass LogEntry = RdfFactory.newProxyClass("LogEntry");
    public static final RdfProperty type = RdfFactory.newProxyProperty("type");
    public static final RdfProperty subject = RdfFactory.newProxyProperty("subject");
    public static final RdfProperty title = RdfFactory.newProxyProperty("title");
    public static final RdfProperty description = RdfFactory.newProxyProperty("description");
    public static final RdfProperty createdAt = RdfFactory.newProxyProperty("createdAt");
    public static final RdfProperty username = RdfFactory.newProxyProperty("username");
    public static final RdfProperty software = RdfFactory.newProxyProperty("software");
    public static final RdfProperty softwareVersion = RdfFactory.newProxyProperty("softwareVersion");

    //-----CONSTRUCTORS-----
    @Override
    protected void create(RdfFactory rdfFactory) throws RdfInitializationException
    {
        rdfFactory.register(type)
                  .title(Entries.propertyTitle_type)
                  .label(Entries.propertyLabel_type)
                  .dataType(RDF.langString)
                  .widgetType(InputType.InlineEditor);

        rdfFactory.register(subject)
                  .title(Entries.propertyTitle_subject)
                  .label(Entries.propertyLabel_subject)
                  .dataType(XSD.anyURI)
                  .widgetType(InputType.Resource);

        rdfFactory.register(title)
                  .title(Entries.propertyTitle_title)
                  .label(Entries.propertyLabel_title)
                  .dataType(RDF.langString)
                  .widgetType(InputType.InlineEditor);

        rdfFactory.register(description)
                  .title(Entries.propertyTitle_description)
                  .label(Entries.propertyLabel_description)
                  .dataType(RDF.langString)
                  .widgetType(InputType.Editor);

        rdfFactory.register(createdAt)
                  .title(Entries.propertyTitle_createdAt)
                  .label(Entries.propertyLabel_createdAt)
                  .dataType(XSD.dateTime)
                  .widgetType(InputType.DateTime);

        rdfFactory.register(username)
                  .title(Entries.propertyTitle_username)
                  .label(Entries.propertyLabel_username)
                  .dataType(XSD.string)
                  .widgetType(InputType.InlineEditor);

        rdfFactory.register(software)
                  .title(Entries.propertyTitle_software)
                  .label(Entries.propertyLabel_software)
                  .dataType(RDF.langString)
                  .widgetType(InputType.InlineEditor);

        rdfFactory.register(softwareVersion)
                  .title(Entries.propertyTitle_softwareVersion)
                  .label(Entries.propertyLabel_softwareVersion)
                  .dataType(RDF.langString)
                  .widgetType(InputType.InlineEditor);

        rdfFactory.register(LogEntry)
                  .title(Entries.classTitle_LogEntry)
                  .label(Entries.classLabel_LogEntry)
                  .properties(type,
                              subject,
                              title,
                              description,
                              createdAt,
                              username,
                              software,
                              softwareVersion);
    }

    //-----PUBLIC METHODS-----
    @Override
    public RdfNamespace getNamespace()
    {
        return null;
    }
    @Override
    public boolean isPublic()
    {
        return false;
    }
}
