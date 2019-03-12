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
import java.lang.reflect.Field;
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
 * <p>
 * Update: we introduced a new factory instance for every ontology initialization, to keep track of the members added in this instance.
 * This way, we can eliminate the need for these lines:
 * <p>
 * .ontology(this)
 * .create()
 * <p>
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
    private static Map<Class<RdfOntologyImpl>, RdfOntologyImpl> ontologyInstances = new LinkedHashMap<>();

    //package-private ontology we're creating members for during this session
    RdfOntologyImpl ontology;
    //a package-private registry of builder instances to automatically call .create() on them
    Set<AbstractRdfOntologyMember.Builder> registry;

    //-----CONSTRUCTORS-----
    /**
     * This private constructor will function as a 'lock' that needs to be passed to the RDF initialization methods
     * and assures those can only be constructed from the assertInitialized() in this class.
     */
    private RdfFactory(RdfOntologyImpl ontology)
    {
        this.ontology = ontology;
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
        return RdfFactory.getOntologyForPrefix(Settings.instance().getRdfMainOntologyNamespace().getPrefix());
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

                    //we allow different instances of the same ontology namespace as a means of modularizing
                    //the ontology instantiating and two instances with the same namespace are considered equal
                    Map<RdfNamespace, List<RdfOntologyImpl>> allOntologies = new LinkedHashMap<>();

                    for (Class<? extends RdfOntology> c : ReflectionFunctions.searchAllClassesImplementing(RdfOntology.class, true)) {
                        try {
                            //universal way of creating new ontology instances
                            RdfOntology rdfOntology = getOntologyInstance(c);

                            //for now, we only support RdfOntologyImpl
                            if (!(rdfOntology instanceof RdfOntologyImpl)) {
                                throw new RdfInstantiationException("Encountered an RDF ontology instance that's not a " + RdfOntologyImpl.class.getSimpleName() + ", please fix this; " + rdfOntology);
                            }
                            else {
                                //we need this to call the create() method below
                                RdfOntologyImpl rdfOntologyImpl = (RdfOntologyImpl) rdfOntology;

                                //Passing an instance of RdfFactory (note the private constructor) to the create() method, assures other developers won't be able to
                                //create RDF ontology instances manually (it's a sort of key for a lock)
                                //Also note that we create a new factory instance for each ontology, because we want to track the ontology members that were created
                                //during this session (using the registry), to auto-finalize them after creating the instances from the proxies.
                                RdfFactory rdfFactory = new RdfFactory(rdfOntologyImpl);

                                //this call will initialize all member fields
                                rdfOntologyImpl.create(rdfFactory);

                                // now loop through all members that were created during the scope of the last create()
                                // to do some auto post-initialization and link them to the main ontology
                                for (AbstractRdfOntologyMember.Builder builder : rdfFactory.registry) {
                                    //all members should still be proxies here
                                    if (builder.rdfResource.isProxy()) {
                                        //now convert the proxy to a real member and attach it to the main ontology
                                        builder.create();
                                    }
                                    else {
                                        throw new RdfInitializationException("Encountered a non-proxy RDF ontology member, this shouldn't happen; " + builder);
                                    }
                                }

                                // It's easy for the create() method to miss some fields, so let's help the dev
                                // a little bit by iterating all members of the ontology and check if they have been
                                // registered properly
                                try {
                                    for (Field field : rdfOntologyImpl.getClass().getFields()) {
                                        //should we also check for a static modifier here?
                                        if (field.getType().isAssignableFrom(RdfOntologyMember.class)) {
                                            RdfOntologyMember member = (RdfOntologyMember) field.get(rdfOntologyImpl);
                                            if (member == null) {
                                                throw new RdfInitializationException(
                                                                "Field inside an RDF ontology turned out null after initializing the ontology; this shouldn't happen; " + rdfOntologyImpl);
                                            }
                                            else if (member.isProxy()) {
                                                throw new RdfInitializationException(
                                                                "Field inside an RDF ontology turned out to be still a proxy after initializing the ontology; this shouldn't happen; " +
                                                                rdfOntologyImpl);
                                            }
                                        }
                                    }
                                }
                                catch (Exception e) {
                                    throw new RdfInitializationException("Error happened while validating the members of an ontology; " + rdfOntologyImpl, e);
                                }

                                //if we reach this, all is well, so store it
                                if (!allOntologies.containsKey(rdfOntologyImpl.getNamespace())) {
                                    allOntologies.put(rdfOntologyImpl.getNamespace(), new ArrayList<>());
                                }
                                allOntologies.get(rdfOntologyImpl.getNamespace()).add(rdfOntologyImpl);
                            }
                        }
                        catch (Throwable e) {
                            throw new RdfInstantiationException("Error while initializing an RDF ontology - phase 1; " + c, e);
                        }
                    }

                    for (Map.Entry<RdfNamespace, List<RdfOntologyImpl>> entry : allOntologies.entrySet()) {

                        try {
                            //let's assume the first one is the main one
                            RdfOntologyImpl mainOntology = entry.getValue().get(0);

                            boolean isPublic = mainOntology.isPublic();

                            //if this ontology occurred more than once, we'll merge the others into the main ontology and discard them
                            for (int i = 1; i < entry.getValue().size(); i++) {

                                RdfOntologyImpl o = entry.getValue().get(i);

                                //we'll configure it like this: if at least one of them is public, the main ontology is public
                                if (!isPublic && o.isPublic()) {
                                    isPublic = true;
                                }

                                //'move' all members to the main ontology
                                for (RdfOntologyMember m : o.getAllMembers().values()) {
                                    //note that this cast should always work because we control the member implementation
                                    mainOntology._register((AbstractRdfOntologyMember) m);
                                }
                            }

                            //'save' the resulting value back to the main ontology
                            mainOntology.setPublic(isPublic);

                            //only public ontologies are saved to the lookup maps; the rest are just initialized and referenced from other public ontologies
                            if (mainOntology.isPublic()) {
                                //store the ontology in a lookup map
                                RdfFactory.getOntologyUriMap().put(mainOntology.getNamespace().getUri(), mainOntology);
                                RdfFactory.getOntologyPrefixMap().put(mainOntology.getNamespace().getPrefix(), mainOntology);

                                // if a public ontology references another ontology, regardless of being public or not,
                                // we'll also save it in the lookup map, because we'll encounter it sooner or later
                                for (RdfOntology ref : mainOntology.getReferencedOntologies()) {
                                    if (!RdfFactory.getOntologyUriMap().containsKey(ref.getNamespace().getUri())) {
                                        RdfFactory.getOntologyUriMap().put(ref.getNamespace().getUri(), ref);
                                        RdfFactory.getOntologyPrefixMap().put(ref.getNamespace().getPrefix(), ref);
                                    }
                                }
                            }

                        }
                        catch (Throwable e) {
                            throw new RdfInstantiationException("Error while initializing an RDF ontology - phase 2; " + entry.getKey(), e);
                        }
                    }

                    //if we reach this point, we iterated all ontologies and put them in our map above,
                    // so it's safe to wipe the static map and release it's memory
                    ontologyInstances.clear();

                    //Note: no need to wipe the allOntologies map; it will be garbage collected

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
        try {
            return new RdfClassImpl(getCallingOntology(), name);
        }
        catch (Throwable e) {
            throw new RdfInstantiationException("Error happened while instantiating RDF ontology member; " + name, e);
        }
    }
    /**
     * Create a new RdfProperty proxy instance. A proxy instance is converted to a true valid instance by passing it to the
     * appropriate proxy() method below (returning a builder wrapper) and then calling create(). Note that all operations on
     * proxy objects will throw a RdfProxyException.
     */
    public static RdfProperty newProxyProperty(String name)
    {
        try {
            return new RdfPropertyImpl(getCallingOntology(), name);
        }
        catch (Throwable e) {
            throw new RdfInstantiationException("Error happened while instantiating RDF ontology member; " + name, e);
        }
    }
    /**
     * Create a new RdfDatatype proxy instance. A proxy instance is converted to a true valid instance by passing it to the
     * appropriate proxy() method below (returning a builder wrapper) and then calling create(). Note that all operations on
     * proxy objects will throw a RdfProxyException.
     */
    public static RdfDatatype newProxyDatatype(String name)
    {
        try {
            return new RdfDatatypeImpl(getCallingOntology(), name);
        }
        catch (Throwable e) {
            throw new RdfInstantiationException("Error happened while instantiating RDF ontology member; " + name, e);
        }
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
    /**
     * This is a little bit dirty magic code, but makes our lives a lot easier;
     * it detects the initialization of static members in ontology classes (and limits them to ontology classes)
     * so that the container class is auto-instantiated and attached to the members on creation.
     * This avoids the need to pass the ontology class name over and over again to the static factory methods
     * and wires-in the (hard needed) ontology information into the members so that our proxy-initialization functionality
     * has a good bit of initial information inside once the create() factory method is called.
     */
    private static RdfOntologyImpl getCallingOntology() throws RdfInitializationException, IllegalAccessException, InstantiationException, ClassNotFoundException
    {
        return getOntologyInstance(Class.forName(Logger.getCallingClassName()));
    }
    /**
     * Ontology instances can be created in two ways (always by calling the default constructor):
     * - auto-magically when a ontologyMember is created, see method above
     * - on system boot, when all ontologies are iterated
     * <p>
     * This method makes sure those are uniformly created
     */
    private static RdfOntologyImpl getOntologyInstance(Class<?> clazz) throws RdfInitializationException, IllegalAccessException, InstantiationException
    {
        if (!ontologyInstances.containsKey(clazz)) {
            if (RdfOntology.class.isAssignableFrom(clazz)) {
                Class<RdfOntologyImpl> ontologyClass = (Class<RdfOntologyImpl>) clazz;
                ontologyInstances.put(ontologyClass, ontologyClass.newInstance());
            }
            else {
                throw new RdfInitializationException("Found an RDF member that's not wrapped inside a " + RdfOntology.class.getSimpleName() + " class, please fix this; " + clazz);
            }
        }

        return ontologyInstances.get(clazz);
    }
    private static Map<URI, RdfOntology> getOntologyUriMap()
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
