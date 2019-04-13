package com.beligum.blocks.index.fields;

import com.beligum.base.utils.toolkit.StringFunctions;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.index.entries.AbstractIndexEntry;
import com.beligum.blocks.index.ifaces.ResourceIndexEntry;
import com.beligum.blocks.index.ifaces.ResourceProxy;
import com.beligum.blocks.utils.RdfTools;
import org.eclipse.rdf4j.model.IRI;

import java.net.URI;

/**
 * Created by bram on Apr 12, 2019
 */
public class ImageField extends JsonField
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public ImageField()
    {
        super("image");
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getValue(ResourceProxy resourceProxy)
    {
        return this.serialize(resourceProxy.getImage());
    }
    @Override
    public boolean hasValue(ResourceProxy resourceProxy)
    {
        return resourceProxy.getImage() != null;
    }
    @Override
    public void setValue(ResourceProxy resourceProxy, String value)
    {
        if (resourceProxy instanceof AbstractIndexEntry) {
            ((AbstractIndexEntry) resourceProxy).setImage(this.deserialize(value));
        }
    }

    public String serialize(URI value)
    {
        return this.serializeUri(value);
    }
    public URI deserialize(String value)
    {
        return this.deserializeUri(value);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
