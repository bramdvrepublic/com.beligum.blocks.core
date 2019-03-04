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

import com.beligum.base.filesystem.MessagesFileEntry;
import com.beligum.base.server.R;
import com.beligum.blocks.endpoints.ifaces.RdfQueryEndpoint;
import com.beligum.blocks.exceptions.RdfInitializationException;
import com.beligum.blocks.filesystem.index.entries.resources.ResourceSummarizer;
import com.beligum.blocks.filesystem.index.entries.resources.SimpleResourceSummarizer;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ifaces.RdfOntology;

import java.net.URI;
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
    protected RdfOntology ontology;
    protected MessagesFileEntry title;
    protected MessagesFileEntry label;
    protected URI[] isSameAs;
    protected RdfQueryEndpoint queryEndpoint;
    protected Set<RdfProperty> properties;
    protected ResourceSummarizer resourceSummarizer;
    protected Set<RdfClass> superClasses;
    protected RdfProperty mainProperty;

    //-----CONSTRUCTORS-----
    RdfClassImpl(String name)
    {
        super(name);

        //makes sense that the properties and superclasses are returned in the same order they are added, no?
        this.properties = new LinkedHashSet<>();
        this.superClasses = new LinkedHashSet<>();
    }

    //-----PUBLIC METHODS-----
    @Override
    public Type getType()
    {
        return Type.CLASS;
    }
    @Override
    public RdfOntology getOntology()
    {
        return ontology;
    }
    @Override
    public URI getFullName()
    {
        return this.ontology.resolve(this.getName());
    }
    @Override
    public URI getCurieName()
    {
        return URI.create(ontology.getNamespace().getPrefix() + ":" + this.getName());
    }
    @Override
    public String getTitleKey()
    {
        return title.getCanonicalKey();
    }
    @Override
    public String getTitle()
    {
        //Note: we can't return the regular optimal locale, because this will probably be called from an admin endpoint
        return this.title == null ? null : this.title.toString(R.i18n().getOptimalRefererLocale());
    }
    @Override
    public MessagesFileEntry getTitleMessage()
    {
        return title;
    }
    @Override
    public String getLabelKey()
    {
        return label.getCanonicalKey();
    }
    @Override
    public String getLabel()
    {
        //Note: we can't return the regular optimal locale, because this will probably be called from an admin endpoint
        return this.label == null ? null : this.label.toString(R.i18n().getOptimalRefererLocale());
    }
    @Override
    public MessagesFileEntry getLabelMessage()
    {
        return label;
    }
    @Override
    public URI[] getIsSameAs()
    {
        return isSameAs;
    }
    @Override
    public RdfQueryEndpoint getEndpoint()
    {
        return queryEndpoint;
    }
    @Override
    public Set<RdfProperty> getProperties()
    {
        return properties;
    }
    @Override
    public RdfProperty getMainProperty()
    {
        return this.mainProperty;
    }
    @Override
    public ResourceSummarizer getResourceSummarizer()
    {
        return resourceSummarizer;
    }
    @Override
    public void addProperties(RdfProperty... properties)
    {
        if (properties != null) {
            for (RdfProperty p : properties) {
                if (this.properties.contains(p)) {
                    throw new RdfInitializationException("Can't add property " + p + " to class " + this + " because it would overwrite and existing property, can't continue.");
                }
                else {
                    this.properties.add(p);
                }
            }
        }
    }
    @Override
    public void setMainProperty(RdfProperty property)
    {
        if (!this.properties.contains(property)) {
            throw new RdfInitializationException("Can't set main property of class " + this + " to " + property + " because it's not a property of this class.");
        }
        else {
            this.mainProperty = property;
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----
    @Override
    public String toString()
    {
        return "" + this.getCurieName();
    }
    /**
     * Note that we overload the equals() method of AbstractRdfResourceImpl to use the CURIE instead of the name
     * because two classes with the same name, but in different ontologies are not the same thing, but I guess
     * we can assume two classes (or properties or datatypes) with the same CURIE to be equal, right?
     */
    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof RdfClassImpl)) return false;

        RdfClassImpl rdfClass = (RdfClassImpl) o;

        return getCurieName() != null ? getCurieName().equals(rdfClass.getCurieName()) : rdfClass.getCurieName() == null;
    }
    @Override
    public int hashCode()
    {
        return getCurieName() != null ? getCurieName().hashCode() : 0;
    }

    //-----INNER CLASSES-----
    public static class Builder extends AbstractRdfOntologyMember.Builder<RdfClass, RdfClassImpl, RdfClassImpl.Builder>
    {
        Builder(RdfFactory rdfFactory, RdfClassImpl rdfClass)
        {
            super(rdfFactory, rdfClass);
        }

        @Override
        public RdfClass create()
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

            //only add ourself to the selected ontology if we are a pure class
            if (this.rdfResource.ontology != null && this.rdfResource.getType().equals(Type.CLASS)) {
                this.rdfResource.ontology.addClass(this.rdfResource);
            }

            return super.create();
        }
        public Builder queryEndpoint(RdfQueryEndpoint queryEndpoint)
        {
            this.rdfResource.queryEndpoint = queryEndpoint;

            return this;
        }
        public Builder properties(Set<RdfProperty> properties)
        {
            this.rdfResource.properties = properties;

            return this;
        }
        public Builder resourceSummarizer(ResourceSummarizer resourceSummarizer)
        {
            this.rdfResource.resourceSummarizer = resourceSummarizer;

            return this;
        }
        public Builder superClasses(Set<RdfClass> superClasses)
        {
            this.rdfResource.superClasses = superClasses;

            return this;
        }
    }
}
