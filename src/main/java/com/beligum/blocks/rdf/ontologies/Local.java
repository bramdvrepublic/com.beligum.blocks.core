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
public class Local extends RdfOntologyImpl
{
    //-----CONSTANTS-----
    public static final RdfNamespace NAMESPACE = Settings.instance().getRdfMainOntologyNamespace();

    //-----MEMBERS-----
    //TODO integrate this in the ... (page?) block
    public static final RdfProperty title = RdfFactory.newProxyProperty("title");

    //TODO integrate this in the blocks-text block
    public static final RdfProperty text = RdfFactory.newProxyProperty("text");

    //this property has precedence over the general text property to be indexed as the "description" of a resource
    public static final RdfProperty description = RdfFactory.newProxyProperty("description");

    //TODO integrate this in the blocks-image block
    public static final RdfProperty image = RdfFactory.newProxyProperty("image");

    public static final RdfClass Page = RdfFactory.newProxyClass("Page");

    //-----CONSTRUCTORS-----
    @Override
    protected void create(RdfFactory rdfFactory) throws RdfInitializationException
    {
        rdfFactory.register(title)
                  .label(Entries.main_label_title)
                  .dataType(RDF.langString)
                  .widgetType(InputType.InlineEditor);

        rdfFactory.register(text)
                  .label(Entries.main_label_text)
                  .dataType(RDF.HTML)
                  .widgetType(InputType.Editor);

        rdfFactory.register(description)
                  .label(Entries.main_label_description)
                  .dataType(RDF.langString)
                  .widgetType(InputType.Editor);

        rdfFactory.register(image)
                  .label(Entries.main_label_image)
                  .dataType(XSD.anyURI)
                  .widgetType(InputType.Uri);

        rdfFactory.register(Page)
                  .label(Entries.main_label_Page)
                  .isPublic(true)
                  .endpoint(new LocalQueryEndpoint());

        //Note: these two are commented out because we moved the vocabularies to a separate project,
        // we might want to implement a setter to add sameAs URIs later on from that project
        //.sameAs(DBR.INSTANCE.resolve("Web_page"),
        //        SCHEMA.INSTANCE.resolve("WebPage"));

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
