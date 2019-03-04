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

import com.beligum.base.cache.CacheFunction;
import com.beligum.base.cache.CacheKey;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.base.utils.toolkit.ReflectionFunctions;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.endpoints.ifaces.RdfQueryEndpoint;
import com.beligum.blocks.exceptions.RdfInitializationException;
import com.beligum.blocks.rdf.ifaces.*;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by bram on 2/26/16.
 */
public class RdfFactory
{
    //-----CONSTANTS-----
    private enum RdfMapCacheKey
    {
        LOCAL_PUBLIC_CLASSES,
        LOCAL_PUBLIC_CLASS_PROPERTIES,
    }

    //-----VARIABLES-----
    private static RdfFactory instance = null;

    //-----CONSTRUCTORS-----
    /**
     * This private constructor will function as a 'lock' that needs to be passed to the RDF initialization methods
     * and assures those can only be constructed from the assertInitialized() in this class.
     */
    private RdfFactory()
    {
    }

    //-----STATIC METHODS-----
    public static RdfOntology getOntologyForPrefix(String prefix)
    {
        //make sure we booted the static members at least once
        assertInitialized();

        return getOntologyPrefixMap().get(prefix);
    }
    public static RdfClass getClassForResourceType(String resourceTypeCurieStr)
    {
        RdfClass retVal = null;

        if (!StringUtils.isEmpty(resourceTypeCurieStr)) {
            try {
                retVal = getClassForResourceType(resourceTypeCurieStr);
            }
            catch (Exception e) {
                Logger.debug("Couldn't parse the supplied resource type curie to a valid URI", e);
            }
        }

        return retVal;
    }
    public static RdfClass getClassForResourceType(URI resourceTypeCurie)
    {
        RdfResource retVal = getForResourceType(resourceTypeCurie);

        return retVal != null && retVal instanceof RdfClass ? (RdfClass) retVal : null;
    }
    public static RdfResource getForResourceType(URI resourceTypeCurie)
    {
        //make sure we booted the static members at least once
        assertInitialized();

        RdfResource retVal = null;

        RdfOntology ontology = getOntologyForPrefix(resourceTypeCurie.getScheme());
        if (ontology != null) {
            //note: We search in all classes (difference between public and non-public classes is that the public classes are exposed to the client as selectable as a page-type).
            //      Since we also want to look up a value (eg. with the innner Geonames endpoint), we allow all classes to be searched.
            retVal = ontology.getAllTypes().get(resourceTypeCurie);
        }

        return retVal;
    }
    /**
     * Returns the endpoint attached to the RDF class with the supplied curie or null if nothing was found.
     */
    public static RdfQueryEndpoint getEndpointForResourceType(URI resourceTypeCurie)
    {
        //make sure we booted the static members at least once
        assertInitialized();

        RdfQueryEndpoint retVal = null;

        RdfClass rdfClass = (RdfClass) RdfFactory.getForResourceType(resourceTypeCurie);
        if (rdfClass != null) {
            retVal = rdfClass.getEndpoint();
        }

        return retVal;
    }
//    /**
//     * Returns all public classes in the local ontology
//     */
//    public static Set<RdfClass> getLocalPublicClasses()
//    {
//        return getLocalRdfMapCache(RdfMapCacheKey.LOCAL_PUBLIC_CLASSES, RdfClass.class);
//    }
//    /**
//     * Returns all properties (so not only the public ones) across all public classes in the local ontology
//     */
//    public static Set<RdfProperty> getLocalPublicClassProperties()
//    {
//        return getLocalRdfMapCache(RdfMapCacheKey.LOCAL_PUBLIC_CLASS_PROPERTIES, RdfProperty.class);
//    }
    /**
     * This will instantiate all ontologies once if needed, so we can be sure every RDF member has been assigned to it's proper ontology, etc.
     */
    public static void assertInitialized()
    {
        //speedy synchronization
        if (instance == null) {
            synchronized (RdfFactory.class) {
                if (instance == null) {

                    instance = new RdfFactory();
                    for (Class<? extends RdfOntology> c : ReflectionFunctions.searchAllClassesImplementing(RdfOntology.class, true)) {
                        try {
                            //passing an instance of RdfFactory (note the private constructor) to the constructor, assures other developers won't be able to
                            //create RDF ontology instances manually
                            RdfOntology rdfOntology = c.getConstructor(instance.getClass()).newInstance(instance);

                            merge and save local

                            //store the ontology in a lookup map
                            RdfFactory.getOntologyMap().put(rdfOntology.getNamespace().getUri(), rdfOntology);
                            RdfFactory.getOntologyPrefixMap().put(rdfOntology.getNamespace().getPrefix(), rdfOntology);
                        }
                        catch (Exception e) {
                            throw new RuntimeException("Error while instantiating an RDF resource factory, this shouldn't happen; " + c, e);
                        }
                    }
                }
            }
        }
    }

    //-----PUBLIC FACTORY METHODS-----
    public RdfClassImpl.Builder newClass(String name)
    {
        return new RdfClassImpl.Builder(this, new RdfClassImpl(name));
    }
    public RdfPropertyImpl.Builder newProperty(String name)
    {
        return new RdfPropertyImpl.Builder(this, new RdfPropertyImpl(name));
    }
    public RdfDataTypeImpl.Builder newDatatype(String name)
    {
        return new RdfDataTypeImpl.Builder(this, new RdfDataTypeImpl(name));
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private static Map<URI, RdfOntology> getOntologyMap()
    {
        try {
            return R.cacheManager().getApplicationCache().getAndPutIfAbsent(CacheKeys.RDF_ONTOLOGIES, new CacheFunction<CacheKey, Map<URI, RdfOntology>>()
            {
                @Override
                public Map<URI, RdfOntology> apply(CacheKey cacheKey)
                {
                    return new HashMap<>();
                }
            });
        }
        catch (IOException e) {
            throw new RdfInitializationException("Error while initializing RDF ontologies; this shouldn't happen", e);
        }
    }
    private static Map<String, RdfOntology> getOntologyPrefixMap()
    {
        try {
            return R.cacheManager().getApplicationCache().getAndPutIfAbsent(CacheKeys.RDF_ONTOLOGY_PREFIXES, new CacheFunction<CacheKey, Map<String, RdfOntology>>()
            {
                @Override
                public Map<String, RdfOntology> apply(CacheKey cacheKey)
                {
                    return new HashMap<>();
                }
            });
        }
        catch (IOException e) {
            throw new RdfInitializationException("Error while initializing RDF ontology prefixes; this shouldn't happen", e);
        }
    }
//    private static <T> Set<T> getLocalRdfMapCache(RdfMapCacheKey type, Class<? extends T> clazz)
//    {
//        try {
//            Map<RdfMapCacheKey, Set> entries = R.cacheManager().getApplicationCache().getAndPutIfAbsent(CacheKeys.RDF_ONTOLOGY_ENTRIES, new CacheFunction<CacheKey, Map<RdfMapCacheKey, Set>>()
//            {
//                @Override
//                public Map<RdfMapCacheKey, Set> apply(CacheKey cacheKey)
//                {
//                    //make sure all ontologies are initialized
//                    assertInitialized();
//
//                    RdfOntology localOntology = RdfFactory.getOntologyForPrefix(Settings.instance().getRdfOntologyPrefix());
//
//                    Set<RdfClass> localPublicClasses = new HashSet<>(localOntology.getPublicClasses().values());
//
//                    Set<RdfProperty> localPublicProperties = new HashSet<>();
//                    for (RdfClass rdfClass : localOntology.getPublicClasses().values()) {
//                        if (rdfClass.getProperties() != null) {
//                            localPublicProperties.addAll(rdfClass.getProperties());
//                        }
//                    }
//
//                    Map<RdfMapCacheKey, Set> retVal = new HashMap<>();
//                    retVal.put(RdfMapCacheKey.LOCAL_PUBLIC_CLASSES, localPublicClasses);
//                    retVal.put(RdfMapCacheKey.LOCAL_PUBLIC_CLASS_PROPERTIES, localPublicProperties);
//
//                    return retVal;
//                }
//            });
//
//            return entries.get(type);
//        }
//        catch (IOException e) {
//            throw new RdfInitializationException("Error while initializing RDF ontologies; this shouldn't happen", e);
//        }
//    }
}
