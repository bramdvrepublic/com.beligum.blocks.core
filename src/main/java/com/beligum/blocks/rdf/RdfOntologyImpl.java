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

import com.beligum.blocks.exceptions.RdfInitializationException;
import com.beligum.blocks.exceptions.RdfInstantiationException;
import com.beligum.blocks.rdf.ifaces.*;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;

import java.net.URI;
import java.util.*;

/**
 * Created by bram on 2/28/16.
 */
public abstract class RdfOntologyImpl extends AbstractRdfResourceImpl implements RdfOntology
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    protected Map<String, AbstractRdfOntologyMember> allMembers;
    protected Map<URI, RdfClass> allClasses;
    protected Map<URI, RdfClass> publicClasses;
    protected Map<URI, RdfProperty> allProperties;
    protected Map<URI, RdfProperty> publicProperties;
    protected Map<URI, RdfProperty> allClassProperties;
    protected Map<URI, RdfProperty> publicClassProperties;

    //-----CONSTRUCTORS-----
    /**
     * Don't use this constructor directly, it's called automatically from RdfFactory and is also meant as a convenience constructor
     * so all subclasses are not obliged to create an empty constructor.
     * Note that the only way to not to force the subclasses to create a mandatory constructor is to have a default constructor
     * and an extra method (see below) that's called after the default (private) constructor was invoked.
     */
    protected RdfOntologyImpl()
    {
        if (this.getNamespace() == null) {
            throw new RdfInstantiationException("Trying to create an RDF ontology without a namespace, can't continue because too much depends on this; " + this.getClass());
        }

        //save the value of the protected method to a variable so we can override it later on
        this.isPublic = this.isPublicOntology();

        this.allMembers = new LinkedHashMap<>();
        this.allClasses = new LinkedHashMap<>();
        this.publicClasses = new LinkedHashMap<>();
        this.allProperties = new LinkedHashMap<>();
        this.publicProperties = new LinkedHashMap<>();
        this.allClassProperties = new LinkedHashMap<>();
        this.publicClassProperties = new LinkedHashMap<>();
    }

    //-----PUBLIC METHODS-----
    @Override
    public Type getType()
    {
        return Type.ONTOLOGY;
    }
    //We'll overload the name method to return the full URI of the namespace, which is the only true identifier of an ontology
    @Override
    public String getName()
    {
        return this.getNamespace().getUri().toString();
    }
    @Override
    public URI getUri()
    {
        return this.getNamespace().getUri();
    }
    @Override
    public URI getCurie()
    {
        //don't really know what to return here because it doesn't really make sense...
        //might change in the future
        return null;
    }
    @Override
    public boolean isPublic()
    {
        return this.isPublic;
    }
    @Override
    public URI resolve(String suffix)
    {
        //note: we can't use namespace.resolve(suffix) because it doesn't resolve anchor-based ontologies correctly
        // (like "http://www.geonames.org/ontology#" + "name" yields "http://www.geonames.org/name" )
        return URI.create(this.getNamespace().getUri().toString() + suffix);
    }
    @Override
    public URI resolveCurie(String suffix)
    {
        return URI.create(this.resolveCurieString(suffix));
    }
    @Override
    public Iterable<RdfOntologyMember> getAllMembers()
    {
        //this does nothing more than restrict the member values to the interface
        return Iterables.transform(this.allMembers.values(),
                                   new Function<AbstractRdfOntologyMember, RdfOntologyMember>()
                                   {
                                       @Override
                                       public RdfOntologyMember apply(AbstractRdfOntologyMember member)
                                       {
                                           return member;
                                       }
                                   }
        );
    }
    @Override
    public RdfOntologyMember getMember(String name)
    {
        return allMembers.get(name);
    }
    @Override
    public Iterable<RdfClass> getAllClasses()
    {
        return allClasses.values();
    }
    @Override
    public Iterable<RdfClass> getPublicClasses()
    {
        return publicClasses.values();
    }
    @Override
    public Iterable<RdfProperty> getAllClassProperties()
    {
        return allClassProperties.values();
    }
    @Override
    public Iterable<RdfProperty> getPublicClassProperties()
    {
        return publicClassProperties.values();
    }
    @Override
    public Iterable<RdfProperty> getAllProperties()
    {
        return allProperties.values();
    }
    @Override
    public Iterable<RdfProperty> getPublicProperties()
    {
        return publicProperties.values();
    }

    //-----PROTECTED METHODS-----
    /**
     * This is called when an instance of this ontology is created and it's members need to be filled in.
     * Before calling this method, the members are just static stubs for convenient referencing. After this method is done,
     * they need to be filled with meaningful entries.
     * Note that the name of this method is more or less chosen to be in sync with AbstractRdfResourceImpl.Builder.create()
     */
    protected abstract void create(RdfFactory rdfFactory) throws RdfInitializationException;

    /**
     * Only public interfaces will be stored in the lookup map during boot.
     * Non-public ones are only meant to be referenced from other ontologies
     * or used directly (like the Log ontology)
     */
    protected abstract boolean isPublicOntology();

    /**
     * Register an ontology member into this ontology, putting it into the relevant maps.
     * Note that this method is also used to merge another duplicate ontology into this one.
     */
    void _register(AbstractRdfOntologyMember member) throws RdfInitializationException
    {
        //always make sure the ontology in the member points to this instance
        if (!member.ontology.equals(this)) {
            throw new RdfInitializationException("Encountered a registration of an ontology member that doesn't belong to this ontology; " + member);
        }

        //first, make sure to register the resource into the set of all members
        this.allMembers.put(member.getName(), member);

        //then, fill some specific lookup maps and check for errors in the mean time
        switch (member.getType()) {
            case ONTOLOGY:
                throw new RdfInitializationException("Encountered a sub-ontology; since RdfOntology doesn't implement RdfOntologyMember, this shoudln't happen; please fix this; " + member);
            case CLASS:
                RdfClass rdfClass = (RdfClass) member;
                this.allClasses.put(rdfClass.getCurie(), rdfClass);
                if (rdfClass.isPublic()) {
                    this.publicClasses.put(rdfClass.getCurie(), rdfClass);
                }

                for (RdfProperty p : rdfClass.getProperties()) {
                    this.allClassProperties.put(p.getCurie(), p);
                    //note that the public selection is made on class level, not property level,
                    //so it's possible to "expose" properties by adding them to a public class
                    if (rdfClass.isPublic()) {
                        this.publicClassProperties.put(p.getCurie(), p);
                    }
                }

                break;
            case PROPERTY:
                RdfProperty rdfProperty = (RdfProperty) member;
                this.allProperties.put(rdfProperty.getCurie(), rdfProperty);
                if (rdfProperty.isPublic()) {
                    this.publicProperties.put(rdfProperty.getCurie(), rdfProperty);
                }
                break;
            case DATATYPE:
                //NOOP: datatypes are hooked into the properties, no need to store them separately
                break;
            default:
                throw new RdfInitializationException("Encountered unimplemented RDF resource type; please fix this; " + member);
        }
    }

    /**
     * Uniform setter to change the visibility of this ontology in the RdfFactory
     */
    protected void setPublic(boolean isPublic)
    {
        this.isPublic = isPublic;
    }

    //-----PRIVATE METHODS-----
    private String resolveCurieString(String suffix)
    {
        return this.getNamespace().getPrefix() + ":" + suffix;
    }

    //-----PRIVATE CLASSES-----
    static abstract class Visitor
    {
        private Set<RdfResource> visitedResources;

        Visitor()
        {
            this.visitedResources = new HashSet<>();
        }

        public void processOntology(RdfOntologyImpl ontology) throws RdfInitializationException
        {
            if (!this.visitedResources.contains(ontology)) {
                //first of all, register the ontology itself
                this.register(ontology);

                //now iterate all ontology members and request them to register their ontologies too
                for (AbstractRdfOntologyMember m : ontology.allMembers.values()) {
                    //note: we could have implemented this as a polymorphic method in the classes,
                    //but this way, we centralize everything here, which is clearer
                    switch (m.getType()) {
                        case CLASS:
                            this.processClass((RdfClassImpl) m);
                            break;
                        case DATATYPE:
                            this.processDatatype((RdfDatatypeImpl) m);
                            break;
                        case PROPERTY:
                            this.processProperty((RdfPropertyImpl) m);
                            break;
                        default:
                            throw new RdfInitializationException("Encountered unimplemented ontology member type; " + m);
                    }
                }
            }
        }

        protected abstract void foundNew(RdfOntology rdfOntology);

        private void processMember(AbstractRdfOntologyMember member) throws RdfInitializationException
        {
            if (!this.visitedResources.contains(member)) {

                //don't forget to register the member itself, it will avoid recursive calls in cyclic dependencies
                this.register(member);

                //now processClass the ontology of the member
                this.processOntology(member.ontology);
            }
        }
        private void processClass(RdfClassImpl clazz) throws RdfInitializationException
        {
            if (!this.visitedResources.contains(clazz)) {

                this.processMember(clazz);

                for (RdfClassImpl c : clazz.superClasses) {
                    this.processClass(c);
                }
                for (RdfPropertyImpl p : clazz.properties) {
                    this.processProperty(p);
                }
            }
        }
        private void processDatatype(RdfDatatypeImpl datatype) throws RdfInitializationException
        {
            this.processClass(datatype);
        }
        private void processProperty(RdfPropertyImpl property) throws RdfInitializationException
        {
            if (!this.visitedResources.contains(property)) {
                this.processMember(property);
                this.processClass(property.dataType);
            }
        }
        private void register(RdfResource resource)
        {
            if (!this.visitedResources.contains(resource)) {

                this.visitedResources.add(resource);

                if (resource instanceof RdfOntology) {
                    this.foundNew((RdfOntology) resource);
                }
            }
        }
    }
}
