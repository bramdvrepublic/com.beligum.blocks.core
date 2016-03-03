package com.beligum.blocks.rdf.ontology;

import com.beligum.blocks.rdf.ifaces.RdfDataType;
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
    private String value;
    private RdfDataType dataType;

    //-----CONSTRUCTORS-----
    public RdfLiteralImpl(String value, RdfDataType dataType)
    {
        this(RDFS.LITERAL.getName(), RDFS.INSTANCE, value, dataType);
    }
    //only for subclasses
    protected RdfLiteralImpl(String name, RdfVocabulary vocabulary, String value, RdfDataType dataType)
    {
        super(name, vocabulary);

        this.value = null;
        this.dataType = null;
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getValue()
    {
        return value;
    }
    @Override
    public RdfDataType getDataType()
    {
        return dataType;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
