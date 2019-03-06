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
public class Local extends RdfOntologyImpl
{
    //-----CONSTANTS-----
    public static final RdfNamespace NAMESPACE = new RdfNamespaceImpl(Settings.instance().getRdfOntologyUri(), Settings.instance().getRdfOntologyPrefix());

    //-----MEMBERS-----
    //TODO integrate this in the ... (page?) block
    public static final RdfProperty title = RdfFactory.newProxyProperty("title");

    //TODO integrate this in the blocks-text block
    public static final RdfProperty text = RdfFactory.newProxyProperty("text");

    //this property has precedence over the general text property to be indexed as the "description" of a resource
    public static final RdfProperty description = RdfFactory.newProxyProperty("description");

    //TODO integrate this in the blocks-image block
    public static final RdfProperty image = RdfFactory.newProxyProperty("image");

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

    public static final RdfClass Page = RdfFactory.newProxyClass("Page");

    //-----CONSTRUCTORS-----
    @Override
    protected void create(RdfFactory rdfFactory) throws RdfInitializationException
    {
        rdfFactory.register(title)
                  .title(Entries.propertyTitle_title)
                  .label(Entries.propertyLabel_title)
                  .dataType(RDF.langString)
                  .widgetType(InputType.InlineEditor);

        rdfFactory.register(text)
                  .title(Entries.propertyTitle_text)
                  .label(Entries.propertyLabel_text)
                  .dataType(RDF.HTML)
                  .widgetType(InputType.Editor);

        rdfFactory.register(description)
                  .title(Entries.propertyTitle_description)
                  .label(Entries.propertyLabel_description)
                  .dataType(RDF.langString)
                  .widgetType(InputType.Editor);

        rdfFactory.register(image)
                  .title(Entries.propertyTitle_image)
                  .label(Entries.propertyLabel_image)
                  .dataType(XSD.anyURI)
                  .widgetType(InputType.Uri);

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

        //Note: these two are commented out because we moved the vocabularies to a separate project,
        // we might want to implement a setter to add sameAs URIs later on from that project
        // DBR.INSTANCE.resolve("Web_page"),
        // SCHEMA.INSTANCE.resolve("WebPage")
        rdfFactory.register(Page)
                  .title(Entries.classTitle_Page)
                  .label(Entries.classLabel_Page)
                  .isPublic(true)
                  .endpoint(new LocalQueryEndpoint());

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
        return true;
    }
}
