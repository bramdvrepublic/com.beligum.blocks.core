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

package com.beligum.blocks.filesystem.index.entries.resources;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.endpoints.ifaces.RdfQueryEndpoint;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ontologies.local.factories.Terms;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import java.net.URI;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by bram on 5/10/16.
 */
public class SimpleResourceSummarizer implements ResourceSummarizer
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private boolean initialized;
    private IRI titleIri;
    private IRI textIri;
    private IRI descriptionIri;
    private IRI imageIri;

    //-----CONSTRUCTORS-----
    //making it private and using the INSTANCE method instead to have control over the static initialization order...
    public SimpleResourceSummarizer()
    {
        this.initialized = false;
    }

    //-----PUBLIC METHODS-----
    /**
     * Note: made synchronized to make it thread safe because of the single instance above
     */
    @Override
    public synchronized SummarizedResource summarize(RdfClass type, Model model)
    {
        //Need to do this here or we'll run into trouble while initializing static members
        this.assertInit();

        String title = null;
        String main = null;
        String label = null;
        String text = null;
        String description = null;
        URI image = null;

        //let's build in support for the main properties of our newly added sub-resources
        IRI mainIri = null;
        Set<IRI> labelIris = new LinkedHashSet<>();
        if (type != null) {
            if (type.getMainProperty() != null) {
                mainIri = SimpleValueFactory.getInstance().createIRI(type.getMainProperty().getFullName().toString());
            }
            RdfQueryEndpoint typeEndpoint = type.getEndpoint();
            if (typeEndpoint != null) {
                RdfProperty[] labelCandidates = typeEndpoint.getLabelCandidates(type);
                if (labelCandidates != null) {
                    for (RdfProperty labelProp : labelCandidates) {
                        labelIris.add(SimpleValueFactory.getInstance().createIRI(labelProp.getFullName().toString()));
                    }
                }
            }
        }

        Iterator<Statement> iter = model.iterator();
        while (iter.hasNext()) {
            Statement stmt = iter.next();

            if (stmt.getPredicate().equals(titleIri)) {
                if (title == null) {
                    title = stmt.getObject().stringValue();
                }
                else {
                    Logger.debug("Double " + titleIri + " predicate entry found for " + stmt.getSubject() + "; only using first.");
                }
            }
            else if (stmt.getPredicate().equals(textIri)) {
                if (text == null) {
                    text = stmt.getObject().stringValue();
                }
                else {
                    Logger.debug("Double " + textIri + " predicate entry found for " + stmt.getSubject() + "; only using first.");
                }
            }
            else if (stmt.getPredicate().equals(mainIri)) {
                if (main == null) {
                    main = stmt.getObject().stringValue();
                }
                else {
                    Logger.debug("Double " + mainIri + " predicate entry found for " + stmt.getSubject() + "; only using first.");
                }
            }
            else if (stmt.getPredicate().equals(descriptionIri)) {
                if (description == null) {
                    description = stmt.getObject().stringValue();
                }
                else {
                    Logger.debug("Double " + descriptionIri + " predicate entry found for " + stmt.getSubject() + "; only using first.");
                }
            }
            else if (stmt.getPredicate().equals(imageIri)) {
                if (image == null) {
                    image = URI.create(stmt.getObject().stringValue());
                }
                else {
                    Logger.debug("Double " + imageIri + " predicate entry found for " + stmt.getSubject() + "; only using first.");
                }
            }

            if (labelIris.contains(stmt.getPredicate())) {
                if (label == null) {
                    label = stmt.getObject().stringValue();
                }
                else {
                    Logger.debug("Double label candidate found for " + stmt.getSubject() + "; only using first.");
                }
            }
        }

        //a description property has precedence over a general 'text', but only if it's not empty
        if (StringUtils.isEmpty(description)) {
            description = text;
        }

        //most of the time, for sub-resources, the title will be empty, so let's check if we have a main property value instead...
        if (StringUtils.isEmpty(title)) {
            title = main;
        }

        //this is a last-option check and a possibility to link the main property, label candidate and title concepts together
        if (StringUtils.isEmpty(title)) {
            title = label;
        }

        return new DefaultSummarizedResource(title, description, image);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private void assertInit()
    {
        if (!this.initialized) {
            this.titleIri = SimpleValueFactory.getInstance().createIRI(Terms.title.getFullName().toString());
            this.textIri = SimpleValueFactory.getInstance().createIRI(Terms.text.getFullName().toString());
            this.descriptionIri = SimpleValueFactory.getInstance().createIRI(Terms.description.getFullName().toString());
            this.imageIri = SimpleValueFactory.getInstance().createIRI(Terms.image.getFullName().toString());

            this.initialized = true;
        }
    }
}
