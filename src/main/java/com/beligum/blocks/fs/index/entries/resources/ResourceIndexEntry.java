package com.beligum.blocks.fs.index.entries.resources;

import com.beligum.blocks.fs.index.entries.IndexEntry;
import com.beligum.blocks.rdf.ifaces.RdfClass;
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
    Map<RdfClass, Value> getProperties();

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
