package com.beligum.blocks.rdf.ontology.vocabularies;

import com.beligum.base.models.AbstractJsonObject;
import com.beligum.blocks.rdf.ifaces.*;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by bram on 2/28/16.
 */
public abstract class AbstractRdfVocabulary extends AbstractJsonObject implements RdfVocabulary
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private final URI namespace;
    private final String prefix;
    private final Set<RdfClass> publicClasses;
    private final Set<RdfDataType> publicDataTypes;
    private final Set<RdfProperty> publicProperties;
    private final Set<RdfLiteral> publicLiterals;

    //-----CONSTRUCTORS-----
    protected AbstractRdfVocabulary(URI namespace, String prefix)
    {
        this.namespace = namespace;
        this.prefix = prefix;
        this.publicClasses = new HashSet<>();
        this.publicDataTypes = new HashSet<>();
        this.publicProperties = new HashSet<>();
        this.publicLiterals = new HashSet<>();
    }

    //-----PUBLIC METHODS-----
    @Override
    public URI getNamespace()
    {
        return namespace;
    }
    @Override
    public final String getPrefix()
    {
        return prefix;
    }
    @Override
    public Set<RdfClass> getPublicClasses()
    {
        return publicClasses;
    }
    @Override
    public Set<RdfDataType> getPublicDataTypes()
    {
        return publicDataTypes;
    }
    @Override
    public Set<RdfProperty> getPublicProperties()
    {
        return publicProperties;
    }
    @Override
    public Set<RdfLiteral> getPublicLiterals()
    {
        return publicLiterals;
    }
    @Override
    public void addClass(RdfClass rdfClass)
    {
        if (rdfClass.isPublic()) {
            this.publicClasses.add(rdfClass);
        }
    }
    @Override
    public void addProperty(RdfProperty rdfProperty)
    {
        if (rdfProperty.isPublic()) {
            this.publicProperties.add(rdfProperty);
        }
    }
    @Override
    public void addDataType(RdfDataType rdfDataType)
    {
        if (rdfDataType.isPublic()) {
            this.publicDataTypes.add(rdfDataType);
        }
    }
    @Override
    public void addLiteral(RdfLiteral rdfLiteral)
    {
        if (rdfLiteral.isPublic()) {
            this.publicLiterals.add(rdfLiteral);
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
