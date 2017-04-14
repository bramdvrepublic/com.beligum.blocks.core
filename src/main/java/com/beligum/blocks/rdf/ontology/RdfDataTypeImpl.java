package com.beligum.blocks.rdf.ontology;

import com.beligum.base.filesystem.MessagesFileEntry;
import com.beligum.blocks.rdf.ifaces.RdfDataType;
import com.beligum.blocks.rdf.ifaces.RdfVocabulary;

import java.net.URI;

/**
 * Created by bram on 2/25/16.
 */
public class RdfDataTypeImpl extends RdfClassImpl implements RdfDataType
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public RdfDataTypeImpl(String name,
                           RdfVocabulary vocabulary,
                           MessagesFileEntry title,
                           MessagesFileEntry label,
                           URI[] isSameAs)
    {
        super(name, vocabulary, title, label, isSameAs);

        //we don't have subclasses so don't worry about type checking (yet)
        vocabulary.addDataType(this);
    }

    //-----PUBLIC METHODS-----
    @Override
    public Type getType()
    {
        return Type.DATATYPE;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
