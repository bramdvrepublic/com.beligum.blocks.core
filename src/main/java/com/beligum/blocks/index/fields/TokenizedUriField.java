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
    public String getValue(ResourceIndexEntry indexEntry)
    {
        //the value is the same, but it should be indexed in a different way
        return ResourceIndexEntry.uriField.getValue(indexEntry);
    }
    @Override
    public boolean hasValue(ResourceIndexEntry indexEntry)
    {
        return ResourceIndexEntry.uriField.hasValue(indexEntry);
    }
    @Override
    public void setValue(ResourceIndexEntry indexEntry, String value)
    {
        //NOOP this is a virtual field, it's value is set with setId()
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
