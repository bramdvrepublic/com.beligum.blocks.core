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
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * Created by bram on 2/28/16.
 */
public abstract class RdfOntologyImpl extends AbstractRdfResourceImpl implements RdfOntology
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private Collection<RdfResource> members;

    private final Map<URI, RdfResource> allTypes;
    private final Map<URI, RdfClass> allClasses;
    private final Map<URI, RdfClass> publicClasses;
    private final Map<URI, RdfDataType> allDataTypes;
    private final Map<URI, RdfDataType> publicDataTypes;
    private final Map<URI, RdfProperty> allProperties;
    private final Map<URI, RdfProperty> publicProperties;

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
        //this call should initialize all member fields
        this.create(rdfFactory);

        // we give a chance to initialize() method to fill the members collection,
        // but if it didn't do that, we'll iterate the fields of this ontology here
        // to build a list of all rdf resources using reflection and heuristics.
        if (this.members == null) {
            try {
                this.members = new LinkedHashSet<>();
                for (Field field : this.getClass().getFields()) {
                    if (field.getType().isAssignableFrom(RdfResource.class)) {
                        RdfResource member = (RdfResource) field.get(this);
                        if (member == null) {
                            throw new RdfInitializationException("Field inside an RDF ontology turned out null after initializing the ontology; this shouldn't happen; " + this);
                        }
                        else {
                            //first, make sure to initialize the list
                            this.members.add(member);

                            //then, fill some maps for later use
                            switch (member.getType()) {
                                case ONTOLOGY:
                                    throw new RdfInitializationException("Encountered a sub-ontology; this is not supported yet, please fix this; " + member);
                                case CLASS:
                                    break;
                                case PROPERTY:
                                    break;
                                case DATATYPE:
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

        //        this.allTypes = new HashMap<>();
        //        this.allClasses = new HashMap<>();
        //        this.publicClasses = new HashMap<>();
        //        this.allDataTypes = new HashMap<>();
        //        this.publicDataTypes = new HashMap<>();
        //        this.allProperties = new HashMap<>();
        //        this.publicProperties = new HashMap<>();
        //
        //        //add this vocabulary to the cached map of vocabularies
        //        RdfFactory.getOntologies().put(this.getNamespace(), this);
        //        //store the prefix mapping
        //        RdfFactory.getOntologyPrefixes().put(this.getPrefix(), this.getNamespace());
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
    public Collection<RdfResource> getMembers()
    {
        return this.members;
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
    public Map<URI, RdfResource> getAllTypes()
    {
        return allTypes;
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
    public void addClass(RdfClass rdfClass)
    {
        this.allTypes.put(rdfClass.getCurieName(), rdfClass);

        this.allClasses.put(rdfClass.getCurieName(), rdfClass);
        if (rdfClass.isPublic()) {
            this.publicClasses.put(rdfClass.getCurieName(), rdfClass);
        }
    }
    @Override
    public void addProperty(RdfProperty rdfProperty)
    {
        this.allTypes.put(rdfProperty.getCurieName(), rdfProperty);

        this.allProperties.put(rdfProperty.getCurieName(), rdfProperty);
        if (rdfProperty.isPublic()) {
            this.publicProperties.put(rdfProperty.getCurieName(), rdfProperty);
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
