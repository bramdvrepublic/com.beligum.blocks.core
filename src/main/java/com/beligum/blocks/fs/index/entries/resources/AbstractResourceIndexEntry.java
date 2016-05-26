package com.beligum.blocks.fs.index.entries.resources;

import com.beligum.blocks.rdf.ifaces.RdfResource;
import com.beligum.blocks.rdf.ontology.factories.Terms;
import org.apache.commons.lang3.StringUtils;
import org.openrdf.model.Value;

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
    private String subject;
    private Map<RdfResource, Value> properties;

    //-----CONSTRUCTORS-----
    /**
     * !!! Note !!! this constructor is expected in com.beligum.blocks.fs.index.buildResourceEntry(), so watch out if you change it's signature!
     */
    protected AbstractResourceIndexEntry(URI subjectOrId)
    {
        this.subject = subjectOrId == null ? null : subjectOrId.toString();
        this.properties = new HashMap<>();
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getId()
    {
        return subject;
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
        if (image != null) {
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
    public String getImage()
    {
        String retVal = null;

        Value image = this.getProperties().get(Terms.image);
        if (image != null && !StringUtils.isEmpty(image.stringValue())) {
            retVal = image.stringValue();
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
