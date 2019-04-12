package com.beligum.blocks.index.fields;

import com.beligum.base.utils.toolkit.StringFunctions;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.index.entries.AbstractIndexEntry;
import com.beligum.blocks.index.ifaces.ResourceIndexEntry;
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
    public String getValue(ResourceIndexEntry indexEntry)
    {
        return this.serialize(indexEntry.getImage());
    }
    @Override
    public boolean hasValue(ResourceIndexEntry indexEntry)
    {
        return ((AbstractIndexEntry)indexEntry).hasImage();
    }
    @Override
    public void setValue(ResourceIndexEntry indexEntry, String value)
    {
        ((AbstractIndexEntry)indexEntry).setImage(this.deserialize(value));
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
