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

import com.beligum.blocks.config.Settings;
import com.beligum.blocks.endpoints.ifaces.RdfQueryEndpoint;
import com.beligum.blocks.exceptions.RdfInitializationException;
import com.beligum.blocks.filesystem.index.ifaces.ResourceSummarizer;
import com.beligum.blocks.filesystem.index.entries.resources.SimpleResourceSummarizer;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

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
    protected Set<RdfClassImpl> superClasses;
    protected Set<RdfClassImpl> subClasses;
    protected Set<RdfPropertyImpl> properties;
    protected RdfQueryEndpoint endpoint;
    protected ResourceSummarizer summarizer;
    protected RdfProperty mainProperty;

    //-----CONSTRUCTORS-----
    RdfClassImpl(RdfOntologyImpl ontology, String name)
    {
        super(ontology, name, false);

        //makes sense that the properties and superclasses are returned in the same order they are added, no?
        this.superClasses = new LinkedHashSet<>();
        this.subClasses = new LinkedHashSet<>();
        this.properties = new LinkedHashSet<>();
    }

    //-----PUBLIC METHODS-----
    @Override
    public Type getType()
    {
        return Type.CLASS;
    }
    @Override
    public Iterable<RdfClass> getSuperClasses()
    {
        this.assertNoProxy();

        //this does nothing more than restrict the member values to the interface
        return Iterables.transform(this.superClasses,
                                   new Function<RdfClassImpl, RdfClass>()
                                   {
                                       @Override
                                       public RdfClass apply(RdfClassImpl member)
                                       {
                                           return member;
                                       }
                                   }
        );
    }
    @Override
    public Iterable<RdfClass> getSubClasses()
    {
        this.assertNoProxy();

        //this does nothing more than restrict the member values to the interface
        return Iterables.transform(this.subClasses,
                                   new Function<RdfClassImpl, RdfClass>()
                                   {
                                       @Override
                                       public RdfClass apply(RdfClassImpl member)
                                       {
                                           return member;
                                       }
                                   }
        );
    }
    @Override
    public Iterable<RdfProperty> getProperties()
    {
        this.assertNoProxy();

        //this does nothing more than restrict the member values to the interface
        return Iterables.transform(this._getProperties(),
                                   new Function<RdfPropertyImpl, RdfProperty>()
                                   {
                                       @Override
                                       public RdfProperty apply(RdfPropertyImpl member)
                                       {
                                           return member;
                                       }
                                   }
        );
    }
    @Override
    public boolean hasProperty(RdfProperty property)
    {
        this.assertNoProxy();

        return _hasProperty(property);
    }
    @Override
    public RdfQueryEndpoint getEndpoint()
    {
        this.assertNoProxy();

        return endpoint;
    }
    @Override
    public ResourceSummarizer getSummarizer()
    {
        this.assertNoProxy();

        return summarizer;
    }
    @Override
    public RdfProperty getMainProperty()
    {
        this.assertNoProxy();

        return mainProperty;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    /**
     * The same as getProperties(), but without the proxy check
     */
    private Iterable<RdfPropertyImpl> _getProperties()
    {
        //by constructing it this way, we can keep using the references to the (proxy) superclasses
        //until this method is really called.
        Iterable<RdfPropertyImpl> retVal = this.properties;
        for (RdfClassImpl c : this.superClasses) {
            retVal = Iterables.concat(retVal, c._getProperties());
        }

        return retVal;
    }
    /**
     * The same as hasProperty(), but without the proxy check
     */
    private boolean _hasProperty(RdfProperty property)
    {
        return Iterables.tryFind(this._getProperties(), Predicates.equalTo(property)).isPresent();
    }

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

                RdfClassImpl cImpl = (RdfClassImpl) c;

                if (this.rdfResource.superClasses.contains(c)) {
                    throw new RdfInitializationException("Can't add superclass " + c + " to class " + this + " because it would overwrite and existing superclass, please fix this.");
                }
                else {
                    this.rdfResource.superClasses.add(cImpl);
                    //also wire-in this class as a subclass of the superclass
                    //note that this cast should always work because it's defined in our generic signature
                    cImpl.subClasses.add(this.rdfResource);
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

                RdfPropertyImpl pImpl = (RdfPropertyImpl) p;

                if (this.rdfResource.properties.contains(pImpl)) {
                    throw new RdfInitializationException("Can't add properties " + p + " to class " + this + " because it would overwrite and existing properties, can't continue.");
                }
                else {
                    this.rdfResource.properties.add(pImpl);
                }
            }

            return this;
        }
        public Builder endpoint(RdfQueryEndpoint endpoint)
        {
            this.rdfResource.endpoint = endpoint;

            return this;
        }
        public Builder summarizer(ResourceSummarizer resourceSummarizer)
        {
            this.rdfResource.summarizer = resourceSummarizer;

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
            //every public class should at least have the label property, otherwise we can't set it's <title>
            if (this.rdfResource.isPublic && !this.rdfResource._hasProperty(Settings.instance().getRdfLabelProperty())) {
                throw new RdfInitializationException("Trying to create a public RDF class '" + this.rdfResource.getName() + "' without the main label property '" + Settings.instance().getRdfLabelProperty() + "'," +
                                                     " please fix this.");
            }

            //revert to default if null (this behaviour is expected in com.beligum.blocks.fs.index.entries.pages.SimplePageIndexEntry)
            if (this.rdfResource.summarizer == null) {
                this.rdfResource.summarizer = new SimpleResourceSummarizer();
            }

            //note: instead of iterating the properties of the superclasses and adding them to this class,
            //we overloaded the getProperties() method to include the properties of the superclass instead.
            //This way, we work around the need for the superclasses to be initialized when this resource is created.

            //Note: this call will add us to the ontology
            return super.create();
        }
    }
}
