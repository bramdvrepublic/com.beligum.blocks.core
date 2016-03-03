package com.beligum.blocks.rdf.ontology;

import com.beligum.base.models.AbstractJsonObject;
import com.beligum.blocks.rdf.ifaces.RdfResource;
import com.beligum.blocks.rdf.ifaces.RdfVocabulary;

import java.net.URI;

/**
 * Created by bram on 3/3/16.
 */
public abstract class AbstractRdfResourceImpl extends AbstractJsonObject implements RdfResource
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private String name;
    private RdfVocabulary vocabulary;

    //-----CONSTRUCTORS-----
    protected AbstractRdfResourceImpl(String name, RdfVocabulary vocabulary)
    {
        this.name = name;
        this.vocabulary = vocabulary;

        //add ourself to the selected vocabulary
        this.vocabulary.add(this);
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getName()
    {
        return name;
    }
    @Override
    public RdfVocabulary getVocabulary()
    {
        return vocabulary;
    }
    @Override
    public URI getFullName()
    {
        return vocabulary.getNamespace().resolve(name);
    }
    @Override
    public URI getCurieName()
    {
        return URI.create(vocabulary.getPrefix() + ":" + name);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
