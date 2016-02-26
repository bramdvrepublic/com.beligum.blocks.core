package com.beligum.blocks.rdf.ontology;

import gen.com.beligum.blocks.core.constants.blocks.core;

import java.net.URI;

/**
 * Created by bram on 2/25/16.
 */
public class Terms
{
    public static final Term postalCode = new Term(URI.create("postalCode"),
                                                   gen.com.beligum.blocks.core.messages.blocks.core.Entries.ontologyTerm_postalCode,
                                                   core.Entries.DATATYPE_TEXT,
                                                   core.Entries.DATATYPE_WIDGET_INLINE_EDITOR,
                                                   new URI[] { URI.create("http://schema.org/postalCode"),
                                                               URI.create("http://dbpedia.org/ontology/postalCode")
                                                   });

    public static final Term isVerified = new Term(URI.create("isVerified"),
                                                   gen.com.beligum.blocks.core.messages.blocks.core.Entries.ontologyTerm_isVerified,
                                                   gen.com.beligum.blocks.core.constants.blocks.core.Entries.DATATYPE_BOOLEAN,
                                                   gen.com.beligum.blocks.core.constants.blocks.core.Entries.DATATYPE_WIDGET_TOGGLE,
                                                   null);

}
