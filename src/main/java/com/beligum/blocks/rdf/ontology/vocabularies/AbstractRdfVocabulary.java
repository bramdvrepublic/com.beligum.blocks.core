package com.beligum.blocks.rdf.ontology.vocabularies;

import com.beligum.base.database.models.AbstractJsonObject;
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
    private final Map<URI, RdfResource> allTypes;
    private final Map<URI, RdfClass> allClasses;
    private final Map<URI, RdfClass> publicClasses;
    private final Map<URI, RdfDataType> allDataTypes;
    private final Map<URI, RdfDataType> publicDataTypes;
    private final Map<URI, RdfProperty> allProperties;
    private final Map<URI, RdfProperty> publicProperties;
    private final Set<RdfLiteral> allLiterals;
    private final Set<RdfLiteral> publicLiterals;

    //-----CONSTRUCTORS-----
    protected AbstractRdfVocabulary(URI namespace, String prefix)
    {
        this.namespace = namespace;
        this.prefix = prefix;
        this.allTypes = new HashMap<>();
        this.allClasses = new HashMap<>();
        this.publicClasses = new HashMap<>();
        this.allDataTypes = new HashMap<>();
        this.publicDataTypes = new HashMap<>();
        this.allProperties = new HashMap<>();
        this.publicProperties = new HashMap<>();
        this.allLiterals = new HashSet<>();
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
    public URI resolve(String suffix)
    {
        return namespace == null ? null : namespace.resolve(suffix);
    }
    @Override
    public final String getPrefix()
    {
        return prefix;
    }
    @Override
    public URI resolveCurie(String suffix)
    {
        return URI.create(this.prefix+":"+suffix);
    }
    @Override
    public Map<URI, RdfResource> getAllTypes()
    {
        return allTypes;
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
    public Map<URI, RdfDataType> getAllDataTypes()
    {
        return allDataTypes;
    }
    @Override
    public Map<URI, RdfDataType> getPublicDataTypes()
    {
        return publicDataTypes;
    }
    @Override
    public Map<URI, RdfProperty> getAllProperties()
    {
        return allProperties;
    }
    @Override
    public Map<URI, RdfProperty> getPublicProperties()
    {
        return publicProperties;
    }
    @Override
    public Set<RdfLiteral> getAllLiterals()
    {
        return allLiterals;
    }
    @Override
    public Set<RdfLiteral> getPublicLiterals()
    {
        return publicLiterals;
    }
    @Override
    public void addClass(RdfClass rdfClass)
    {
        this.allTypes.put(rdfClass.getCurieName(), rdfClass);

        this.allClasses.put(rdfClass.getCurieName(), rdfClass);
        if (rdfClass.isPublic()) {
            this.publicClasses.put(rdfClass.getCurieName(), rdfClass);
        }
    }
    @Override
    public void addProperty(RdfProperty rdfProperty)
    {
        this.allTypes.put(rdfProperty.getCurieName(), rdfProperty);

        this.allProperties.put(rdfProperty.getCurieName(), rdfProperty);
        if (rdfProperty.isPublic()) {
            this.publicProperties.put(rdfProperty.getCurieName(), rdfProperty);
        }
    }
    @Override
    public void addDataType(RdfDataType rdfDataType)
    {
        this.allTypes.put(rdfDataType.getCurieName(), rdfDataType);

        this.allDataTypes.put(rdfDataType.getCurieName(), rdfDataType);
        if (rdfDataType.isPublic()) {
            this.publicDataTypes.put(rdfDataType.getCurieName(), rdfDataType);
        }
    }
    @Override
    public void addLiteral(RdfLiteral rdfLiteral)
    {
        this.allLiterals.add(rdfLiteral);
        if (rdfLiteral.isPublic()) {
            this.publicLiterals.add(rdfLiteral);
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
