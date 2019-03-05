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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
    private Map<URI, RdfDataType> allDataTypes;
    private Map<URI, RdfDataType> publicDataTypes;
    private Map<URI, RdfProperty> allProperties;
    private Map<URI, RdfProperty> publicProperties;

    //-----CONSTRUCTORS-----
    /**
     * Don't use this constructor, it's only meant as a convenience constructor so all subclasses are not obliged to create an empty constructor
     */
    protected RdfOntologyImpl()
    {
        throw new RdfInitializationException("The empty constructor is a convenience-only method, this shouldn't happen; " + this);
    }
    /**
     * Called from RdfFactory during initialization
     */
    RdfOntologyImpl(RdfFactory rdfFactory)
    {
        this.allMembers = new LinkedHashMap<>();
        this.allClasses = new LinkedHashMap<>();
        this.publicClasses = new LinkedHashMap<>();
        this.allProperties = new LinkedHashMap<>();
        this.publicProperties = new LinkedHashMap<>();
        this.allDataTypes = new LinkedHashMap<>();
        this.publicDataTypes = new LinkedHashMap<>();

        //this call should initialize all member fields
        this.create(rdfFactory);

        // we give a chance to initialize() method to fill the members collection,
        // but if it didn't do that, we'll iterate the fields of this ontology here
        // to build a list of all rdf resources using reflection and heuristics.
        if (this.allMembers.isEmpty()) {
            try {
                for (Field field : this.getClass().getFields()) {
                    if (field.getType().isAssignableFrom(RdfOntologyMember.class)) {
                        RdfOntologyMember member = (RdfOntologyMember) field.get(this);
                        if (member == null) {
                            throw new RdfInitializationException("Field inside an RDF ontology turned out null after initializing the ontology; this shouldn't happen; " + this);
                        }
                        else {
                            //first, make sure to initialize the list
                            this.allMembers.put(member.getCurieName(), member);

                            //then, fill some maps for later use
                            switch (member.getType()) {
                                case ONTOLOGY:
                                    throw new RdfInitializationException("Encountered a sub-ontology; this is not supported yet, please fix this; " + member);
                                case CLASS:
                                    RdfClass rdfClass = (RdfClass) member;
                                    this.allClasses.put(rdfClass.getCurieName(), rdfClass);
                                    if (rdfClass.isPublic()) {
                                        this.publicClasses.put(rdfClass.getCurieName(), rdfClass);
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
                                    RdfDataType rdfDataType = (RdfDataType) member;
                                    this.allDataTypes.put(rdfDataType.getCurieName(), rdfDataType);
                                    if (rdfDataType.isPublic()) {
                                        this.publicDataTypes.put(rdfDataType.getCurieName(), rdfDataType);
                                    }
                                    break;
                                default:
                                    throw new RdfInitializationException("Encountered unimplemented RDF resource type; please fix this; " + member);
                            }
                        }
                    }
                }
            }
            catch (Exception e) {
                throw new RdfInitializationException("Error happened while auto-iterating the members of an ontology; " + this, e);
            }
        }
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
    public Map<URI, RdfDataType> getAllDataTypes()
    {
        return allDataTypes;
    }
    @Override
    public Map<URI, RdfDataType> getPublicDataTypes()
    {
        return publicDataTypes;
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
    @Override
    public void register(RdfFactory rdfFactory, RdfOntologyMember member)
    {
        //first, make sure to register the resource into the set of all members
        this.allMembers.put(member.getCurieName(), member);

        //then, fill some specific lookup maps and check for errors in the mean time
        switch (this.getType()) {
            case CLASS:
                RdfClass rdfClass = (RdfClass) member;
                this.allClasses.put(rdfClass.getCurieName(), rdfClass);
                if (rdfClass.isPublic()) {
                    this.publicClasses.put(rdfClass.getCurieName(), rdfClass);
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
                RdfDataType rdfDataType = (RdfDataType) member;
                this.allDataTypes.put(rdfDataType.getCurieName(), rdfDataType);
                if (rdfDataType.isPublic()) {
                    this.publicDataTypes.put(rdfDataType.getCurieName(), rdfDataType);
                }
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
    protected abstract void create(RdfFactory rdfFactory);

    //-----PRIVATE METHODS-----

}
