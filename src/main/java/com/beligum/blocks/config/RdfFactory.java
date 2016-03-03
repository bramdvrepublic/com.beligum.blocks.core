package com.beligum.blocks.config;

import com.beligum.base.server.R;
import com.beligum.base.utils.toolkit.ReflectionFunctions;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfClassFactory;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ifaces.RdfPropertyFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by bram on 2/26/16.
 */
public class RdfFactory
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    public static RdfProperty[] getProperties()
    {
        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.RDF_PROPERTIES)) {
            Set<RdfProperty> discovered = new HashSet<>();
            Set<Class<?>> collections = ReflectionFunctions.searchAllClassesImplementing(RdfPropertyFactory.class);
            for (Class<?> c : collections) {
                try {
                    RdfPropertyFactory collection = (RdfPropertyFactory) c.newInstance();
                    discovered.addAll(collection.getRdfProperties());
                }
                catch (Exception e) {
                    throw new RuntimeException("Error while instantiating an RDF property collection, this shouldn't happen; "+c, e);
                }
            }

            R.cacheManager().getApplicationCache().put(CacheKeys.RDF_PROPERTIES, discovered.toArray(new RdfProperty[discovered.size()]));
        }

        return (RdfProperty[]) R.cacheManager().getApplicationCache().get(CacheKeys.RDF_PROPERTIES);
    }
    public static RdfClass[] getClasses()
    {
        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.RDF_CLASSES)) {
            Set<RdfClass> discoveredClasses = new HashSet<>();
            Set<Class<?>> rdfClasses = ReflectionFunctions.searchAllClassesImplementing(RdfClassFactory.class);
            for (Class<?> c : rdfClasses) {
                try {
                    RdfClassFactory collection = (RdfClassFactory) c.newInstance();
                    discoveredClasses.addAll(collection.getRdfClasses());
                }
                catch (Exception e) {
                    throw new RuntimeException("Error while instantiating an RDF class collection, this shouldn't happen; "+c, e);
                }
            }

            R.cacheManager().getApplicationCache().put(CacheKeys.RDF_CLASSES, discoveredClasses.toArray(new RdfClass[discoveredClasses.size()]));
        }

        return (RdfClass[]) R.cacheManager().getApplicationCache().get(CacheKeys.RDF_CLASSES);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
