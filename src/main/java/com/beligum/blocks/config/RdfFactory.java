package com.beligum.blocks.config;

import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.base.utils.toolkit.ReflectionFunctions;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.endpoints.ifaces.RdfQueryEndpoint;
import com.beligum.blocks.rdf.ifaces.*;
import com.google.common.collect.ImmutableMap;

import java.net.URI;
import java.util.*;

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

            final Map<Class<? extends RdfResourceFactory>, Integer> PRIORITY_MAP = ImmutableMap.<Class<? extends RdfResourceFactory>, Integer>builder()
                            .put(RdfResourceFactory.RdfClassFactory.class, 1)
                            .put(RdfResourceFactory.RdfTermFactory.class, 2)
                            .put(RdfResourceFactory.RdfMappingFactory.class, 3)
                            .put(RdfResourceFactory.class, 4)
                            .build();
            final int LOWEST_PRIORITY = PRIORITY_MAP.size();

            //we'll wrap the set in a list to be able to sort them
            List<Class<? extends RdfResourceFactory>> resourceFactories = new ArrayList<>(ReflectionFunctions.searchAllClassesImplementing(RdfResourceFactory.class, true));
            resourceFactories.sort(new Comparator<Class<? extends RdfResourceFactory>>()
            {
                @Override
                public int compare(Class<? extends RdfResourceFactory> o1, Class<? extends RdfResourceFactory> o2)
                {
                    return getPriority(o1).compareTo(getPriority(o2));
                }
                /**
                 * This is a quick and dirty lookup function because the hash-lookup doesn't work of course,
                 * because specific classes don't result in the same hashcode as their implementations.
                 */
                private Integer getPriority(Class<? extends RdfResourceFactory> clazz)
                {
                    Integer retVal = LOWEST_PRIORITY;

                    for (Map.Entry<Class<? extends RdfResourceFactory>, Integer> e : PRIORITY_MAP.entrySet()) {
                        if (e.getKey().isAssignableFrom(clazz)) {
                            retVal = e.getValue();
                            break;
                        }
                    }

                    return retVal;
                }
            });

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
        //if we find nothing, we return null, which kind of makes sense to indicate an setRollbackOnly
        URI retVal = null;

        RdfVocabulary vocab = getVocabularyForPrefix(resourceTypeCurie.getScheme());
        if (vocab != null) {
            retVal = vocab.resolve(resourceTypeCurie.getPath());
        }
        else {
            Logger.warn("Encountered unknown curie schema, returning null for; " + resourceTypeCurie);
        }

        return retVal;
    }
}
