package com.beligum.blocks.fs.index.entries.resources;

import com.beligum.blocks.rdf.ifaces.RdfResource;
import com.beligum.blocks.rdf.ontology.factories.Terms;
import org.apache.commons.lang3.StringUtils;
import org.openrdf.model.Value;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by bram on 4/7/16.
 */
public abstract class AbstractResourceIndexEntry implements ResourceIndexEntry
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private URI subject;
    private Map<RdfResource, Value> properties;

    //-----CONSTRUCTORS-----
    /**
     * !!! Note !!! this constructor is expected in com.beligum.blocks.fs.index.buildResourceEntry(), so watch out if you change it's signature!
     */
    protected AbstractResourceIndexEntry(URI subject) throws IOException
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
    @Override
    public URI getLink()
    {
        return this.getId();
    }
    @Override
    public Map<RdfResource, Value> getProperties()
    {
        return properties;
    }
    @Override
    public String getTitle()
    {
        String retVal = null;

        Value image = this.getProperties().get(Terms.title);
        if (image!=null) {
            retVal = image.stringValue();
        }

        return retVal;
    }
    @Override
    public String getDescription()
    {
        return null;
    }
    @Override
    public URI getImage()
    {
        URI retVal = null;

        Value image = this.getProperties().get(Terms.image);
        if (image!=null && !StringUtils.isEmpty(image.stringValue())) {
            retVal = URI.create(image.stringValue());
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
