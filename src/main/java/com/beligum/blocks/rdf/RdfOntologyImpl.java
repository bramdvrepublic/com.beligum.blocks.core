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
import com.beligum.blocks.rdf.ifaces.*;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by bram on 2/28/16.
 */
public abstract class RdfOntologyImpl extends AbstractRdfResourceImpl implements RdfOntology
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private Map<URI, RdfOntologyMember> allMembers;
    private Map<URI, RdfClass> allClasses;
    private Map<URI, RdfClass> publicClasses;
    private Map<URI, RdfProperty> allProperties;
    private Map<URI, RdfProperty> publicProperties;
    private Map<URI, RdfProperty> allClassProperties;
    private Map<URI, RdfProperty> publicClassProperties;

    //-----CONSTRUCTORS-----
    /**
     * Don't use this constructor, it's only meant as a convenience constructor so all subclasses are not obliged to create an empty constructor
     */
    protected RdfOntologyImpl()
    {
    }
    /**
     * Called from RdfFactory during initialization
     */
    RdfOntologyImpl(RdfFactory rdfFactory) throws RdfInitializationException
    {
        //now we're created, store a reference in the factory
        rdfFactory.ontology = this;

        this.allMembers = new LinkedHashMap<>();
        this.allClasses = new LinkedHashMap<>();
        this.publicClasses = new LinkedHashMap<>();
        this.allProperties = new LinkedHashMap<>();
        this.publicProperties = new LinkedHashMap<>();
        this.allClassProperties = new LinkedHashMap<>();
        this.publicClassProperties = new LinkedHashMap<>();

        //this call should initialize all member fields
        this.create(rdfFactory);

        // now loop through all members that were created during the scope of the last create()
        // to do some auto post-initialization
        for (AbstractRdfOntologyMember.Builder m : rdfFactory.registry) {
            //if the caller refrained from calling create(), do it here
            if (m.rdfResource.isProxy()) {
                m.create();
            }
        }

        // It's easy for the create() method to miss some fields, so let's help the dev
        // a little bit by iterating all members of the ontology and check if they have been
        // registered properly
        try {
            for (Field field : this.getClass().getFields()) {
                //should we also check for a static modifier here?
                if (field.getType().isAssignableFrom(RdfOntologyMember.class)) {
                    RdfOntologyMember member = (RdfOntologyMember) field.get(this);
                    if (member == null) {
                        throw new RdfInitializationException("Field inside an RDF ontology turned out null after initializing the ontology; this shouldn't happen; " + this);
                    }
                    else if (member.isProxy()) {
                        throw new RdfInitializationException("Field inside an RDF ontology turned out to be still a proxy after initializing the ontology; this shouldn't happen; " + this);
                    }
                }
            }
        }
        catch (Exception e) {
            throw new RdfInitializationException("Error happened while validating the members of an ontology; " + this, e);
        }
    }

    //-----PUBLIC METHODS-----
    /**
     * Explicitly re-enabled from the interface because it's important:
     * only public interfaces will be stored in the lookup map during boot.
     * Non-public ones are only meant to be referenced from other ontologies
     * or used directly (like the Log ontology)
     */
    @Override
    public abstract boolean isPublic();

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
    public URI resolve(String suffix)
    {
        //note: we can't use namespace.resolve(suffix) because it doesn't resolve anchor-based ontologies correctly
        // (like "http://www.geonames.org/ontology#" + "name" yields "http://www.geonames.org/name" )
        return URI.create(this.getNamespace().getUri().toString() + suffix);
    }
    @Override
    public URI resolveCurie(String suffix)
    {
        return URI.create(this.getNamespace().getPrefix() + ":" + suffix);
    }
    @Override
    public Map<URI, RdfOntologyMember> getAllMembers()
    {
        return allMembers;
    }
    @Override
    public Map<URI, RdfClass> getAllClasses()
    {
        return allClasses;
    }
    @Override
    public Map<URI, RdfClass> getPublicClasses()
    {
        return publicClasses;
    }
    @Override
    public Map<URI, RdfProperty> getAllClassProperties()
    {
        return allClassProperties;
    }
    @Override
    public Map<URI, RdfProperty> getPublicClassProperties()
    {
        return publicClassProperties;
    }
    @Override
    public Map<URI, RdfProperty> getAllProperties()
    {
        return allProperties;
    }
    @Override
    public Map<URI, RdfProperty> getPublicProperties()
    {
        return publicProperties;
    }
    /**
     * Register an ontology member into this ontology, putting it into the relevant maps.
     * Note that this method is also used to merge another duplicate ontology into this one.
     */
    @Override
    public void _register(RdfOntologyMember member) throws RdfInitializationException
    {
        //first, make sure to register the resource into the set of all members
        this.allMembers.put(member.getCurieName(), member);

        //then, fill some specific lookup maps and check for errors in the mean time
        switch (this.getType()) {
            case ONTOLOGY:
                throw new RdfInitializationException("Encountered a sub-ontology; since RdfOntology doesn't implement RdfOntologyMember, this shoudln't happen; please fix this; " + member);
            case CLASS:
                RdfClass rdfClass = (RdfClass) member;
                this.allClasses.put(rdfClass.getCurieName(), rdfClass);
                if (rdfClass.isPublic()) {
                    this.publicClasses.put(rdfClass.getCurieName(), rdfClass);
                }

                for (RdfProperty p : rdfClass.getProperties()) {
                    this.allClassProperties.put(p.getCurieName(), p);
                    //note that the public selection is made on class level, not property level,
                    //so it's possible to "expose" properties by adding them to a public class
                    if (rdfClass.isPublic()) {
                        this.publicClassProperties.put(p.getCurieName(), p);
                    }
                }

                break;
            case PROPERTY:
                RdfProperty rdfProperty = (RdfProperty) member;
                this.allProperties.put(rdfProperty.getCurieName(), rdfProperty);
                if (rdfProperty.isPublic()) {
                    this.publicProperties.put(rdfProperty.getCurieName(), rdfProperty);
                }
                break;
            case DATATYPE:
                //NOOP: datatypes are hooked into the properties, no need to store them separately
                break;
            default:
                throw new RdfInitializationException("Encountered unimplemented RDF resource type; please fix this; " + member);
        }
    }

    //-----PROTECTED METHODS-----
    /**
     * This is called when an instance of this ontology is created and it's members need to be filled in.
     * Before calling this method, the members are just static stubs for convenient referencing. After this method is done,
     * they need to be filled with meaningful entries.
     * Note that the name of this method is more or less chosen to be in sync with AbstractRdfResourceImpl.Builder.create()
     */
    protected abstract void create(RdfFactory rdfFactory) throws RdfInitializationException;

    //-----PRIVATE METHODS-----

}
