package com.beligum.blocks.rdf.ontology.vocabularies;

import com.beligum.base.models.AbstractJsonObject;
import com.beligum.blocks.config.RdfFactory;
import com.beligum.blocks.rdf.ifaces.*;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
    private final Map<URI, RdfClass> allClasses;
    private final Map<URI, RdfClass> publicClasses;
    private final Map<URI, RdfDataType> publicDataTypes;
    private final Map<URI, RdfProperty> publicProperties;
    private final Set<RdfLiteral> publicLiterals;

    //-----CONSTRUCTORS-----
    protected AbstractRdfVocabulary(URI namespace, String prefix)
    {
        this.namespace = namespace;
        this.prefix = prefix;
        this.allClasses = new HashMap<>();
        this.publicClasses = new HashMap<>();
        this.publicDataTypes = new HashMap<>();
        this.publicProperties = new HashMap<>();
        this.publicLiterals = new HashSet<>();

        //add this vocabulary to the cached map of vocabularies
        RdfFactory.getVocabularies().put(this.getNamespace(), this);
        //store the prefix mapping
        RdfFactory.getVocabularyPrefixes().put(this.getPrefix(), this.getNamespace());
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
    public Map<URI, RdfClass> getAllClasses()
    {
        return allClasses;
    }
    @Override
    public Map<URI, RdfClass> getPublicClasses()
    {
        return publicClasses;
    }
    @Override
    public Map<URI, RdfDataType> getPublicDataTypes()
    {
        return publicDataTypes;
    }
    @Override
    public Map<URI, RdfProperty> getPublicProperties()
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
        this.allClasses.put(rdfClass.getCurieName(), rdfClass);

        if (rdfClass.isPublic()) {
            this.publicClasses.put(rdfClass.getCurieName(), rdfClass);
        }
    }
    @Override
    public void addProperty(RdfProperty rdfProperty)
    {
        if (rdfProperty.isPublic()) {
            this.publicProperties.put(rdfProperty.getCurieName(), rdfProperty);
        }
    }
    @Override
    public void addDataType(RdfDataType rdfDataType)
    {
        if (rdfDataType.isPublic()) {
            this.publicDataTypes.put(rdfDataType.getCurieName(), rdfDataType);
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
