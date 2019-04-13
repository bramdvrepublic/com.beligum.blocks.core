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
public class TokenizedUriField extends JsonField
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public TokenizedUriField()
    {
        super("tokenisedUri");
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getValue(ResourceProxy resourceProxy)
    {
        //the value is the same, but it should be indexed in a different way
        return ResourceIndexEntry.uriField.getValue(resourceProxy);
    }
    @Override
    public boolean hasValue(ResourceProxy resourceProxy)
    {
        return ResourceIndexEntry.uriField.hasValue(resourceProxy);
    }
    @Override
    public void setValue(ResourceProxy resourceProxy, String value)
    {
        //NOOP this is a virtual field, it's value is set with setId()
    }
    /**
     * This is a virtual field, only needed to so index-lookups.
     * It shouldn't be included in our auto-getter/setter iterators because it's
     * configured as a copyField in Solr config
     */
    @Override
    public boolean isVirtual()
    {
        return true;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
