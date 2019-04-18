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

import com.beligum.blocks.config.WidgetType;
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
public class Blocks extends RdfOntologyImpl
{
    //-----CONSTANTS-----
    public static final RdfNamespace NAMESPACE = Settings.instance().getRdfBlocksOntologyNamespace();

    //-----MEMBERS-----

    public static final RdfProperty title = RdfFactory.newProxyProperty("title");
    public static final RdfProperty description = RdfFactory.newProxyProperty("description");
    public static final RdfProperty icon = RdfFactory.newProxyProperty("icon");
    public static final RdfProperty controller = RdfFactory.newProxyProperty("controller");
    public static final RdfProperty display = RdfFactory.newProxyProperty("display");
    public static final RdfProperty render = RdfFactory.newProxyProperty("render");
    public static final RdfProperty properties = RdfFactory.newProxyProperty("properties");

    //-----CONSTRUCTORS-----
    @Override
    protected void create(RdfFactory rdfFactory) throws RdfInitializationException
    {
        rdfFactory.register(title)
                  .label(Entries.blocks_label_title)
                  .dataType(RDF.langString)
                  .widgetType(WidgetType.InlineEditor);

        rdfFactory.register(description)
                  .label(Entries.blocks_label_description)
                  .dataType(RDF.langString)
                  .widgetType(WidgetType.InlineEditor);

        rdfFactory.register(icon)
                  .label(Entries.blocks_label_icon)
                  .dataType(XSD.string)
                  .widgetType(WidgetType.InlineEditor);

        rdfFactory.register(controller)
                  .label(Entries.blocks_label_controller)
                  .dataType(XSD.string)
                  .widgetType(WidgetType.InlineEditor);

        rdfFactory.register(display)
                  .label(Entries.blocks_label_display)
                  .dataType(XSD.string)
                  .widgetType(WidgetType.InlineEditor);

        rdfFactory.register(render)
                  .label(Entries.blocks_label_render)
                  .dataType(XSD.string)
                  .widgetType(WidgetType.InlineEditor);

        rdfFactory.register(properties)
                  .label(Entries.blocks_label_properties)
                  .dataType(XSD.string)
                  .widgetType(WidgetType.InlineEditor);

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
        return false;
    }
}
