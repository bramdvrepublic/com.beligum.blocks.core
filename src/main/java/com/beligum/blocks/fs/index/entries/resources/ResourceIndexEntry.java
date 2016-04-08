package com.beligum.blocks.fs.index.entries.resources;

import com.beligum.blocks.fs.index.entries.IndexEntry;
import org.openrdf.model.Value;

import java.net.URI;
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
    Map<URI, Value> getProperties();

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
