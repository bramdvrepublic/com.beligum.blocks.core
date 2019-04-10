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

package com.beligum.blocks.index.summarizers;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.rdf.ifaces.RdfEndpoint;
import com.beligum.blocks.index.ifaces.ResourceSummarizer;
import com.beligum.blocks.index.ifaces.ResourceSummary;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
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
 * A simple summarizer that tries to summarize an RDF graph based on some heuristics and settings.
 *
 * Created by bram on 5/10/16.
 */
public class SimpleResourceSummarizer implements ResourceSummarizer
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private IRI labelIri;

    //-----CONSTRUCTORS-----
    public SimpleResourceSummarizer()
    {
        this.labelIri = SimpleValueFactory.getInstance().createIRI(Settings.instance().getRdfLabelProperty().getFullName().toString());
    }

    //-----PUBLIC METHODS-----
    /**
     * Note: made synchronized to make it thread safe because of the single instance above
     */
    @Override
    public synchronized ResourceSummary summarize(RdfClass type, Model model)
    {
        //let's build in support for the main properties of our newly added sub-resources
        IRI mainIri = null;
        Set<IRI> labelIris = new LinkedHashSet<>();
        if (type != null) {
            if (type.getMainProperty() != null) {
                mainIri = SimpleValueFactory.getInstance().createIRI(type.getMainProperty().getFullName().toString());
            }
            RdfEndpoint typeEndpoint = type.getEndpoint();
            if (typeEndpoint != null) {
                RdfProperty[] labelCandidates = typeEndpoint.getLabelCandidates(type);
                if (labelCandidates != null) {
                    for (RdfProperty labelProp : labelCandidates) {
                        labelIris.add(SimpleValueFactory.getInstance().createIRI(labelProp.getFullName().toString()));
                    }
                }
            }
        }

        String label = null;
        String altLabel = null;
        String main = null;
        String text = null;
        String description = null;
        URI image = null;

        Iterator<Statement> iter = model.iterator();
        while (iter.hasNext()) {
            Statement stmt = iter.next();

            if (stmt.getPredicate().equals(labelIri)) {
                if (label == null) {
                    label = stmt.getObject().stringValue();
                }
                else {
                    Logger.debug("Double " + labelIri + " predicate entry found for " + stmt.getSubject() + "; only using first.");
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
            else if (stmt.getPredicate().getLocalName().equalsIgnoreCase("text")) {
                if (text == null) {
                    text = stmt.getObject().stringValue();
                }
                else {
                    Logger.debug("Double text predicate entry found for " + stmt.getSubject() + ", only using first; " + stmt.getPredicate());
                }
            }
            else if (stmt.getPredicate().getLocalName().equalsIgnoreCase("description")) {
                if (description == null) {
                    description = stmt.getObject().stringValue();
                }
                else {
                    Logger.debug("Double description predicate entry found for " + stmt.getSubject() + ", only using first; " + stmt.getPredicate());
                }
            }
            else if (stmt.getPredicate().getLocalName().equalsIgnoreCase("image")) {
                if (image == null) {
                    image = URI.create(stmt.getObject().stringValue());
                }
                else {
                    Logger.debug("Double image predicate entry found for " + stmt.getSubject() + ", only using first; " + stmt.getPredicate());
                }
            }

            // save some alternative label values
            if (labelIris.contains(stmt.getPredicate())) {
                if (altLabel == null) {
                    altLabel = stmt.getObject().stringValue();
                }
                else {
                    Logger.debug("Double altLabel predicate entry found for " + stmt.getSubject() + ", only using first; " + stmt.getPredicate());
                }
            }
        }

        // if for one reason or the other, the label is not found, try the alternative because having a label is quite important
        if (StringUtils.isEmpty(label)) {
            label = altLabel;
        }

        // most of the time, for sub-resources, the label will be empty (update: is it?), so let's check if we have a main property value instead...
        if (StringUtils.isEmpty(label)) {
            label = main;
        }

        //a description property has precedence over a general 'text', but only if it's not empty
        if (StringUtils.isEmpty(description)) {
            description = text;
        }

        return new DefaultSummarizedResource(label, description, image);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
