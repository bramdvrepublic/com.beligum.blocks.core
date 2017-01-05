package com.beligum.blocks.config;

import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
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
        CLASS,
        PROPERTY
        ;
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
        if (vocabUri!=null) {
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
        if (vocab!=null) {
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
        if (rdfClass!=null) {
            retVal = rdfClass.getEndpoint();
        }

        return retVal;
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
    private static <T> Set<T> getRdfMapCache(RdfMapCacheKey type, Class < ? extends T > clazz)
    {
        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.RDF_VOCABULARY_ENTRIES)) {
            Map<RdfMapCacheKey, Set> retVal = new HashMap<>();
            retVal.put(RdfMapCacheKey.CLASS, new HashSet<RdfClass>());
            retVal.put(RdfMapCacheKey.PROPERTY, new HashSet<RdfProperty>());

            //make sure we booted the static members at least once
            assertInitialized();

            Map<URI, RdfVocabulary> vocabularies = getVocabularies();
            for (Map.Entry<URI, RdfVocabulary> e : vocabularies.entrySet()) {
                RdfVocabulary vocab = e.getValue();
                retVal.get(RdfMapCacheKey.CLASS).addAll(vocab.getPublicClasses().values());
                retVal.get(RdfMapCacheKey.PROPERTY).addAll(vocab.getPublicProperties().values());
            }

            R.cacheManager().getApplicationCache().put(CacheKeys.RDF_VOCABULARY_ENTRIES, retVal);
        }

        Object tempRetVal = ((Map)R.cacheManager().getApplicationCache().get(CacheKeys.RDF_VOCABULARY_ENTRIES)).get(type);

        return (Set<T>) tempRetVal;
    }
    /**
     * This will instantiate all static factory classes once if needed,
     * so we can be sure every RDF member has been assigned to it's proper vocabulary, etc.
     */
    public static void assertInitialized()
    {
        if (!initialized) {
            Set<Class<? extends RdfResourceFactory>> resourceFactories = ReflectionFunctions.searchAllClassesImplementing(RdfResourceFactory.class, true);
            for (Class<? extends RdfResourceFactory> c : resourceFactories) {
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
    private static URI curieToFull(URI resourceTypeCurie)
    {
        //if we find nothing, we return null, which kind of makes sense to indicate an error
        URI retVal = null;

        RdfVocabulary vocab = getVocabularyForPrefix(resourceTypeCurie.getScheme());
        if (vocab!=null) {
            retVal = vocab.resolve(resourceTypeCurie.getPath());
        }
        else {
            Logger.warn("Encountered unknown curie schema, returning null for; " + resourceTypeCurie);
        }

        return retVal;
    }
}
