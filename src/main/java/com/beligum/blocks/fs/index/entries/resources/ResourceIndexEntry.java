package com.beligum.blocks.fs.index.entries.resources;

import com.beligum.blocks.fs.index.entries.IndexEntry;
import com.beligum.blocks.rdf.ifaces.RdfResource;
import org.openrdf.model.Value;

import java.util.Map;

/**
 * Created by bram on 4/7/16.
 */
public interface ResourceIndexEntry extends IndexEntry
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----

    /**
     * These are the predicate-object mappings that are returned from the triple store.
     * Note that raw predicates that are unknown to the local server ontology are ignored.
     * Also, in the current implementation, double predicate mappings are overwritten (without any specific order).
     * This is actually a bug and a TODO
     */
    Map<RdfResource, Value> getProperties();


    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
