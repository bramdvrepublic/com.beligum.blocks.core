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

package com.beligum.blocks.rdf.ontology.vocabularies;

import com.beligum.base.database.models.AbstractJsonObject;
import com.beligum.blocks.config.RdfFactory;
import com.beligum.blocks.rdf.ifaces.*;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by bram on 2/28/16.
 */
public abstract class AbstractRdfVocabulary extends AbstractJsonObject implements RdfVocabulary
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private final URI namespace;
    private final String prefix;
    private final Map<URI, RdfResource> allTypes;
    private final Map<URI, RdfClass> allClasses;
    private final Map<URI, RdfClass> publicClasses;
    private final Map<URI, RdfDataType> allDataTypes;
    private final Map<URI, RdfDataType> publicDataTypes;
    private final Map<URI, RdfProperty> allProperties;
    private final Map<URI, RdfProperty> publicProperties;
    private final Set<RdfLiteral> allLiterals;
    private final Set<RdfLiteral> publicLiterals;

    //-----CONSTRUCTORS-----
    protected AbstractRdfVocabulary(URI namespace, String prefix)
    {
        this.namespace = namespace;
        this.prefix = prefix;
        this.allTypes = new HashMap<>();
        this.allClasses = new HashMap<>();
        this.publicClasses = new HashMap<>();
        this.allDataTypes = new HashMap<>();
        this.publicDataTypes = new HashMap<>();
        this.allProperties = new HashMap<>();
        this.publicProperties = new HashMap<>();
        this.allLiterals = new HashSet<>();
        this.publicLiterals = new HashSet<>();

        //add this vocabulary to the cached map of vocabularies
        RdfFactory.getVocabularies().put(this.getNamespace(), this);
        //store the prefix mapping
        RdfFactory.getVocabularyPrefixes().put(this.getPrefix(), this.getNamespace());
    }

    //-----PUBLIC METHODS-----
    @Override
    public URI getNamespace()
    {
        return namespace;
    }
    @Override
    public URI resolve(String suffix)
    {
        //note: we can't use namespace.resolve(suffix) because it doesn't resolve anchor-based ontologies correctly
        // (like "http://www.geonames.org/ontology#" + "name" yields "http://www.geonames.org/name" )
        return namespace == null ? null : URI.create(namespace.toString() + suffix);
    }
    @Override
    public final String getPrefix()
    {
        return prefix;
    }
    @Override
    public URI resolveCurie(String suffix)
    {
        return URI.create(this.prefix + ":" + suffix);
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
    public Set<RdfLiteral> getAllLiterals()
    {
        return allLiterals;
    }
    @Override
    public Set<RdfLiteral> getPublicLiterals()
    {
        return publicLiterals;
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
    @Override
    public void addDataType(RdfDataType rdfDataType)
    {
        this.allTypes.put(rdfDataType.getCurieName(), rdfDataType);

        this.allDataTypes.put(rdfDataType.getCurieName(), rdfDataType);
        if (rdfDataType.isPublic()) {
            this.publicDataTypes.put(rdfDataType.getCurieName(), rdfDataType);
        }
    }
    @Override
    public void addLiteral(RdfLiteral rdfLiteral)
    {
        this.allLiterals.add(rdfLiteral);
        if (rdfLiteral.isPublic()) {
            this.publicLiterals.add(rdfLiteral);
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
