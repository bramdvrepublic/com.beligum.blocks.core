package com.beligum.blocks.fs.index.entries.resources;

import com.beligum.blocks.rdf.ifaces.RdfResource;
import org.openrdf.model.Value;

import java.net.URI;
import java.util.Map;

/**
 * Created by bram on 4/7/16.
 */
public class SimpleResourceIndexEntry implements ResourceIndexEntry
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private URI id;
    private String title;
    private String description;
    private URI image;

    //-----CONSTRUCTORS-----
    public SimpleResourceIndexEntry(URI id, String title, String description, URI image)
    {
        this.id = id;
        this.title = title;
        this.description = description;
        this.image = image;
    }

    //-----PUBLIC METHODS-----
    @Override
    public URI getId()
    {
        return id;
    }
    @Override
    public Map<RdfResource, Value> getProperties()
    {
        return null;
    }
    @Override
    public String getTitle()
    {
        return title;
    }
    @Override
    public String getDescription()
    {
        return description;
    }
    @Override
    public URI getImage()
    {
        return image;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
