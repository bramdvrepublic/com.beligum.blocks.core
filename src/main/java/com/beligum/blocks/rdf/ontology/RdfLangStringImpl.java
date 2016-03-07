package com.beligum.blocks.rdf.ontology;

import com.beligum.blocks.rdf.ifaces.RdfLangString;
import com.beligum.blocks.rdf.ontology.vocabularies.RDF;

import java.util.Locale;

/**
 * Created by bram on 3/2/16.
 */
public class RdfLangStringImpl extends RdfLiteralImpl implements RdfLangString
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private Locale language;

    //-----CONSTRUCTORS-----
    public RdfLangStringImpl(String value, Locale language)
    {
        this(value, language, false);
    }
    public RdfLangStringImpl(String value, Locale language, boolean isPublic)
    {
        super(RDF.LANGSTRING.getName(), RDF.INSTANCE, value, RDF.LANGSTRING, isPublic);

        this.language = language;
    }

    //-----PUBLIC METHODS-----
    @Override
    public Locale getLanguage()
    {
        return language;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
