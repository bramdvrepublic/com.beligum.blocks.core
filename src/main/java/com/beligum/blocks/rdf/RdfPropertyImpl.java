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

package com.beligum.blocks.rdf;

import com.beligum.base.filesystem.ConstantsFileEntry;
import com.beligum.blocks.config.WidgetType;
import com.beligum.blocks.config.WidgetTypeConfig;
import com.beligum.blocks.index.ifaces.IndexSearchRequest;
import com.beligum.blocks.rdf.ifaces.RdfEndpoint;
import com.beligum.blocks.exceptions.RdfInitializationException;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ontologies.XSD;
import gen.com.beligum.blocks.core.constants.blocks.core;
import gen.com.beligum.blocks.endpoints.RdfEndpointRoutes;

import java.net.URI;

/**
 * Created by bram on 2/25/16.
 */
public class RdfPropertyImpl extends AbstractRdfOntologyMember implements RdfProperty
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    protected RdfClassImpl dataType;
    protected RdfEndpoint endpoint;
    protected WidgetType widgetType;
    protected WidgetTypeConfig widgetConfig;

    //-----CONSTRUCTORS-----
    RdfPropertyImpl(RdfOntologyImpl ontology, String name)
    {
        super(ontology, name, false);
    }

    //-----PUBLIC METHODS-----
    @Override
    public Type getType()
    {
        return Type.PROPERTY;
    }
    @Override
    public RdfClass getDataType()
    {
        this.assertNoProxy();

        return dataType;
    }
    @Override
    public RdfEndpoint getEndpoint()
    {
        this.assertNoProxy();

        return endpoint;
    }
    @Override
    public WidgetType getWidgetType()
    {
        this.assertNoProxy();

        return widgetType;
    }
    @Override
    public WidgetTypeConfig getWidgetConfig()
    {
        this.assertNoProxy();

        return widgetConfig;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----INNER CLASSES-----
    public static class Builder extends AbstractRdfOntologyMember.Builder<RdfProperty, RdfPropertyImpl, RdfPropertyImpl.Builder>
    {
        Builder(RdfFactory rdfFactory, RdfPropertyImpl rdfProperty)
        {
            super(rdfFactory, rdfProperty);

            this.rdfResource.widgetConfig = new WidgetTypeConfig();
        }

        public Builder dataType(RdfClass dataType)
        {
            this.rdfResource.dataType = (RdfClassImpl) dataType;

            return this;
        }
        public Builder widgetType(WidgetType widgetType)
        {
            this.rdfResource.widgetType = widgetType;

            return this;
        }
        public Builder widgetConfig(ConstantsFileEntry key, String value)
        {
            this.rdfResource.widgetConfig.put(key.getValue(), value);

            return this;
        }
        //        public Builder indexer(RdfPropertyIndexer indexer)
        //        {
        //            this.rdfResource.indexer = indexer;
        //
        //            return this;
        //        }

        @Override
        RdfProperty create() throws RdfInitializationException
        {
            if (this.rdfResource.ontology.isPublic) {

                //enforce a naming policy on the properties of our local public ontologies
                if (!Character.isLowerCase(this.rdfResource.name.charAt(0))) {
                    throw new RdfInitializationException("Encountered RDF property with uppercase name; our policy enforces all RDF properties should start with a lowercase letter; " + this);
                }

                //enforces the properties in our local public ontologies to have valid datatypes
                if (this.rdfResource.dataType == null) {
                    throw new RdfInitializationException("Datatype of RDF property '" + this.rdfResource.getName() + "' inside a public ontology is null. This is not allowed; " + this);
                }
                else if (this.rdfResource.widgetType == null) {
                    throw new RdfInitializationException("Widget type of RDF property '" + this.rdfResource.getName() + "' inside a public ontology is null. This is not allowed; " + this);
                }
                else {
                    //this is a double-check to make sure we accidently don't select the wrong inputtype for date/time
                    if ((this.rdfResource.dataType.equals(XSD.date) && !this.rdfResource.widgetType.equals(WidgetType.Date))
                        || (this.rdfResource.dataType.equals(XSD.time) && !this.rdfResource.widgetType.equals(WidgetType.Time))
                        || (this.rdfResource.dataType.equals(XSD.dateTime) && !this.rdfResource.widgetType.equals(WidgetType.DateTime))) {
                        throw new RdfInitializationException("Encountered RDF property with a datatype-widgetType mismatch; " + this);
                    }
                }

                // auto-init some widget configs for public properties if none were set
                if (this.rdfResource.isPublic) {

                    if (this.rdfResource.widgetType.equals(WidgetType.Resource)) {

                        if (this.rdfResource.dataType.endpoint == null) {
                            throw new RdfInitializationException("Encountered RDF property '" + this.rdfResource.getName() + "' with datatype '" + this.rdfResource.dataType +
                                                                 "' that has a missing endpoint. This is not allowed; " + this);
                        }
                        else {
                            if (!this.rdfResource.widgetConfig.containsKey(core.Entries.INPUT_TYPE_CONFIG_RESOURCE_AC_ENDPOINT)) {
                                this.widgetConfig(core.Entries.INPUT_TYPE_CONFIG_RESOURCE_AC_ENDPOINT,
                                                  RdfEndpointRoutes.getResources(this.rdfResource.dataType.getCurieName(), IndexSearchRequest.DEFAULT_MAX_SEARCH_RESULTS, true, "").getAbsoluteUrl());
                            }
                            if (!this.rdfResource.widgetConfig.containsKey(core.Entries.INPUT_TYPE_CONFIG_RESOURCE_VAL_ENDPOINT)) {
                                this.widgetConfig(core.Entries.INPUT_TYPE_CONFIG_RESOURCE_VAL_ENDPOINT,
                                                  RdfEndpointRoutes.getResource(this.rdfResource.dataType.getCurieName(), URI.create("")).getAbsoluteUrl());
                            }
                            if (!this.rdfResource.widgetConfig.containsKey(core.Entries.INPUT_TYPE_CONFIG_RESOURCE_MAXRESULTS)) {
                                this.widgetConfig(core.Entries.INPUT_TYPE_CONFIG_RESOURCE_MAXRESULTS, "" + IndexSearchRequest.DEFAULT_MAX_SEARCH_RESULTS);
                            }
                        }
                    }
                }
            }

            //make it uniform; no nulls
            if (this.rdfResource.widgetConfig == null) {
                this.rdfResource.widgetConfig = new WidgetTypeConfig();
            }

            //Note: this call will add us to the ontology
            return super.create();
        }
    }
}
