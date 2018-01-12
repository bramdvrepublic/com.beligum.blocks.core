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

package com.beligum.blocks.rdf.ontology;

import com.beligum.base.filesystem.MessagesFileEntry;
import com.beligum.base.server.R;
import com.beligum.blocks.endpoints.ifaces.RdfQueryEndpoint;
import com.beligum.blocks.exceptions.RdfInitializationException;
import com.beligum.blocks.filesystem.index.entries.resources.ResourceIndexer;
import com.beligum.blocks.filesystem.index.entries.resources.SimpleResourceIndexer;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ifaces.RdfVocabulary;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by bram on 2/27/16.
 */
public class RdfClassImpl extends AbstractRdfResourceImpl implements RdfClass
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private String name;
    private RdfVocabulary vocabulary;
    private MessagesFileEntry title;
    private MessagesFileEntry label;
    private URI[] isSameAs;
    //we need to be able to set this from the RdfProperty interface (to make static initializing possible)
    protected RdfQueryEndpoint queryEndpoint;
    private Set<RdfProperty> properties;
    private ResourceIndexer resourceIndexer;
    private Set<RdfClass> superClasses;

    //-----CONSTRUCTORS-----
    public RdfClassImpl(String name,
                        RdfVocabulary vocabulary,
                        MessagesFileEntry title,
                        MessagesFileEntry label)
    {
        this(name, vocabulary, title, label, null, false, null, null);
    }
    public RdfClassImpl(String name,
                        RdfVocabulary vocabulary,
                        MessagesFileEntry title,
                        MessagesFileEntry label,
                        URI[] isSameAs)
    {
        this(name, vocabulary, title, label, isSameAs, false, null, null);
    }
    public RdfClassImpl(String name,
                        RdfVocabulary vocabulary,
                        MessagesFileEntry title,
                        MessagesFileEntry label,
                        URI[] isSameAs,
                        boolean isPublic,
                        RdfQueryEndpoint queryEndpoint)
    {
        this(name, vocabulary, title, label, isSameAs, isPublic, queryEndpoint, null);
    }
    public RdfClassImpl(String name,
                        RdfVocabulary vocabulary,
                        MessagesFileEntry title,
                        MessagesFileEntry label,
                        URI[] isSameAs,
                        boolean isPublic,
                        RdfQueryEndpoint queryEndpoint,
                        ResourceIndexer resourceIndexer)
    {
        this(name, vocabulary, title, label, isSameAs, isPublic, queryEndpoint, resourceIndexer, null);
    }
    public RdfClassImpl(String name,
                        RdfVocabulary vocabulary,
                        MessagesFileEntry title,
                        MessagesFileEntry label,
                        URI[] isSameAs,
                        boolean isPublic,
                        RdfQueryEndpoint queryEndpoint,
                        ResourceIndexer resourceIndexer,
                        RdfClass... superClasses)
    {
        super(isPublic);

        this.name = name;
        this.vocabulary = vocabulary;
        if (this.vocabulary == null) {
            throw new RdfInitializationException("Trying to create an RdfClass '" + this.name + "' without any vocabulary to connect to, can't continue because too much depends on this.");
        }

        this.title = title;
        this.label = label;
        //make it uniform (always an array)
        this.isSameAs = isSameAs == null ? new URI[] {} : isSameAs;
        this.queryEndpoint = queryEndpoint;
        this.resourceIndexer = resourceIndexer;
        //revert to default if null (this behaviour is expected in com.beligum.blocks.fs.index.entries.pages.SimplePageIndexEntry)
        if (this.resourceIndexer == null) {
            this.resourceIndexer = new SimpleResourceIndexer();
        }

        //makes sense the properties are returned in the same order they are added, no?
        this.properties = new LinkedHashSet<>();

        this.superClasses = new LinkedHashSet<>();
        if (superClasses != null) {
            for (RdfClass c : superClasses) {
                //we save the relationship and add all properties of the superclasses to this class
                this.superClasses.add(c);
                for (RdfProperty p : c.getProperties()) {
                    if (this.properties.contains(p)) {
                        throw new RdfInitializationException("RDFClass " + this + " inherits from " + c + ", but the property " + p + " would overwrite an existing property, can't continue.");
                    }
                    else {
                        this.properties.add(p);
                    }
                }
            }
        }

        //only add ourself to the selected vocabulary if we are a pure class
        if (this.vocabulary != null && this.getType().equals(Type.CLASS)) {
            this.vocabulary.addClass(this);
        }
    }

    //-----PUBLIC METHODS-----
    @Override
    public Type getType()
    {
        return Type.CLASS;
    }
    @Override
    public String getName()
    {
        return name;
    }
    @Override
    public RdfVocabulary getVocabulary()
    {
        return vocabulary;
    }
    @Override
    public URI getFullName()
    {
        return this.vocabulary == null ? null : vocabulary.resolve(name);
    }
    @Override
    public URI getCurieName()
    {
        return this.vocabulary == null ? null : URI.create(vocabulary.getPrefix() + ":" + name);
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
    public ResourceIndexer getResourceIndexer()
    {
        return resourceIndexer;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----
    @Override
    public String toString()
    {
        return "" + this.getCurieName();
    }
    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof RdfClassImpl)) return false;

        RdfClassImpl rdfClass = (RdfClassImpl) o;

        //I guess we can assume two classes (or properties) with the same CURIE to be equal, right?
        return getCurieName() != null ? getCurieName().equals(rdfClass.getCurieName()) : rdfClass.getCurieName() == null;
    }
    @Override
    public int hashCode()
    {
        return getCurieName() != null ? getCurieName().hashCode() : 0;
    }
}
