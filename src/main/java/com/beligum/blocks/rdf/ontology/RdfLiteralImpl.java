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
    private String value;
    private RdfClass dataType;

    //-----CONSTRUCTORS-----
    public RdfLiteralImpl(String value, RdfClass dataType)
    {
        this(RDFS.LITERAL.getName(), RDFS.INSTANCE, value, dataType);
    }
    //only for subclasses
    protected RdfLiteralImpl(String name, RdfVocabulary vocabulary, String value, RdfClass dataType)
    {
        super();

        this.value = value;
        this.dataType = dataType;
    }

    //-----PUBLIC METHODS-----
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

}
