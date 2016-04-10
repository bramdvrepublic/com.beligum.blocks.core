package com.beligum.blocks.fs.index.entries.resources;

import com.beligum.blocks.fs.index.entries.IndexEntry;
import com.beligum.blocks.rdf.ifaces.RdfResource;
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

    /**
     * These are the predicate-object mappings that are returned from the triple store.
     * Note that raw predicates that are unknown to the local server ontology are ignored.
     * Also, in the current implementation, double predicate mappings are overwritten (without any specific order).
     * This is actually a bug and a TODO
     */
    Map<RdfResource, Value> getProperties();

    /**
     * The link to this resource. Should generally be the same as getId(), but this separation enables us to make it different if needed.
     */
    URI getLink();

    /**
     * The title of this resource, to be used directly in the HTML that is returned to the client.
     * So, in the right language and format. Mainly used to build eg. search result lists.
     * Try not to return null or "".
     */
    String getTitle();

    /**
     * The description of this resource, to be used directly in the HTML that is returned to the client.
     * So, in the right language and format. Mainly used to build eg. search result lists.
     * Might be empty or null.
     */
    String getDescription();

    /**
     * A link to the main image that describes this resource, mainly used to build eg. search result lists.
     * Might be null.
     */
    URI getImage();


    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
