package com.beligum.blocks.rdf.ontology;

import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfLiteral;
import com.beligum.blocks.rdf.ifaces.RdfVocabulary;
import com.beligum.blocks.rdf.ontology.vocabularies.RDFS;

/**
 * Created by bram on 3/2/16.
 */
public class RdfLiteralImpl extends AbstractRdfResourceImpl implements RdfLiteral
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private String name;
    private String value;
    private RdfClass dataType;

    //-----CONSTRUCTORS-----
    public RdfLiteralImpl(String value, RdfClass dataType)
    {
        this(RDFS.LITERAL.getName(), RDFS.INSTANCE, value, dataType, false);
    }
    //only for subclasses
    protected RdfLiteralImpl(String name, RdfVocabulary vocabulary, String value, RdfClass dataType, boolean isPublic)
    {
        super(isPublic);

        this.name = name;
        this.value = value;
        this.dataType = dataType;

        //we'll add ourself and the subclass to the literal collection of the vocab
        vocabulary.addLiteral(this);
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
        return value;
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
        return this.getValue();
    }
}
