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

import com.beligum.blocks.endpoints.ifaces.RdfQueryEndpoint;
import com.beligum.blocks.exceptions.RdfInitializationException;
import com.beligum.blocks.filesystem.index.entries.resources.ResourceSummarizer;
import com.beligum.blocks.filesystem.index.entries.resources.SimpleResourceSummarizer;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The main implementation of the RdfClass interface
 * <p>
 * Created by bram on 2/27/16.
 */
public class RdfClassImpl extends AbstractRdfOntologyMember implements RdfClass
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private Set<RdfClass> superClasses;
    private Set<RdfProperty> properties;
    private RdfQueryEndpoint endpoint;
    private ResourceSummarizer resourceSummarizer;
    private RdfProperty mainProperty;

    //-----CONSTRUCTORS-----
    RdfClassImpl(String name)
    {
        super(name);

        //makes sense that the properties and superclasses are returned in the same order they are added, no?
        this.superClasses = new LinkedHashSet<>();
        this.properties = new LinkedHashSet<>();
    }

    //-----PUBLIC METHODS-----
    @Override
    public Type getType()
    {
        return Type.CLASS;
    }
    @Override
    public Set<RdfClass> getSuperClasses()
    {
        this.assertNoProxy();

        return superClasses;
    }
    @Override
    public Set<RdfProperty> getProperties()
    {
        this.assertNoProxy();

        return properties;
    }
    @Override
    public RdfQueryEndpoint getEndpoint()
    {
        this.assertNoProxy();

        return endpoint;
    }
    @Override
    public ResourceSummarizer getResourceSummarizer()
    {
        this.assertNoProxy();

        return resourceSummarizer;
    }
    @Override
    public RdfProperty getMainProperty()
    {
        this.assertNoProxy();

        return mainProperty;
    }
    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----INNER CLASSES-----
    public static class Builder extends AbstractRdfOntologyMember.Builder<RdfClass, RdfClassImpl, RdfClassImpl.Builder>
    {
        Builder(RdfFactory rdfFactory, RdfClassImpl rdfClass)
        {
            super(rdfFactory, rdfClass);
        }

        public Builder superClass(RdfClass superClass) throws RdfInitializationException
        {
            return this.superClasses(superClass);
        }
        public Builder superClasses(RdfClass... superClasses) throws RdfInitializationException
        {
            for (RdfClass c : superClasses) {
                if (this.rdfResource.superClasses.contains(c)) {
                    throw new RdfInitializationException("Can't add superclass " + c + " to class " + this + " because it would overwrite and existing superclass, can't continue.");
                }
                else {
                    this.rdfResource.superClasses.add(c);
                }
            }

            return this;
        }
        public Builder property(RdfProperty property) throws RdfInitializationException
        {
            return this.properties(property);
        }
        public Builder properties(RdfProperty... properties) throws RdfInitializationException
        {
            for (RdfProperty p : properties) {
                if (this.rdfResource.properties.contains(p)) {
                    throw new RdfInitializationException("Can't add properties " + p + " to class " + this + " because it would overwrite and existing properties, can't continue.");
                }
                else {
                    this.rdfResource.properties.add(p);
                }
            }

            return this;
        }
        public Builder endpoint(RdfQueryEndpoint endpoint)
        {
            this.rdfResource.endpoint = endpoint;

            return this;
        }
        public Builder resourceSummarizer(ResourceSummarizer resourceSummarizer)
        {
            this.rdfResource.resourceSummarizer = resourceSummarizer;

            return this;
        }
        public Builder mainProperty(RdfProperty mainProperty) throws RdfInitializationException
        {
            if (!this.rdfResource.properties.contains(mainProperty)) {
                throw new RdfInitializationException("Can't set main property of class " + this + " to " + mainProperty + " because it's not a property of this class.");
            }
            else {
                this.rdfResource.mainProperty = mainProperty;
            }

            return this;
        }

        @Override
        RdfClass create() throws RdfInitializationException
        {
            //revert to default if null (this behaviour is expected in com.beligum.blocks.fs.index.entries.pages.SimplePageIndexEntry)
            if (this.rdfResource.resourceSummarizer == null) {
                this.rdfResource.resourceSummarizer = new SimpleResourceSummarizer();
            }

            for (RdfClass c : this.rdfResource.superClasses) {
                //we save the relationship and add all properties of the superclasses to this class
                this.rdfResource.superClasses.add(c);
                for (RdfProperty p : c.getProperties()) {
                    if (this.rdfResource.properties.contains(p)) {
                        throw new RdfInitializationException("RDFClass " + this + " inherits from " + c + ", but the property " + p + " would overwrite an existing property, can't continue.");
                    }
                    else {
                        this.rdfResource.properties.add(p);
                    }
                }
            }

            //Note: this call will add us to the ontology
            return super.create();
        }
    }
}
