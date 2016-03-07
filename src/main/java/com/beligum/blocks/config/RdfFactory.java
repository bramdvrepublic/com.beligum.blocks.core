package com.beligum.blocks.config;

import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.base.utils.toolkit.ReflectionFunctions;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ifaces.RdfResourceFactory;
import com.beligum.blocks.rdf.ifaces.RdfVocabulary;

import java.lang.reflect.Field;
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
        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.RDF_VOCABULARIES)) {
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

            Set<Class<?>> collections = ReflectionFunctions.searchAllClassesImplementing(RdfVocabulary.class);
            for (Class<?> c : collections) {
                try {
                    //we don't take abstract superclasses into account
                    if (!java.lang.reflect.Modifier.isAbstract(c.getModifiers())) {
                        Field instanceVariable = c.getDeclaredField(RdfVocabulary.INSTANCE_FIELD_NAME);
                        if (java.lang.reflect.Modifier.isStatic(instanceVariable.getModifiers())) {
                            if (RdfVocabulary.class.isAssignableFrom(instanceVariable.getType())) {
                                RdfVocabulary vocab = (RdfVocabulary) instanceVariable.get(null);
                                retVal.get(RdfMapCacheKey.CLASS).addAll(vocab.getPublicClasses());
                                retVal.get(RdfMapCacheKey.PROPERTY).addAll(vocab.getPublicProperties());
                            }
                            else {
                                Logger.warn("Encountered RDF vocabulary class with a static singleton field '" + RdfVocabulary.INSTANCE_FIELD_NAME + "', but not of type '" +
                                            RdfVocabulary.class.getCanonicalName() + "' please make it implement that class; " +
                                            c.getCanonicalName());
                            }
                        }
                        else {
                            Logger.warn("Encountered RDF vocabulary class with a non-static singleton field '" + RdfVocabulary.INSTANCE_FIELD_NAME + "', please make it static instead; " +
                                        c.getCanonicalName());
                        }
                    }
                }
                catch (NoSuchFieldException e) {
                    Logger.warn("Encountered RDF vocabulary class without a static singleton field '" + RdfVocabulary.INSTANCE_FIELD_NAME + "', please change the implementation; " +
                                c.getCanonicalName(), e);
                }
                catch (Exception e) {
                    throw new RuntimeException("Error while instantiating an RDF vocabulary, this shouldn't happen; " + c, e);
                }
            }

            R.cacheManager().getApplicationCache().put(CacheKeys.RDF_VOCABULARIES, retVal);
        }

        Object tempRetVal = ((Map)R.cacheManager().getApplicationCache().get(CacheKeys.RDF_VOCABULARIES)).get(type);

        return (Set<T>) tempRetVal;
    }
}
