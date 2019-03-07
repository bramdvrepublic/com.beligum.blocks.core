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
import com.beligum.blocks.exceptions.RdfInstantiationException;
import com.beligum.blocks.exceptions.RdfInitializationException;
import com.beligum.blocks.rdf.ifaces.*;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * Main factory class for all RDF-related initialization and lookup.
 * <p>
 * Note that this facilitates a specific pattern and needs a bit more explanation:
 * <p>
 * When creating ontologies, we want to be able to link them together at compile time, but also initialize the specifics of all ontology
 * members during boot at runtime (eg. create a sort of framework for the ontology in code, but load the details from a config file).
 * To make this possible, we create stub-objects (proxies) in code that can be linked into other ontologies, only supplying the bare necessities
 * (eg. only the name of the member), while filling in the details after the system has finished booting, effectively transforming those proxies
 * to valid instances with all bells and whistles correctly initialized.
 * <p>
 * We have hidden all this functionality behind a few methods in this class. Eg. to create a RDF class in an ontology, you call:
 * <p>
 * public static final RdfClass Thing = RdfFactory.newProxyClass("Thing");
 * <p>
 * After boot finishes, the create() method of the ontology is called, with a valid instance of this factory class. This instance
 * more or less functions as a 'key' (note the private constructor of RdfFactory) to initialize the proxy further using a decorator
 * pattern. Eg. to finish creating the 'Thing' above, we call:
 * <p>
 * rdfFactory.proxy(Thing)
 * .ontology(this)
 * .title(Entries.OWL_title_Thing)
 * .label(Entries.OWL_label_Thing)
 * .create();
 *
 * Update: we introduced a new factory instance for every ontology initialization, to keep track of the members added in this instance.
 * This way, we can eliminate the need for these lines:
 *
 * .ontology(this)
 * .create()
 *
 * because they can be added automatically after calling RdfOntology.create()
 *
 * <p>
 * Note that create() needs to be called to finish everything off. Also note it returns the instance, but can be ignored to simplify the code.
 * If a method inside an ontology member is called without having created it properly, an RdfProxyException is thrown to signal the developer
 * a mistake was made.
 * <p>
 * Created by bram on 2/26/16.
 */
public class RdfFactory
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private static boolean initialized = false;

    //package-private only: these will be used to keep track of classes added during this factory session
    //will be initialized as soon as the ontology is created (because we're using a dynamic instanceof())
    RdfOntology ontology;
    //a registry of builder instances to automatically call .create() on them
    Set<AbstractRdfOntologyMember.Builder> registry;

    //-----CONSTRUCTORS-----
    /**
     * This private constructor will function as a 'lock' that needs to be passed to the RDF initialization methods
     * and assures those can only be constructed from the assertInitialized() in this class.
     */
    private RdfFactory()
    {
        this.ontology = null;
        this.registry = new LinkedHashSet<>();
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
            retVal = ontology.getAllMembers().get(resourceTypeCurie);
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
    /**
     * Returns a reference to the local ontology
     */
    public static RdfOntology getLocalOntology()
    {
        return RdfFactory.getOntologyForPrefix(Settings.instance().getRdfLocalOntologyNamespace().getPrefix());
    }
    /**
     * This will instantiate all ontologies once if needed, so we can be sure every RDF member has been assigned to it's proper ontology, etc.
     */
    public static void assertInitialized()
    {
        //speedy synchronization
        if (!initialized) {
            synchronized (RdfFactory.class) {
                if (!initialized) {

                    for (Class<? extends RdfOntology> c : ReflectionFunctions.searchAllClassesImplementing(RdfOntology.class, true)) {
                        try {
                            //Passing an instance of RdfFactory (note the private constructor) to the constructor, assures other developers won't be able to
                            //create RDF ontology instances manually.
                            //Also note that we create a new factory instance for each ontology, because we want to track the ontology members that were created
                            //during this session, to auto-finalize them after creating the instances from the proxies, see RdfOntology constructor for details.
                            RdfOntology rdfOntology = c.getConstructor(RdfFactory.class).newInstance(new RdfFactory());

                            //only public ontologies are saved to the map; the rest are just initialized and references from other public ontologies
                            if (rdfOntology.isPublic()) {
                                //this is support for a splitted implementation of an ontology, spread out over multiple java classes
                                //(needed for modularization)
                                //Note that the registry of the factory instance will still contain all the builders created during this session since we
                                //always create a new instance
                                if (RdfFactory.getOntologyMap().containsKey(rdfOntology.getNamespace().getUri())) {
                                    RdfOntology existingOntology = RdfFactory.getOntologyMap().get(rdfOntology.getNamespace().getUri());
                                    for (RdfOntologyMember m : rdfOntology.getAllMembers().values()) {
                                        existingOntology._register(m);
                                    }
                                    //don't add the ontology to the map, it will get garbage collected instead
                                }
                                //a true new ontology; make sure we add it to the lookup maps
                                else {
                                    //store the ontology in a lookup map
                                    RdfFactory.getOntologyMap().put(rdfOntology.getNamespace().getUri(), rdfOntology);
                                    RdfFactory.getOntologyPrefixMap().put(rdfOntology.getNamespace().getPrefix(), rdfOntology);
                                }
                            }
                        }
                        catch (Exception e) {
                            throw new RdfInstantiationException("Error while instantiating an RDF resource factory, this shouldn't happen; " + c, e);
                        }
                    }

                    initialized = true;
                }
            }
        }
    }

    //-----PUBLIC FACTORY METHODS-----
    /**
     * Create a new RdfClass proxy instance. A proxy instance is converted to a true valid instance by passing it to the
     * appropriate proxy() method below (returning a builder wrapper) and then calling create(). Note that all operations on
     * proxy objects will throw a RdfProxyException.
     */
    public static RdfClass newProxyClass(String name)
    {
        return new RdfClassImpl(name);
    }
    /**
     * Create a new RdfProperty proxy instance. A proxy instance is converted to a true valid instance by passing it to the
     * appropriate proxy() method below (returning a builder wrapper) and then calling create(). Note that all operations on
     * proxy objects will throw a RdfProxyException.
     */
    public static RdfProperty newProxyProperty(String name)
    {
        return new RdfPropertyImpl(name);
    }
    /**
     * Create a new RdfDatatype proxy instance. A proxy instance is converted to a true valid instance by passing it to the
     * appropriate proxy() method below (returning a builder wrapper) and then calling create(). Note that all operations on
     * proxy objects will throw a RdfProxyException.
     */
    public static RdfDatatype newProxyDatatype(String name)
    {
        return new RdfDatatypeImpl(name);
    }
    /**
     * Call this method to start the un-proxy process to convert a proxy instance to a valid instance.
     * Note that you can't pass already created classes.
     */
    public RdfClassImpl.Builder register(RdfClass rdfProxyClass) throws RdfInitializationException
    {
        if (rdfProxyClass.isProxy()) {
            //note: this cast is safe because in sync with the factory method above (and a private constructor)
            return new RdfClassImpl.Builder(this, (RdfClassImpl) rdfProxyClass);
        }
        else {
            throw new RdfInitializationException("This RDF class has already been built, you can't call this method twice on the same instance; " + rdfProxyClass);
        }
    }
    /**
     * Call this method to start the un-proxy process to convert a proxy instance to a valid instance.
     * Note that you can't pass already created classes.
     */
    public RdfPropertyImpl.Builder register(RdfProperty rdfProxyProperty) throws RdfInitializationException
    {
        if (rdfProxyProperty.isProxy()) {
            //note: this cast is safe because in sync with the factory method above (and a private constructor)
            return new RdfPropertyImpl.Builder(this, (RdfPropertyImpl) rdfProxyProperty);
        }
        else {
            throw new RdfInitializationException("This RDF class has already been built, you can't call this method twice on the same instance; " + rdfProxyProperty);
        }
    }
    /**
     * Call this method to start the un-proxy process to convert a proxy instance to a valid instance.
     * Note that you can't pass already created classes.
     */
    public RdfDatatypeImpl.Builder register(RdfDatatype rdfProxyDatatype) throws RdfInitializationException
    {
        if (rdfProxyDatatype.isProxy()) {
            //note: this cast is safe because in sync with the factory method above (and a private constructor)
            return new RdfDatatypeImpl.Builder(this, (RdfDatatypeImpl) rdfProxyDatatype);
        }
        else {
            throw new RdfInitializationException("This RDF class has already been built, you can't call this method twice on the same instance; " + rdfProxyDatatype);
        }
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
            //don't throw a RdfInitializationException, it spills over into the calling methods too much
            throw new RuntimeException("Error while initializing RDF ontologies; this shouldn't happen", e);
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
            //don't throw a RdfInitializationException, it spills over into the calling methods too much
            throw new RuntimeException("Error while initializing RDF ontology prefixes; this shouldn't happen", e);
        }
    }
}
