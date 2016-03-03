package com.beligum.blocks.rdf.ontology.vocabularies;

import com.beligum.base.models.AbstractJsonObject;
import com.beligum.base.utils.Logger;
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
    private final Set<RdfClass> classes;
    private final Set<RdfDataType> dataTypes;
    private final Set<RdfProperty> properties;
    private final Set<RdfLiteral> literals;

    //-----CONSTRUCTORS-----
    protected AbstractRdfVocabulary(URI namespace, String prefix)
    {
        this.namespace = namespace;
        this.prefix = prefix;
        this.classes = new HashSet<>();
        this.dataTypes = new HashSet<>();
        this.properties = new HashSet<>();
        this.literals = new HashSet<>();
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
    public Set<RdfClass> getClasses()
    {
        return classes;
    }
    @Override
    public Set<RdfDataType> getDataTypes()
    {
        return dataTypes;
    }
    @Override
    public Set<RdfProperty> getProperties()
    {
        return properties;
    }
    @Override
    public Set<RdfLiteral> getLiterals()
    {
        return literals;
    }
    @Override
    public void add(RdfResource rdfResource)
    {
        if (rdfResource instanceof RdfClass) {
            this.classes.add((RdfClass) rdfResource);
        }
        else if (rdfResource instanceof RdfDataType) {
            this.dataTypes.add((RdfDataType) rdfResource);
        }
        else if (rdfResource instanceof RdfProperty) {
            this.properties.add((RdfProperty) rdfResource);
        }
        else if (rdfResource instanceof RdfLiteral) {
            this.literals.add((RdfLiteral) rdfResource);
        }
        else {
            Logger.error("Unknown RDF resource type encountered, not adding to vocabulary; "+rdfResource.getClass());
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
