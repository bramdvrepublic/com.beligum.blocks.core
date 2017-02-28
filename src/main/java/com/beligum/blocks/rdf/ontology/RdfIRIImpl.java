package com.beligum.blocks.rdf.ontology;

import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfIRI;
import com.beligum.blocks.rdf.ifaces.RdfVocabulary;
import com.beligum.blocks.rdf.ontology.vocabularies.RDFS;

import java.net.URI;

/**
 * Created by bram on 3/2/16.
 */
public class RdfIRIImpl extends AbstractRdfResourceImpl implements RdfIRI
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private String name;
    private URI value;
    private RdfClass dataType;

    //-----CONSTRUCTORS-----
    public RdfIRIImpl(URI value, RdfClass dataType)
    {
        this(RDFS.RESOURCE.getName(), RDFS.INSTANCE, value, dataType, false);
    }
    //only for subclasses
    protected RdfIRIImpl(String name, RdfVocabulary vocabulary, URI value, RdfClass dataType, boolean isPublic)
    {
        super(isPublic);

        this.name = name;
        this.value = value;
        this.dataType = dataType;

        //Don't know if we should add this to to the vocabulary...
        //we'll add ourself and the subclass to the literal collection of the vocab
        //vocabulary.addLiteral(this);
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getName()
    {
        return name;
    }
    @Override
    public String getValue()
    {
        return value==null?null:value.toString();
    }
    @Override
    public RdfClass getDataType()
    {
        return dataType;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----
    @Override
    public String toString()
    {
        return this.getValue()==null?null:this.getValue().toString();
    }
}
