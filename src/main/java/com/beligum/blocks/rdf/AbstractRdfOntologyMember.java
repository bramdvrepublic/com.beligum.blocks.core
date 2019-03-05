package com.beligum.blocks.rdf;

import com.beligum.base.filesystem.MessagesFileEntry;
import com.beligum.base.server.R;
import com.beligum.blocks.exceptions.RdfInitializationException;
import com.beligum.blocks.exceptions.RdfProxyException;
import com.beligum.blocks.rdf.ifaces.*;

import java.net.URI;

public abstract class AbstractRdfOntologyMember extends AbstractRdfResourceImpl implements RdfOntologyMember
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    protected boolean proxy;
    protected RdfOntology ontology;
    protected MessagesFileEntry title;
    protected MessagesFileEntry label;
    protected URI[] isSameAs;

    //-----CONSTRUCTORS-----
    protected AbstractRdfOntologyMember(String name)
    {
        super(name);

        //will be set to false when it was passed through the create() factory method
        this.proxy = true;
        //make it uniform (never null, always an array)
        this.isSameAs = new URI[] {};
    }

    //-----PUBLIC METHODS-----
    @Override
    public boolean isProxy()
    {
        return proxy;
    }
    @Override
    public RdfOntology getOntology()
    {
        this.assertNoProxy();

        return ontology;
    }
    @Override
    public URI getFullName()
    {
        this.assertNoProxy();

        return this.ontology.resolve(this.getName());
    }
    @Override
    public URI getCurieName()
    {
        this.assertNoProxy();

        return URI.create(ontology.getNamespace().getPrefix() + ":" + this.getName());
    }
    @Override
    public String getTitleKey()
    {
        this.assertNoProxy();

        return title.getCanonicalKey();
    }
    @Override
    public String getTitle()
    {
        this.assertNoProxy();

        //Note: we can't return the regular optimal locale, because this will probably be called from an admin endpoint
        return this.title == null ? null : this.title.toString(R.i18n().getOptimalRefererLocale());
    }
    @Override
    public MessagesFileEntry getTitleMessage()
    {
        this.assertNoProxy();

        return title;
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
        return "" + this.getCurieName();
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
        if (!(o instanceof RdfClassImpl)) return false;

        RdfClassImpl rdfClass = (RdfClassImpl) o;

        return getCurieName() != null ? getCurieName().equals(rdfClass.getCurieName()) : rdfClass.getCurieName() == null;
    }
    @Override
    public int hashCode()
    {
        return getCurieName() != null ? getCurieName().hashCode() : 0;
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
    protected static abstract class Builder<T extends RdfResource, V extends AbstractRdfOntologyMember, B extends AbstractRdfOntologyMember.Builder>
    {
        //-----CONSTANTS-----

        //-----VARIABLES-----
        //not really needed but let's keep a reference anyway
        protected RdfFactory rdfFactory;
        protected V rdfResource;
        private B builder;

        //-----CONSTRUCTORS-----
        protected Builder(RdfFactory rdfFactory, V rdfResource)
        {
            //by keeping it in a variable, we only need to cast once, see below
            this.builder = (B) this;

            this.rdfFactory = rdfFactory;
            this.rdfResource = rdfResource;
        }

        //-----PUBLIC METHODS-----
        public T create() throws RdfInitializationException
        {
            if (this.rdfResource.ontology == null) {
                throw new RdfInitializationException(
                                "Trying to create an RdfClass '" + this.rdfResource.getName() + "' without any ontology to connect to, can't continue because too much depends on this.");
            }
            else {
                this.rdfResource.ontology._register(this.rdfResource);
            }

            if (this.rdfResource.title == null) {
                throw new RdfInitializationException("Trying to create an RdfClass '" + this.rdfResource.getName() + "' without title, can't continue because too much depends on this.");
            }

            if (this.rdfResource.label == null) {
                this.rdfResource.label = this.rdfResource.title;
            }

            //here, all checks passed and the proxy can be converted to a valid instance
            this.rdfResource.proxy = false;

            //this cast is needed because <V extends AbstractRdfOntologyMember> instead of <V extends T>
            return (T) this.rdfResource;
        }
        public B ontology(RdfOntology ontology)
        {
            this.rdfResource.ontology = ontology;

            return this.builder;
        }
        public B isPublic(boolean isPublic)
        {
            this.rdfResource.isPublic = isPublic;

            return this.builder;
        }
        public B title(MessagesFileEntry title)
        {
            this.rdfResource.title = title;

            return this.builder;
        }
        public B label(MessagesFileEntry label)
        {
            this.rdfResource.label = label;

            return this.builder;
        }
        public B isSameAs(URI[] isSameAs)
        {
            this.rdfResource.isSameAs = isSameAs;

            return this.builder;
        }

        //-----PROTECTED METHODS-----

        //-----PRIVATE METHODS-----
    }
}
