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
    private String id;
    private String title;
    private String description;
    private String image;

    //-----CONSTRUCTORS-----
    public SimpleResourceIndexEntry(URI id, String title, String description, URI image)
    {
        this.id = id == null ? null : id.toString();
        this.title = title;
        this.description = description;
        this.image = image == null ? null : image.toString();
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getId()
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
    public String getImage()
    {
        return image;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
