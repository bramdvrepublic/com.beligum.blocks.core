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

import com.beligum.blocks.config.Settings;
import com.beligum.blocks.exceptions.RdfInitializationException;
import com.beligum.blocks.index.ifaces.ResourceSummarizer;
import com.beligum.blocks.index.summarizers.SimpleResourceSummarizer;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfClassValidator;
import com.beligum.blocks.rdf.ifaces.RdfOntologyMember;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import org.apache.commons.collections.iterators.IteratorChain;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The main implementation of the RdfClass interface
 * <p>
 * Created by bram on 2/27/16.
 */
public class RdfClassImpl extends AbstractRdfOntologyMember implements RdfClass
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    protected Set<RdfClassImpl> superClasses;
    protected Set<RdfClassImpl> subClasses;
    protected Set<RdfPropertyImpl> properties;
    protected ResourceSummarizer summarizer;
    protected RdfProperty mainProperty;
    protected RdfClassValidator validator;

    //-----CONSTRUCTORS-----
    RdfClassImpl(RdfOntologyImpl ontology, String name)
    {
        super(ontology, name, false);

        //makes sense that the properties and superclasses are returned in the same order they are added, no?
        this.superClasses = new LinkedHashSet<>();
        this.subClasses = new LinkedHashSet<>();
        this.properties = new LinkedHashSet<>();
    }

    //-----PUBLIC METHODS-----
    @Override
    public Type getType()
    {
        return Type.CLASS;
    }
    @Override
    public Iterable<RdfClass> getSuperClasses()
    {
        this.assertNoProxy();

        //this does nothing more than restrict the member values to the interface
        return Iterables.transform(this.superClasses,
                                   new Function<RdfClassImpl, RdfClass>()
                                   {
                                       @Override
                                       public RdfClass apply(RdfClassImpl member)
                                       {
                                           return member;
                                       }
                                   }
        );
    }
    @Override
    public Iterable<RdfClass> getSubClasses()
    {
        this.assertNoProxy();

        //this does nothing more than restrict the member values to the interface
        return Iterables.transform(this.subClasses,
                                   new Function<RdfClassImpl, RdfClass>()
                                   {
                                       @Override
                                       public RdfClass apply(RdfClassImpl member)
                                       {
                                           return member;
                                       }
                                   }
        );
    }
    @Override
    public Iterable<RdfProperty> getProperties()
    {
        this.assertNoProxy();

        //this does nothing more than restrict the member values to the interface
        Iterable<RdfProperty> retVal = Iterables.transform(this.getPropertiesWithoutProxyCheck(),
                                   new Function<RdfPropertyImpl, RdfProperty>()
                                   {
                                       @Override
                                       public RdfProperty apply(RdfPropertyImpl member)
                                       {
                                           return member;
                                       }
                                   }
        );

        // basically, this creates an iterable with the main property removed,
        // and re-introduced as the first element (instead of sorting)
        if (this.mainProperty != null) {
            retVal = Iterables.concat(Collections.singleton(mainProperty),
                                      Iterables.filter(retVal, new Predicate<RdfProperty>()
                                      {
                                          @Override
                                          public boolean apply(@Nullable RdfProperty input)
                                          {
                                              //skip this element if it's the main property
                                              //because we introduced it as the first in the list
                                              return !input.equals(mainProperty);
                                          }
                                      })
            );
        }

        return retVal;
    }
    @Override
    public boolean hasProperty(RdfProperty property)
    {
        this.assertNoProxy();

        return hasPropertyWithoutProxyCheck(property);
    }
    @Override
    public ResourceSummarizer getSummarizer()
    {
        this.assertNoProxy();

        return summarizer;
    }
    @Override
    public RdfProperty getMainProperty()
    {
        this.assertNoProxy();

        return mainProperty;
    }
    @Override
    public RdfClassValidator getValidator()
    {
        this.assertNoProxy();

        return validator;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    /**
     * The same as getProperties(), but without the proxy check
     */
    private Iterable<RdfPropertyImpl> getPropertiesWithoutProxyCheck()
    {
        //by constructing it this way, we can keep using the references to the (proxy) superclasses
        //until this method is really called.
        Iterable<RdfPropertyImpl> retVal = this.properties;
        for (RdfClassImpl c : this.superClasses) {
            retVal = Iterables.concat(retVal, c.getPropertiesWithoutProxyCheck());
        }

        return retVal;
    }
    /**
     * The same as hasProperty(), but without the proxy check
     */
    private boolean hasPropertyWithoutProxyCheck(RdfProperty property)
    {
        return Iterables.tryFind(this.getPropertiesWithoutProxyCheck(), Predicates.equalTo(property)).isPresent();
    }

    //-----INNER CLASSES-----
    public static class Builder extends AbstractRdfOntologyMember.Builder<RdfClass, RdfClassImpl, RdfClassImpl.Builder>
    {
        Builder(RdfFactory rdfFactory, RdfClassImpl rdfClass)
        {
            super(rdfFactory, rdfClass);
        }

        public Builder superClass(RdfClass superClass) throws RdfInitializationException
        {
            return this.superClasses(superClass);
        }
        public Builder superClasses(RdfClass... superClasses) throws RdfInitializationException
        {
            for (RdfClass c : superClasses) {

                RdfClassImpl cImpl = (RdfClassImpl) c;

                if (this.rdfResource.superClasses.contains(c)) {
                    throw new RdfInitializationException("Can't add superclass " + c + " to class " + this + " because it would overwrite and existing superclass, please fix this.");
                }
                else {
                    this.rdfResource.superClasses.add(cImpl);
                    //also wire-in this class as a subclass of the superclass
                    //note that this cast should always work because it's defined in our generic signature
                    cImpl.subClasses.add(this.rdfResource);
                }
            }

            return this;
        }
        public Builder property(RdfProperty property) throws RdfInitializationException
        {
            return this.property(property, new RdfProperty.Option[0]);
        }
        public Builder property(RdfProperty property, RdfProperty.Option... options) throws RdfInitializationException
        {
            // we'll skip the overwrite check when we're dealing with the default class because overwrites will happen
            // because of the default public fields registering
            return this.addProperty(property, options, this.rdfResource.equals(Settings.DEFAULT_CLASS));
        }
        public Builder summarizer(ResourceSummarizer resourceSummarizer)
        {
            this.rdfResource.summarizer = resourceSummarizer;

            return this;
        }
        public Builder mainProperty(RdfProperty mainProperty) throws RdfInitializationException
        {
            if (!this.rdfResource.properties.contains(mainProperty)) {
                throw new RdfInitializationException("Can't set main property of class " + this + " to " + mainProperty + " because it's not a property of this class.");
            }
            else {
                this.rdfResource.mainProperty = mainProperty;
            }

            return this;
        }
        public Builder validator(RdfClassValidator validator)
        {
            this.rdfResource.validator = validator;

            return this;
        }

        @Override
        RdfClass create() throws RdfInitializationException
        {
            if (this.rdfResource.isProxy()) {
                //enforce a naming policy on the classes of our local public ontologies
                if (this.rdfResource.ontology.isPublic) {
                    if (Character.isLowerCase(this.rdfResource.name.charAt(0))) {
                        throw new RdfInitializationException("Encountered RDF class with lowercase name; our policy enforces all RDF classes should start with an uppercase letter; " + this);
                    }
                    else {
                        //every public class (in a public ontology) should at least have a few standard properties, so auto-add them if they're missing
                        if (this.rdfResource.isPublic) {

                            // If this class is public, add all default properties to it
                            // Note that the default label property and the RDF.type property are
                            // marked default automatically before calling this
                            RdfProperty labelProperty = Settings.instance().getRdfLabelProperty();
                            for (RdfOntologyMember p : this.rdfFactory.defaultMemberRegistry) {
                                if (p.isProperty()) {
                                    // If we hit the label property (note that this should always happen because the label property
                                    // is marked default in RdfFactory), we'll skip adding it if we have a main property. This will
                                    // allow us to specify another property (class-scoped instead of application-scoped) as the most
                                    // important one (eg. important for sub-resources) without automatically adding the label to
                                    // all classes (only the ones without a main property).
                                    // Note that the main property getter will return the label property if the main property is null
                                    // Also note that the default summarizer behavior is adapted to this
                                    if (p.equals(labelProperty) && this.rdfResource.mainProperty != null) {
                                        //NOOP, don't add
                                    }
                                    else {
                                        //skip the overwrite check so the addition doesn't throw an exception
                                        this.addProperty((RdfProperty) p, new RdfProperty.Option[0], true);

                                        //this will make sure the label property is the main property if no explicit main property is set
                                        if (p.equals(labelProperty) && this.rdfResource.mainProperty == null) {
                                            this.rdfResource.mainProperty = labelProperty;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                //revert to default if null (this behaviour is expected in our implementation)
                if (this.rdfResource.summarizer == null) {
                    this.rdfResource.summarizer = new SimpleResourceSummarizer();
                }

                //note: instead of iterating the properties of the superclasses and adding them to this class,
                //we overloaded the getProperties() method to include the properties of the superclass instead.
                //This way, we work around the need for the superclasses to be initialized when this resource is created.
            }

            //Note: this call will add us to the ontology
            return super.create();
        }

        private Builder addProperty(RdfProperty property, RdfProperty.Option[] options, boolean skipOverwriteCheck) throws RdfInitializationException
        {
            RdfPropertyImpl pImpl = (RdfPropertyImpl) property;

            // we never overwrite existing values, the check only decides if we throw an exception or not
            if (this.rdfResource.properties.contains(pImpl)) {
                if (skipOverwriteCheck) {
                    //NOOP don't overwrite (note that this is actually the behavior of .add() but this way it's more clear)
                }
                else {
                    throw new RdfInitializationException("Can't add property " + property + " to class " + this + " because it would overwrite and existing properties, can't continue.");
                }
            }
            else {
                // Instead of adding the property to the list of properties of the class,
                // we create a clone of the root property, so we can tweak some options of the property
                // on a per-class basis (like public, weight, ...).
                // note: if the options are empty, we might as well use the root property to save some memory (hoping the larger
                // part of the ontology won't have options)
                this.rdfResource.properties.add(options.length == 0 ? pImpl : pImpl.buildClone(options));
            }

            return this;
        }
    }
}
