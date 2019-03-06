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

package com.beligum.blocks.rdf.ontologies.local.factories;

import com.beligum.blocks.config.InputType;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ifaces.RdfResourceFactory;
import com.beligum.blocks.rdf.RdfPropertyImpl;
import com.beligum.blocks.rdf.ontology.vocabularies.*;
import com.beligum.blocks.rdf.ontologies.Local;
import com.beligum.blocks.rdf.ontologies.OWL;
import com.beligum.blocks.rdf.ontologies.RDF;
import com.beligum.blocks.rdf.ontologies.XSD;
import gen.com.beligum.blocks.core.messages.blocks.ontology;

import java.net.URI;

/**
 * Note: we try to keep this list as short as possible and keep all ontology-implementations in sub-modules.
 * However, some property-references are needed by the core system (eg. during indexing), so these are the properties
 * that are too integrated in blocks-core to move anywhere else.
 * <p>
 * Created by bram on 3/22/16.
 */
public class Terms implements RdfResourceFactory
{
    //-----CONSTANTS-----

    //-----ENTRIES-----
    //TODO integrate this in the ... (page?) block
    public static final RdfProperty title = new RdfPropertyImpl("title",
                                                                Local.INSTANCE,
                                                                ontology.Entries.propertyTitle_title,
                                                                ontology.Entries.propertyLabel_title,
                                                                RDF.LANGSTRING,
                                                                InputType.InlineEditor,
                                                                null,
                                                                new URI[] {
                                                                },
                                                                false);

    //TODO integrate this in the blocks-text block
    public static final RdfProperty text = new RdfPropertyImpl("text",
                                                               Local.INSTANCE,
                                                               ontology.Entries.propertyTitle_text,
                                                               ontology.Entries.propertyLabel_text,
                                                               RDF.HTML,
                                                               InputType.Editor,
                                                               null,
                                                               new URI[] {
                                                               },
                                                               false);

    //this property has precedence over the general text property to be indexed as the "description" of a resource
    public static final RdfProperty description = new RdfPropertyImpl("description",
                                                                      Local.INSTANCE,
                                                                      ontology.Entries.propertyTitle_description,
                                                                      ontology.Entries.propertyLabel_description,
                                                                      RDF.LANGSTRING,
                                                                      InputType.Editor,
                                                                      null,
                                                                      new URI[] {
                                                                      },
                                                                      false);

    //TODO integrate this in the blocks-image block
    public static final RdfProperty image = new RdfPropertyImpl("image",
                                                                Local.INSTANCE,
                                                                ontology.Entries.propertyTitle_image,
                                                                ontology.Entries.propertyLabel_image,
                                                                XSD.anyURI,
                                                                InputType.Uri,
                                                                null,
                                                                new URI[] {
                                                                },
                                                                false);

    public static final RdfProperty sameAs = new RdfPropertyImpl("sameAs",
                                                                 Local.INSTANCE,
                                                                 ontology.Entries.propertyTitle_sameAs,
                                                                 ontology.Entries.propertyLabel_sameAs,
                                                                 XSD.anyURI,
                                                                 InputType.InlineEditor,
                                                                 null,
                                                                 new URI[] { OWL.SAMEAS.getFullName()
                                                                 },
                                                                 false);

    //note the difference between the LogEntry createdAt (which is modeled after our internal BasicModel interface)
    //and this (publicly accessible) term, which is modeled after the Dublin Core (and EBU Core)
    public static final RdfProperty created = new RdfPropertyImpl("created",
                                                                  Local.INSTANCE,
                                                                  ontology.Entries.propertyTitle_created,
                                                                  ontology.Entries.propertyLabel_created,
                                                                  XSD.dateTime,
                                                                  InputType.DateTime,
                                                                  null,
                                                                  new URI[] { // TODO http://dublincore.org/documents/dcmi-terms/#terms-created
                                                                  },
                                                                  false);

    public static final RdfProperty creator = new RdfPropertyImpl("creator",
                                                                  Local.INSTANCE,
                                                                  ontology.Entries.propertyTitle_creator,
                                                                  ontology.Entries.propertyLabel_creator,
                                                                  XSD.anyURI,
                                                                  InputType.Resource,
                                                                  null,
                                                                  new URI[] { // TODO http://dublincore.org/documents/dcmi-terms/#terms-creator
                                                                  },
                                                                  false);

    public static final RdfProperty modified = new RdfPropertyImpl("modified",
                                                                   Local.INSTANCE,
                                                                   ontology.Entries.propertyTitle_modified,
                                                                   ontology.Entries.propertyLabel_modified,
                                                                   XSD.dateTime,
                                                                   InputType.DateTime,
                                                                   null,
                                                                   new URI[] { //TODO http://dublincore.org/documents/dcmi-terms/#terms-modified
                                                                   },
                                                                   false);

    public static final RdfProperty contributor = new RdfPropertyImpl("contributor",
                                                                      Local.INSTANCE,
                                                                      ontology.Entries.propertyTitle_contributor,
                                                                      ontology.Entries.propertyLabel_contributor,
                                                                      XSD.anyURI,
                                                                      InputType.Resource,
                                                                      null,
                                                                      new URI[] { // TODO http://dublincore.org/documents/dcmi-terms/#terms-contributor
                                                                      },
                                                                      false);

    public static final RdfProperty aclRead = new RdfPropertyImpl("aclRead",
                                                                  Local.INSTANCE,
                                                                  ontology.Entries.propertyTitle_aclRead,
                                                                  ontology.Entries.propertyLabel_aclRead,
                                                                  XSD.int_,
                                                                  InputType.Number,
                                                                  null,
                                                                  new URI[] {
                                                                  },
                                                                  false);

    public static final RdfProperty aclUpdate = new RdfPropertyImpl("aclUpdate",
                                                                    Local.INSTANCE,
                                                                    ontology.Entries.propertyTitle_aclUpdate,
                                                                    ontology.Entries.propertyLabel_aclUpdate,
                                                                    XSD.int_,
                                                                    InputType.Number,
                                                                    null,
                                                                    new URI[] {
                                                                    },
                                                                    false);

    public static final RdfProperty aclDelete = new RdfPropertyImpl("aclDelete",
                                                                    Local.INSTANCE,
                                                                    ontology.Entries.propertyTitle_aclDelete,
                                                                    ontology.Entries.propertyLabel_aclDelete,
                                                                    XSD.int_,
                                                                    InputType.Number,
                                                                    null,
                                                                    new URI[] {
                                                                    },
                                                                    false);

    public static final RdfProperty aclManage = new RdfPropertyImpl("aclManage",
                                                                    Local.INSTANCE,
                                                                    ontology.Entries.propertyTitle_aclManage,
                                                                    ontology.Entries.propertyLabel_aclManage,
                                                                    XSD.int_,
                                                                    InputType.Number,
                                                                    null,
                                                                    new URI[] {
                                                                    },
                                                                    false);

    //-----CONFIGS-----

}
