package com.beligum.blocks.fs.index.entries.resources;

import com.beligum.blocks.rdf.ifaces.RdfClass;
import org.openrdf.model.Value;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by bram on 4/7/16.
 */
public class SimpleResourceIndexEntry implements ResourceIndexEntry
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private URI subject;
    private Map<RdfClass, Value> properties;

    //-----CONSTRUCTORS-----
    public SimpleResourceIndexEntry(URI subject) throws IOException
    {
        this.subject = subject;
        this.properties = new HashMap<>();
    }

    //-----PUBLIC METHODS-----
    @Override
    public URI getId()
    {
        return subject;
    }
    public Map<RdfClass, Value> getProperties()
    {
        return properties;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
