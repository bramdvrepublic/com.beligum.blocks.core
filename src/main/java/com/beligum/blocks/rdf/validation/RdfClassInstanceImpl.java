package com.beligum.blocks.rdf.validation;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.exceptions.RdfValidationException;
import com.beligum.blocks.rdf.RdfFactory;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfOntologyMember;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.validation.ifaces.RdfClassInstance;
import com.beligum.blocks.rdf.validation.ifaces.RdfPropertyInstance;
import gen.com.beligum.blocks.core.messages.blocks.core;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;

import java.net.URI;
import java.util.*;

/**
 * Created by bram on May 21, 2019
 */
public class RdfClassInstanceImpl implements RdfClassInstance
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private final URI subject;
    private RdfClass type;
    private List<RdfPropertyInstance> properties;
    private Map<RdfProperty, RdfStat> propertyStats;

    //-----CONSTRUCTORS-----
    public RdfClassInstanceImpl(URI subject)
    {
        this.subject = subject;
        this.properties = new ArrayList<>();
        this.propertyStats = new HashMap<>();
    }

    //-----PUBLIC METHODS-----
    @Override
    public URI getSubject()
    {
        return subject;
    }
    @Override
    public RdfClass getType()
    {
        return type;
    }
    @Override
    public Iterable<RdfPropertyInstance> getProperties()
    {
        return properties;
    }
    @Override
    public void validate() throws RdfValidationException
    {
        // first of all, if this entire class has a validator, it must be valid
        // Note that we can't validate classes with no type. This will happen for eg. the public pages (ie. not the RDF resources described by them)
        // because they are included here since they will always have the rdfa:usesVocabulary property
        if (this.getType() != null && this.getType().getValidator() != null) {
            this.getType().getValidator().validate(this);
        }

        // now iterate all properties and make sure their multiplicity is correct
        // and they are valid when a validator is present
        for (RdfPropertyInstance p : this.properties) {

            //first, we check if this property doesn't occur more than the allowed times inside this class instance
            RdfStat propertyStats = this.propertyStats.get(p.getType());
            if (propertyStats.count > p.getType().getMultiplicity()) {
                throw new RdfValidationException(core.Entries.rdfPropertyMultiplicityError, p.getType(), propertyStats.count, this.getType(), p.getType().getMultiplicity());
            }

            //make sure the property instance is valid
            p.validate();
        }
    }

    //-----PROTECTED METHODS-----
    protected void addStatement(Statement stmt)
    {
        RdfProperty property = RdfFactory.getProperty(stmt.getPredicate());

        if (property != null) {
            // Note: we treat the type separately
            if (property.equals(com.beligum.blocks.rdf.ontologies.RDF.type)) {
                RdfClass rdfClass = RdfFactory.getClass((IRI) stmt.getObject());
                if (rdfClass != null) {
                    this.type = rdfClass;
                }
                else {
                    Logger.warn("Encountered RDF type that doesn't map to a known class, watch out; " + stmt);
                }
            }
            else {

                this.properties.add(new RdfPropertyInstanceImpl(this, property, stmt.getObject()));

                if (!this.propertyStats.containsKey(property)) {
                    this.propertyStats.put(property, new RdfStat());
                }
                this.propertyStats.get(property).add(property);
            }
        }
        else {
            Logger.warn("Encountered RDF predicate that doesn't map to a known property, watch out; " + stmt);
        }
    }

    //-----PRIVATE METHODS-----

    //-----INNER CLASSES-----
    private static class RdfStat
    {
        private int count;

        public RdfStat()
        {
            this.count = 0;
        }
        public void add(RdfOntologyMember member)
        {
            this.count++;
        }
    }
}
