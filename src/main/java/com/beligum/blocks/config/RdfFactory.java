package com.beligum.blocks.config;

import com.beligum.base.server.R;
import com.beligum.base.utils.toolkit.ReflectionFunctions;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ifaces.RdfResourceFactory;
import com.beligum.blocks.rdf.ifaces.RdfVocabulary;

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
        CLASS,
        PROPERTY
        ;
    }

    //-----VARIABLES-----

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
    public static Set<RdfProperty> getProperties()
    {
        return getRdfMapCache(RdfMapCacheKey.PROPERTY, RdfProperty.class);
    }
    public static Set<RdfClass> getClasses()
    {
        return getRdfMapCache(RdfMapCacheKey.CLASS, RdfClass.class);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    /**
     * TODO: little bit dirty with all the casting...
     */
    private static <T> Set<T> getRdfMapCache(RdfMapCacheKey type, Class<? extends T> clazz)
    {
        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.RDF_VOCABULARY_ENTRIES)) {
            Map<RdfMapCacheKey, Set> retVal = new HashMap<>();
            retVal.put(RdfMapCacheKey.CLASS, new HashSet<RdfClass>());
            retVal.put(RdfMapCacheKey.PROPERTY, new HashSet<RdfProperty>());

            //make sure we instantiate the resource factories once before building up the cache,
            // to allow static members to be initialized and added to the proper vocabulary
            Set<Class<?>> resourceFactories = ReflectionFunctions.searchAllClassesImplementing(RdfResourceFactory.class);
            for (Class<?> c : resourceFactories) {
                try {
                    c.newInstance();
                }
                catch (Exception e) {
                    throw new RuntimeException("Error while instantiating an RDF resource factory, this shouldn't happen; " + c, e);
                }
            }

            Map<URI, RdfVocabulary> vocabularies = getVocabularies();
            for (Map.Entry<URI, RdfVocabulary> e : vocabularies.entrySet()) {
                RdfVocabulary vocab = e.getValue();
                retVal.get(RdfMapCacheKey.CLASS).addAll(vocab.getPublicClasses());
                retVal.get(RdfMapCacheKey.PROPERTY).addAll(vocab.getPublicProperties());
            }

            R.cacheManager().getApplicationCache().put(CacheKeys.RDF_VOCABULARY_ENTRIES, retVal);
        }

        Object tempRetVal = ((Map)R.cacheManager().getApplicationCache().get(CacheKeys.RDF_VOCABULARY_ENTRIES)).get(type);

        return (Set<T>) tempRetVal;
    }
}
