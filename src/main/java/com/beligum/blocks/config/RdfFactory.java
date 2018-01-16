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

package com.beligum.blocks.config;

import com.beligum.base.server.R;
import com.beligum.base.utils.toolkit.ReflectionFunctions;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.endpoints.ifaces.RdfQueryEndpoint;
import com.beligum.blocks.rdf.ifaces.*;

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
    private static boolean initialized = false;

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    public static Map<URI, RdfVocabulary> getVocabularies()
    {
        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.RDF_VOCABULARIES)) {
            R.cacheManager().getApplicationCache().put(CacheKeys.RDF_VOCABULARIES, new HashMap<URI, RdfVocabulary>());
        }

        return (Map<URI, RdfVocabulary>) R.cacheManager().getApplicationCache().get(CacheKeys.RDF_VOCABULARIES);
    }
    public static Map<String, URI> getVocabularyPrefixes()
    {
        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.RDF_VOCABULARY_PREFIXES)) {
            R.cacheManager().getApplicationCache().put(CacheKeys.RDF_VOCABULARY_PREFIXES, new HashMap<String, URI>());
        }

        return (Map<String, URI>) R.cacheManager().getApplicationCache().get(CacheKeys.RDF_VOCABULARY_PREFIXES);
    }
    public static RdfVocabulary getVocabularyForPrefix(String prefix)
    {
        //make sure we booted the static members at least once
        assertInitialized();

        RdfVocabulary retVal = null;

        URI vocabUri = getVocabularyPrefixes().get(prefix);
        if (vocabUri != null) {
            retVal = getVocabularies().get(vocabUri);
        }

        return retVal;
    }
    public static RdfClass getClassForResourceType(URI resourceTypeCurie)
    {
        return (RdfClass) getForResourceType(resourceTypeCurie);
    }
    public static RdfResource getForResourceType(URI resourceTypeCurie)
    {
        //make sure we booted the static members at least once
        assertInitialized();

        RdfResource retVal = null;

        RdfVocabulary vocab = getVocabularyForPrefix(resourceTypeCurie.getScheme());
        if (vocab != null) {
            //note: We search in all classes (difference between public and non-public classes is that the public classes are exposed to the client as selectable as a page-type).
            //      Since we also want to look up a value (eg. with the innner Geonames endpoint), we allow all classes to be searched.
            retVal = vocab.getAllTypes().get(resourceTypeCurie);
        }

        return retVal;
    }
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
    /**
     * Returns all public classes in the local vocabulary
     */
    public static Set<RdfClass> getLocalPublicClasses()
    {
        return getLocalRdfMapCache(RdfMapCacheKey.LOCAL_PUBLIC_CLASSES, RdfClass.class);
    }
    /**
     * Returns all properties (so not only the public ones) across all public classes in the local vocabulary
     */
    public static Set<RdfProperty> getLocalPublicClassProperties()
    {
        return getLocalRdfMapCache(RdfMapCacheKey.LOCAL_PUBLIC_CLASS_PROPERTIES, RdfProperty.class);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    /**
     * TODO: little bit dirty with all the casting...
     */
    private static synchronized  <T> Set<T> getLocalRdfMapCache(RdfMapCacheKey type, Class<? extends T> clazz)
    {
        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.RDF_VOCABULARY_ENTRIES)) {
            Map<RdfMapCacheKey, Set> retVal = new HashMap<>();
            retVal.put(RdfMapCacheKey.LOCAL_PUBLIC_CLASSES, new HashSet<RdfClass>());
            retVal.put(RdfMapCacheKey.LOCAL_PUBLIC_CLASS_PROPERTIES, new HashSet<RdfProperty>());

            //make sure we booted the static members at least once
            assertInitialized();

            RdfVocabulary vocab = RdfFactory.getVocabularyForPrefix(Settings.instance().getRdfOntologyPrefix());
            retVal.get(RdfMapCacheKey.LOCAL_PUBLIC_CLASSES).addAll(vocab.getPublicClasses().values());
            for (RdfClass rdfClass : vocab.getPublicClasses().values()) {
                if (rdfClass.getProperties() != null) {
                    retVal.get(RdfMapCacheKey.LOCAL_PUBLIC_CLASS_PROPERTIES).addAll(rdfClass.getProperties());
                }
            }

            R.cacheManager().getApplicationCache().put(CacheKeys.RDF_VOCABULARY_ENTRIES, retVal);
        }

        Object tempRetVal = ((Map) R.cacheManager().getApplicationCache().get(CacheKeys.RDF_VOCABULARY_ENTRIES)).get(type);

        return (Set<T>) tempRetVal;
    }
    /**
     * This will instantiate all static factory classes once if needed,
     * so we can be sure every RDF member has been assigned to it's proper vocabulary, etc.
     */
    public static synchronized void assertInitialized()
    {
        if (!initialized) {

            for (Class<? extends RdfResourceFactory> c : ReflectionFunctions.searchAllClassesImplementing(RdfResourceFactory.class, true)) {
                try {
                    c.newInstance();
                }
                catch (Exception e) {
                    throw new RuntimeException("Error while instantiating an RDF resource factory, this shouldn't happen; " + c, e);
                }
            }

            initialized = true;
        }
    }
}
