package com.beligum.blocks.rdf;

import com.beligum.base.filesystem.MessagesFileEntry;
import com.beligum.base.server.R;
import com.beligum.blocks.exceptions.RdfInitializationException;
import com.beligum.blocks.exceptions.RdfInstantiationException;
import com.beligum.blocks.exceptions.RdfProxyException;
import com.beligum.blocks.rdf.ifaces.*;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Set;

public abstract class AbstractRdfOntologyMember extends AbstractRdfResourceImpl implements RdfOntologyMember
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    protected RdfOntologyImpl ontology;
    protected final String name;
    protected boolean proxy;
    protected MessagesFileEntry label;
    protected URI[] isSameAs;
    protected boolean isDefault;
    protected RdfEndpoint endpoint;
    protected int weight;
    // WARNING: when added fields, check RdfPropertyImpl.initFromToClone()

    //-----CONSTRUCTORS-----
    protected AbstractRdfOntologyMember(RdfOntologyImpl ontology, String name, boolean isPublic)
    {
        super();

        if (ontology == null) {
            throw new RdfInstantiationException("Can't create a RDF ontology member without an ontology; " + this);
        }
        if (StringUtils.isEmpty(name)) {
            throw new RdfInstantiationException("Can't create a RDF ontology member without a name; " + this);
        }

        this.ontology = ontology;
        this.name = name;
        this.isPublic = isPublic;

        //will be set to false when it was passed through the create() factory method
        this.proxy = true;
        //make it uniform (never null, always an array)
        this.isSameAs = new URI[] {};
        //this should be activated
        this.isDefault = false;
        //just default to zero
        this.weight = DEFAULT_WEIGHT;
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getName()
    {
        return name;
    }
    @Override
    public URI getUri()
    {
        return this.ontology.resolve(this.getName());
    }
    @Override
    public URI getCurie()
    {
        return URI.create(ontology.getNamespace().getPrefix() + ":" + this.getName());
    }
    @Override
    public boolean isProxy()
    {
        return proxy;
    }
    @Override
    public RdfOntology getOntology()
    {
        return ontology;
    }
    @Override
    public String getLabelKey()
    {
        this.assertNoProxy();

        return label.getCanonicalKey();
    }
    @Override
    public String getLabel()
    {
        this.assertNoProxy();

        //Note: we can't return the regular optimal locale, because this will probably be called from an admin endpoint
        return this.label == null ? null : this.label.toString(R.i18n().getOptimalRefererLocale());
    }
    @Override
    public MessagesFileEntry getLabelMessage()
    {
        this.assertNoProxy();

        return label;
    }
    @Override
    public URI[] getIsSameAs()
    {
        this.assertNoProxy();

        return isSameAs;
    }
    @Override
    public boolean isDefault()
    {
        return isDefault;
    }
    @Override
    public RdfEndpoint getEndpoint()
    {
        this.assertNoProxy();

        return endpoint;
    }
    @Override
    public int getWeight()
    {
        this.assertNoProxy();

        return weight;
    }

    //-----PROTECTED METHODS-----
    protected void assertNoProxy()
    {
        if (this.isProxy()) {
            throw new RdfProxyException("A core functionality method of an RDF ontology member was called without properly initializing it; please see RdfFactory comments for details; " + this);
        }
    }

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----
    @Override
    public String toString()
    {
        return "" + this.getCurie();
    }
    /**
     * Note that we overload the equals() method of AbstractRdfResourceImpl to use the CURIE instead of the name
     * because two classes with the same name, but in different ontologies are not the same thing, but I guess
     * we can assume two classes (or properties or datatypes) with the same CURIE to be equal, right?
     */
    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof AbstractRdfOntologyMember)) return false;

        AbstractRdfOntologyMember that = (AbstractRdfOntologyMember) o;

        return getCurie() != null ? getCurie().equals(that.getCurie()) : that.getCurie() == null;

    }
    @Override
    public int hashCode()
    {
        return getCurie() != null ? getCurie().hashCode() : 0;
    }

    //-----INNER CLASSES-----

    /**
     * Factory/builder pattern that maps an RDF implementation (V) to an RDF interface (T), while offering a means to hide away
     * certain setters of the superclass (eg. hiding the superClass setter in RdfClass while building a RdfProperty)
     * and offering a means to expose a cleaner API to the developer.
     * On top, we can control (eg. validate) the creation of objects in the create() method  in future implementations
     * (eg. overloaded in subclasses) so no instances with wrong settings are forbidden/blocked.
     * Note that the RdfFactory instance is only accessible from the RdfFactory initialization method, so we have guaranteed
     * control over the creation of RdfResource-instances (package-private constructors)
     */
    protected static abstract class Builder<T extends RdfOntologyMember, V extends AbstractRdfOntologyMember, B extends AbstractRdfOntologyMember.Builder>
    {
        //-----CONSTANTS-----

        //-----VARIABLES-----
        //not really needed but let's keep a reference anyway
        protected RdfFactory rdfFactory;
        protected V rdfResource;
        private B builder;
        private Set<T> sameAs;

        //-----CONSTRUCTORS-----
        protected Builder(RdfFactory rdfFactory, V rdfResource)
        {
            //by keeping it in a variable, we only need to cast once, see below
            this.builder = (B) this;
            this.sameAs = new LinkedHashSet<>();

            this.rdfFactory = rdfFactory;
            this.rdfResource = rdfResource;

            if (this.rdfResource.ontology != rdfFactory.ontology) {
                this.rdfResource.ontology = rdfFactory.ontology;
            }
        }

        //-----PUBLIC METHODS-----
        public B isPublic(boolean isPublic)
        {
            this.rdfResource.isPublic = isPublic;

            return this.builder;
        }
        public B label(MessagesFileEntry label)
        {
            this.rdfResource.label = label;

            return this.builder;
        }
        public B isSameAs(T isSameAs)
        {
            this.sameAs.add(isSameAs);

            return this.builder;
        }
        public B isDefault(boolean isDefault)
        {
            this.rdfResource.isDefault = isDefault;

            //by putting all default members in a central registry, we avoid having to iterate it again after first phase
            //now, we can just check this registry in create() below
            if (isDefault) {
                this.rdfFactory.defaultMemberRegistry.add(this.rdfResource);
            }

            return this.builder;
        }
        public B endpoint(RdfEndpoint endpoint)
        {
            this.rdfResource.endpoint = endpoint;

            return this.builder;
        }
        public B weight(int weight)
        {
            this.rdfResource.weight = weight;

            return this.builder;
        }

        //-----PROTECTED METHODS-----
        /**
         * The ontology passed here is the actual main ontology for this member and can differ from the
         * ontology instance the member belongs to because of our modular-ontologies-design.
         * <p>
         * Note this method is package-private because it's called automatically for every new member
         * in the RdfOntology constructor.
         */
        T create() throws RdfInitializationException
        {
            if (this.rdfResource.isProxy()) {

                if (this.rdfResource.ontology == null) {
                    throw new RdfInitializationException("Trying to create an RdfClass '" + this.rdfResource.getName() + "' without ontology, can't continue because too much depends on this.");
                }

                //validate the multiplicity
                if (this.rdfResource.weight < MINIMUM_WEIGHT || this.rdfResource.weight > MAXIMUM_WEIGHT) {
                    throw new RdfInitializationException("Encountered RDF ontology member '" + this.rdfResource.getName() + "' with an invalid weight value; " + this.rdfResource.weight);
                }

                //convert the RdfOntologyMember set to an array of URI
                int i = 0;
                this.rdfResource.isSameAs = new URI[this.sameAs.size()];
                for (T m : this.sameAs) {
                    this.rdfResource.isSameAs[i++] = m.getUri();
                }

                // --- here, we all done initializing/checking the resource ---

                //here, all checks passed and the proxy can be converted to a valid instance
                this.rdfResource.proxy = false;

                // register the member into it's ontology, filling all the right mappings
                // note that this needs to happen after creation (proxy = false) because it will
                // call public methods on the resource and they are programmed to throw exceptions
                // when the instance is still a proxy
                this.rdfResource.ontology._register(this.rdfResource);
            }

            //this cast is needed because <V extends AbstractRdfOntologyMember> instead of <V extends T>
            return (T) this.rdfResource;
        }

        //-----PRIVATE METHODS-----

        //-----MGMT METHODS-----
        @Override
        public String toString()
        {
            return "Builder for " + rdfResource;
        }
    }
}
