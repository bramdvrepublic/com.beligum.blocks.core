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

import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.InputType;
import com.beligum.blocks.config.InputTypeConfig;
import com.beligum.blocks.endpoints.ifaces.RdfQueryEndpoint;
import com.beligum.blocks.exceptions.RdfInitializationException;
import com.beligum.blocks.filesystem.index.entries.RdfIndexer;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfOntology;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.indexers.DefaultRdfPropertyIndexer;
import com.beligum.blocks.rdf.ifaces.RdfPropertyIndexer;
import com.beligum.blocks.rdf.ontologies.XSD;
import org.eclipse.rdf4j.model.Value;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Created by bram on 2/25/16.
 */
public class RdfPropertyImpl extends AbstractRdfOntologyMember implements RdfProperty
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private RdfClassImpl dataType;
    private RdfQueryEndpoint endpoint;
    private InputType widgetType;
    private InputTypeConfig widgetConfig;
    private RdfPropertyIndexer indexer;

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
    public RdfQueryEndpoint getEndpoint()
    {
        this.assertNoProxy();

        return endpoint;
    }
    @Override
    public InputType getWidgetType()
    {
        this.assertNoProxy();

        return widgetType;
    }
    @Override
    public InputTypeConfig getWidgetConfig()
    {
        this.assertNoProxy();

        return widgetConfig;
    }
    @Override
    public RdfIndexer.IndexResult indexValue(RdfIndexer indexer, URI resource, Value value, Locale language, RdfQueryEndpoint.SearchOption... options) throws IOException
    {
        this.assertNoProxy();

        return this.indexer.index(indexer, resource, this, value, language, options);
    }
    @Override
    public Object prepareIndexValue(String value, Locale language) throws IOException
    {
        this.assertNoProxy();

        return this.indexer.prepareIndexValue(this, value, language);
    }

    //-----PROTECTED METHODS-----
    /**
     * We need to overload this method for a RdfProperty to also add all the referenced ontologies
     * of the datatypes
     */
    @Override
    protected void _findOntologyReferences(RdfOntologyImpl.Visitor ontologyVisitor)
    {
        super._findOntologyReferences(ontologyVisitor);

        this.dataType._findOntologyReferences(ontologyVisitor);
    }

    //-----PRIVATE METHODS-----

    //-----INNER CLASSES-----
    public static class Builder extends AbstractRdfOntologyMember.Builder<RdfProperty, RdfPropertyImpl, RdfPropertyImpl.Builder>
    {
        Builder(RdfFactory rdfFactory, RdfPropertyImpl rdfProperty)
        {
            super(rdfFactory, rdfProperty);
        }

        public Builder dataType(RdfClass dataType)
        {
            this.rdfResource.dataType = (RdfClassImpl) dataType;

            return this;
        }
        public Builder widgetType(InputType widgetType)
        {
            this.rdfResource.widgetType = widgetType;

            return this;
        }
        public Builder widgetConfig(InputTypeConfig widgetConfig)
        {
            this.rdfResource.widgetConfig = widgetConfig;

            return this;
        }
        public Builder indexer(RdfPropertyIndexer indexer)
        {
            this.rdfResource.indexer = indexer;

            return this;
        }

        @Override
        RdfProperty create() throws RdfInitializationException
        {
            //make it uniform; no nulls
            if (this.rdfResource.widgetConfig == null) {
                this.rdfResource.widgetConfig = new InputTypeConfig();
            }

            if (this.rdfResource.dataType == null) {
                Logger.error("Datatype of " + this.rdfResource.getName() + " (" + this.rdfResource.getFullName() + ") is null! This is a static-initializer bug and should be fixed");
            }
            else {
                //this is a double-check to make sure we accidently don't select the wrong inputtype for date/time
                if ((this.rdfResource.dataType.equals(XSD.date) && !this.rdfResource.widgetType.equals(InputType.Date))
                    || (this.rdfResource.dataType.equals(XSD.time) && !this.rdfResource.widgetType.equals(InputType.Time))
                    || (this.rdfResource.dataType.equals(XSD.dateTime) && !this.rdfResource.widgetType.equals(InputType.DateTime))) {
                    throw new RdfInitializationException("Encountered RDF property with datatype-inputtype mismatch; " + this);
                }
            }

            if (this.rdfResource.indexer == null) {
                this.rdfResource.indexer = DefaultRdfPropertyIndexer.INSTANCE;
            }

            //Note: this call will add us to the ontology
            return super.create();
        }
    }
}
