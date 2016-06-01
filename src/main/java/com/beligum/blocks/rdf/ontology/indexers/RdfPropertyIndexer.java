package com.beligum.blocks.rdf.ontology.indexers;

import com.beligum.blocks.fs.index.entries.RdfIndexer;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import org.openrdf.model.Value;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;

/**
 * Created by bram on 5/31/16.
 */
public interface RdfPropertyIndexer
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    Object index(RdfIndexer indexer, URI subject, RdfProperty property, Value value, Locale language) throws IOException;
    Object prepareIndexValue(RdfProperty property, String value, Locale language) throws IOException;

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
