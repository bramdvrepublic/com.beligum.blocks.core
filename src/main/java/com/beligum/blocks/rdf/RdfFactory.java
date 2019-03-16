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
import com.beligum.blocks.exceptions.RdfInstantiationException;
import com.beligum.blocks.exceptions.RdfInitializationException;
import com.beligum.blocks.rdf.ifaces.*;
import com.beligum.blocks.utils.RdfTools;
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
    final Map<RdfOntologyMember, AbstractRdfOntologyMember.Builder> registry;

    //-----CONSTRUCTORS-----
    /**
     * This private constructor will function as a 'lock' that needs to be passed to the RDF initialization methods
     * and assures those can only be constructed from the assertInitialized() in this class.
     */
    private RdfFactory(RdfOntologyImpl ontology, Map<RdfOntologyMember, AbstractRdfOntologyMember.Builder> registry)
    {
        this.ontology = ontology;
        this.registry = registry;
    }

    //-----STATIC METHODS-----
    /**
     * Returns a collection of all registered public ontologies.
     * Note that this list consists of all ontologies that were marked public explicitly,
     * but also those that are referenced from any public ontologies; eg. all ontologies that are 'accessible'
     * from our explicit public ontologies (or it's members, properties, classes, datatypes, ...).
     */
    public static Iterable<RdfOntology> getPublicOntologies()
    {
        //make sure we booted the static members at least once
        assertInitialized();

        return getPublicOntologyUriMap().values();
    }
    /**
     * Looks up the ontology in the public ontologies for the supplied URI
     */
    public static RdfOntology getOntology(URI uri)
    {
        //make sure we booted the static members at least once
        assertInitialized();

        return getPublicOntologyUriMap().get(uri);
    }
    /**
     * Looks up the ontology in the public ontologies for the supplied prefix
     */
    public static RdfOntology getOntology(String prefix)
    {
        //make sure we booted the static members at least once
        assertInitialized();

        return getPublicOntologyPrefixMap().get(prefix);
    }
    /**
     * This is the most general find-method to translate a random name to a RDF resource,
     * according to a few heuristics.
     * - the name is first trimmed
     * - the name can either be:
     * a) a CURIE-prefixed ontology member name
     * b) a full URI:
     * b1) of an ontology
     * b2) of an ontology member
     * c) a regular string to be looked up in the default vocabulary
     * d) null or the empty string
     * - if a match was found, the first one is returned
     * - if nothing was found, null is returned
     */
    public static RdfResource lookup(String unsafeValue) throws IOException
    {
        RdfResource retVal = null;

        if (!StringUtils.isEmpty(unsafeValue)) {

            String value = unsafeValue.trim();

            //this means it can be a URI or a CURIE
            if (RdfTools.isUri(value)) {

                //first, check if we're dealing with a full blown URI
                URI uri = null;
                try {
                    //Note that this will NOT throw an exception in case of a CURIE (which is a valid URI)
                    uri = URI.create(value);
                }
                catch (IllegalArgumentException e) {
                    //ignored
                }

                //here we must try to expand a CURIE
                if (uri != null) {

                    if (RdfTools.isCurie(uri)) {
                        RdfOntology ontology = getPublicOntologyPrefixMap().get(uri.getScheme());
                        if (ontology != null) {
                            //this will return null when no such member was found, which is what we want
                            retVal = ontology.getMember(uri.getSchemeSpecificPart());
                        }
                        else {
                            throw new IOException("Encountered a CURIE with an unknown ontology prefix '" + uri.getScheme() + "'; " + value);
                        }
                    }
                    //here, the URI is a full-blown uri
                    else {
                        //first, check if the uri is the namespace of an ontology
                        retVal = getPublicOntologyUriMap().get(uri);

                        //if it's not an ontology, we'll try to cut off the name and split the uri in an ontology uri and a name string;
                        //RDF ontologies either use anchor based names or real endpoints, so search for the pound sign or use the last part of the path as the name
                        if (retVal == null) {

                            retVal = parseOntologyMemberUri(value, "#");

                            //if anchor-splitting didn't result anything, try the last slash
                            if (retVal == null) {
                                retVal = parseOntologyMemberUri(value, "/");
                            }
                        }
                    }
                }
                else {
                    throw new IOException("Encountered a value with a colon (:), but it didn't parse to a valid URI; " + value);
                }
            }
            //if the value is no CURIE or URI, look it up as a member of the default ontology
            else {
                retVal = RdfFactory.getLocalOntology().getMember(value);
            }
        }

        return retVal;
    }
    /**
     * Convenience method around getOntologyMember() that only returns non-null if the member is a RdfClass instance
     * Note that this will also return RdfDatatype because it extends RdfClass.
     */
    public static RdfClass getClass(URI curie)
    {
        RdfOntologyMember member = getOntologyMember(curie);

        return member != null && member instanceof RdfClass ? (RdfClass) member : null;
    }
    /**
     * Convenience method around getClass() with a curie string instead of URI
     */
    public static RdfClass getClass(String curie)
    {
        return getClass(URI.create(curie));
    }
    /**
     * Convenience method around getOntologyMember() that only returns non-null if the member is a RdfDatatype instance
     */
    public static RdfDatatype getDatatype(URI curie)
    {
        RdfOntologyMember member = getOntologyMember(curie);

        return member != null && member instanceof RdfDatatype ? (RdfDatatype) member : null;
    }
    /**
     * Convenience method around getDatatype() with a curie string instead of URI
     */
    public static RdfDatatype getDatatype(String curie)
    {
        return getDatatype(URI.create(curie));
    }
    /**
     * Convenience method around getOntologyMember() that only returns non-null if the member is a RdfProperty instance
     */
    public static RdfProperty getProperty(URI curie)
    {
        RdfOntologyMember member = getOntologyMember(curie);

        return member != null && member instanceof RdfProperty ? (RdfProperty) member : null;
    }
    /**
     * Convenience method around getProperty() with a curie string instead of URI
     */
    public static RdfProperty getProperty(String curie)
    {
        return getProperty(URI.create(curie));
    }
    /**
     * Convenience method around getOntologyMember() with a curie string instead of URI
     */
    public static RdfOntologyMember getOntologyMember(String curie)
    {
        return getOntologyMember(URI.create(curie));
    }
    /**
     * Looks up the RDF ontology member in all known (public) ontologies.
     * Returns null if nothing was found.
     */
    public static RdfOntologyMember getOntologyMember(URI curie)
    {
        //make sure we booted the static members at least once
        assertInitialized();

        RdfOntologyMember retVal = null;

        RdfOntology ontology = getOntology(curie.getScheme());
        if (ontology != null) {
            //note: We search in all classes (difference between public and non-public classes is that the public classes are exposed to the client as selectable as a page-type).
            //      Since we also want to look up a value (eg. with the innner Geonames endpoint), we allow all classes to be searched.
            retVal = ontology.getMember(curie.getSchemeSpecificPart());
        }

        return retVal;
    }
    /**
     * Returns a reference to the local main ontology
     */
    public static RdfOntology getLocalOntology()
    {
        return RdfFactory.getOntology(Settings.instance().getRdfMainOntologyNamespace().getPrefix());
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

                    // we'll keep a map of all proxy builders around while iterating all ontologies,
                    // before we turn the proxies into real instances, so other ontologies in the classpath
                    // can overwrite and/or extend members of each other. Then, when all ontologiy subclasses are iterated,
                    // we'll re-iterate the list of builders and create them.
                    Map<RdfOntologyMember, AbstractRdfOntologyMember.Builder> registry = new LinkedHashMap<>();

                    //we allow different instances of the same ontology namespace as a means of modularizing
                    //the ontology instantiating and two instances with the same namespace are considered equal
                    Map<RdfNamespace, RdfOntologyImpl> allOntologies = new LinkedHashMap<>();

                    for (Class<? extends RdfOntology> c : ReflectionFunctions.searchAllClassesImplementing(RdfOntology.class, true)) {
                        try {
                            //universal way of creating new ontology instances
                            RdfOntology rdfOntology = getOntologyInstance(c);

                            //for now, we only support RdfOntologyImpl
                            if (!(rdfOntology instanceof RdfOntologyImpl)) {
                                throw new RdfInstantiationException("Encountered an RDF ontology instance that's not a " + RdfOntologyImpl.class.getSimpleName() + ", please fix this; " + rdfOntology);
                            }
                            else {
                                //we need this to access package-private class members
                                RdfOntologyImpl rdfOntologyImpl = (RdfOntologyImpl) rdfOntology;

                                // This is subtle: we allow the dev to implement an ontology in multiple modules
                                // and for each member, we auto-initialize the container ontology because we need
                                // it for it's management methods (equals(), hashCode() and toString()). This means
                                // the members will sometimes point to different instances of the same ontology,
                                // which is not practical (even though they are "equal()").
                                //
                                // By keeping a central ontology map, we fix this situation by always passing the same
                                // instance to the RdfFactory below, and correct the ontology pointer in the builder methods
                                // (note that this is implemented in the constructor of the builder).
                                //
                                // Note that we still need to call the create() method on the specific ontology-instance, though.
                                if (!allOntologies.containsKey(rdfOntology.getNamespace())) {
                                    allOntologies.put(rdfOntology.getNamespace(), rdfOntologyImpl);
                                }
                                RdfOntologyImpl mainOntology = allOntologies.get(rdfOntologyImpl.getNamespace());

                                // public-ness of a modularized ontologies is contagious; if one of them is public,
                                // the result will be a public ontology
                                if (rdfOntologyImpl.isPublic() && !mainOntology.isPublic()) {
                                    mainOntology.setPublic(true);
                                }

                                // Passing an instance of RdfFactory (note the private constructor) to the create() method,
                                // assures other developers won't be able to create RDF ontology instances manually
                                // (it's a sort of key for a lock)
                                RdfFactory rdfFactory = new RdfFactory(mainOntology, registry);

                                //this call will initialize all member fields and add them to the registry if
                                //the're not present yet.
                                rdfOntologyImpl.create(rdfFactory);
                            }
                        }
                        catch (Throwable e) {
                            throw new RdfInstantiationException("Error while initializing an RDF ontology - phase 1; " + c, e);
                        }
                    }

                    // Now loop through all members that were created during the scope of the last create()
                    // to do some auto post-initialization and link them to main ontology they belong to.
                    // Note that we can't do this after calling RdfOntology.create() above because we want to
                    // support initializing members from multiple modules (eg. only set the sameAs() when the
                    // according ontology of the sameAs members was created)
                    for (AbstractRdfOntologyMember.Builder builder : registry.values()) {
                        try {
                            //all members should still be proxies here
                            if (builder.rdfResource.isProxy()) {

                                // now convert the proxy to a real member and attach it to the main ontology
                                builder.create();

                                // register the member into it's ontology, filling all the right mappings
                                // note that this needs to happen after creation (proxy = false) because it will
                                // call public methods on the resource and they are programmed to throw exceptions
                                // when the instance is still a proxy
                                builder.rdfResource.ontology._register(builder.rdfResource);
                            }
                            else {
                                throw new RdfInitializationException("Encountered a non-proxy RDF ontology member, this shouldn't happen; " + builder);
                            }
                        }
                        catch (Throwable e) {
                            throw new RdfInstantiationException("Error while initializing an RDF member - phase 2; " + builder.rdfResource, e);
                        }
                    }

                    for (Map.Entry<RdfNamespace, RdfOntologyImpl> entry : allOntologies.entrySet()) {

                        try {

                            RdfOntologyImpl mainOntology = entry.getValue();

                            //check if all members of this ontology are initialized by the loop above
                            checkOntologyMembers(mainOntology);

                            //only public ontologies are saved to the lookup maps; the rest are just initialized and referenced from other public ontologies
                            if (mainOntology.isPublic()) {
                                //store the ontology in a lookup map
                                addPublicOntology(mainOntology);

                                // if a public ontology references another ontology, regardless of being public or not,
                                // we'll also save it in the lookup map, because we'll encounter it sooner or later
                                for (RdfOntology ref : mainOntology.getOntologyReferences()) {
                                    addPublicOntology(ref);
                                }
                            }
                        }
                        catch (Throwable e) {
                            throw new RdfInstantiationException("Error while initializing an RDF ontology - phase 2; " + entry.getKey(), e);
                        }
                    }

                    //make sure we also include the ontology of the configured label property to the public set of ontologies
                    addPublicOntology(Settings.instance().getRdfLabelProperty().getOntology());

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
    public RdfClassImpl.Builder build(RdfClass rdfProxyClass) throws RdfInitializationException
    {
        if (rdfProxyClass.isProxy()) {
            if(!this.registry.containsKey(rdfProxyClass)) {
                //note: this cast is safe because in sync with the factory method above (and a private constructor)
                this.registry.put(rdfProxyClass, new RdfClassImpl.Builder(this, ((RdfClassImpl) rdfProxyClass)));
            }

            //this cast always works, see above
            return (RdfClassImpl.Builder) this.registry.get(rdfProxyClass);
        }
        else {
            throw new RdfInitializationException("This RDF class has already been built, you can't call this method twice on the same instance; " + rdfProxyClass);
        }
    }
    /**
     * Call this method to start the un-proxy process to convert a proxy instance to a valid instance.
     * Note that you can't pass already created classes.
     */
    public RdfPropertyImpl.Builder build(RdfProperty rdfProxyProperty) throws RdfInitializationException
    {
        if (rdfProxyProperty.isProxy()) {
            if (!this.registry.containsKey(rdfProxyProperty)) {
                //note: this cast is safe because in sync with the factory method above (and a private constructor)
                this.registry.put(rdfProxyProperty, new RdfPropertyImpl.Builder(this, (RdfPropertyImpl) rdfProxyProperty));
            }
            //this cast always works, see above
            return (RdfPropertyImpl.Builder) this.registry.get(rdfProxyProperty);
        }
        else {
            throw new RdfInitializationException("This RDF class has already been built, you can't call this method twice on the same instance; " + rdfProxyProperty);
        }
    }
    /**
     * Call this method to start the un-proxy process to convert a proxy instance to a valid instance.
     * Note that you can't pass already created classes.
     */
    public RdfDatatypeImpl.Builder build(RdfDatatype rdfProxyDatatype) throws RdfInitializationException
    {
        if (rdfProxyDatatype.isProxy()) {
            if (!this.registry.containsKey(rdfProxyDatatype)) {
                //note: this cast is safe because in sync with the factory method above (and a private constructor)
                this.registry.put(rdfProxyDatatype, new RdfDatatypeImpl.Builder(this, (RdfDatatypeImpl) rdfProxyDatatype));
            }
            //this cast always works, see above
            return (RdfDatatypeImpl.Builder) this.registry.get(rdfProxyDatatype);
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
    private static Map<URI, RdfOntology> getPublicOntologyUriMap()
    {
        try {
            return R.cacheManager().getApplicationCache().getAndPutIfAbsent(CacheKeys.RDF_PUBLIC_ONTOLOGIES, new CacheFunction<CacheKey, Map<URI, RdfOntology>>()
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
            throw new RuntimeException("Error while initializing RDF ontology URI map; this shouldn't happen", e);
        }
    }
    private static Map<String, RdfOntology> getPublicOntologyPrefixMap()
    {
        try {
            return R.cacheManager().getApplicationCache().getAndPutIfAbsent(CacheKeys.RDF_PUBLIC_ONTOLOGY_PREFIXES, new CacheFunction<CacheKey, Map<String, RdfOntology>>()
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
            throw new RuntimeException("Error while initializing RDF ontology prefix map; this shouldn't happen", e);
        }
    }
    private static RdfOntologyMember parseOntologyMemberUri(String uriStr, String separator)
    {
        RdfOntologyMember retVal = null;

        int sepIdx = uriStr.lastIndexOf(separator);
        if (sepIdx >= 0 && sepIdx < uriStr.length() - separator.length()) {
            RdfOntology ontology = getPublicOntologyUriMap().get(URI.create(uriStr.substring(0, sepIdx + separator.length())));
            if (ontology != null) {
                retVal = ontology.getMember(uriStr.substring(sepIdx + separator.length()));
            }
        }

        return retVal;
    }
    /**
     * Centralized method to add ontologies to the public maps
     */
    private static void addPublicOntology(RdfOntology ontology)
    {
        if (!RdfFactory.getPublicOntologyUriMap().containsKey(ontology.getNamespace().getUri())) {
            RdfFactory.getPublicOntologyUriMap().put(ontology.getNamespace().getUri(), ontology);
            RdfFactory.getPublicOntologyPrefixMap().put(ontology.getNamespace().getPrefix(), ontology);
        }
    }
    private static void checkOntologyMembers(RdfOntologyImpl rdfOntologyImpl) throws IllegalAccessException, RdfInitializationException
    {
        // It's easy for the create() method to miss some fields, so let's help the dev
        // a little bit by iterating all members of the ontology and check if they have been
        // registered properly
        for (Field field : rdfOntologyImpl.getClass().getFields()) {
            //should we also check for a static modifier here?
            if (field.getType().isAssignableFrom(RdfOntologyMember.class)) {
                RdfOntologyMember member = (RdfOntologyMember) field.get(rdfOntologyImpl);
                if (member == null) {
                    throw new RdfInitializationException("Field inside an RDF ontology turned out null after initializing the ontology;" +
                                                         " this shouldn't happen; " + rdfOntologyImpl);
                }
                else if (member.isProxy()) {
                    throw new RdfInitializationException("Field inside an RDF ontology turned out to be still a proxy after initializing the ontology;" +
                                                         " this shouldn't happen; " + rdfOntologyImpl);
                }
            }
        }
    }
}
