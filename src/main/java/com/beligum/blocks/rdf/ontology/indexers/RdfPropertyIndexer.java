package com.beligum.blocks.rdf.ontology.indexers;

import com.beligum.blocks.filesystem.index.entries.RdfIndexer;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import org.eclipse.rdf4j.model.Value;

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
    /**
     * This method gets called when an RDF property is indexed by the indexer.
     * It should call the right method on the indexer to index the property value as closely as possible.
     * @return the value-object as it was indexed
     * @see com.beligum.blocks.rdf.ifaces.RdfProperty
     */
    RdfIndexer.IndexResult index(RdfIndexer indexer, URI subject, RdfProperty property, Value value, Locale language) throws IOException;

    /**
     * Converts the supplied value to an object to be used during index lookups
     * @see com.beligum.blocks.rdf.ifaces.RdfProperty
     */
    Object prepareIndexValue(RdfProperty property, String value, Locale language) throws IOException;

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
