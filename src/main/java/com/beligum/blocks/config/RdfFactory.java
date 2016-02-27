package com.beligum.blocks.config;

import com.beligum.base.server.R;
import com.beligum.base.utils.toolkit.ReflectionFunctions;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfClassCollection;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ifaces.RdfPropertyCollection;

import java.util.Arrays;
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
            Set<Class<?>> collections = ReflectionFunctions.searchAllClassesImplementing(RdfPropertyCollection.class);
            for (Class<?> c : collections) {
                try {
                    RdfPropertyCollection collection = (RdfPropertyCollection) c.newInstance();
                    discovered.addAll(Arrays.asList(collection.getProperties()));
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
            Set<Class<?>> rdfClasses = ReflectionFunctions.searchAllClassesImplementing(RdfClassCollection.class);
            for (Class<?> c : rdfClasses) {
                try {
                    RdfClassCollection collection = (RdfClassCollection) c.newInstance();
                    discoveredClasses.addAll(Arrays.asList(collection.getClasses()));
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
