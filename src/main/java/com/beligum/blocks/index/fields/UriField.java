package com.beligum.blocks.index.fields;

import com.beligum.base.server.R;
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
public class UriField extends JsonField
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public UriField()
    {
        super("uri");
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getValue(ResourceIndexEntry indexEntry)
    {
        return this.serialize(indexEntry.getUri());
    }
    @Override
    public boolean hasValue(ResourceIndexEntry indexEntry)
    {
        return ((AbstractIndexEntry)indexEntry).hasUri();
    }
    @Override
    public void setValue(ResourceIndexEntry indexEntry, String value)
    {
        ((AbstractIndexEntry)indexEntry).setUri(this.deserialize(value));
    }

    public URI create(IRI iri)
    {
        return RdfTools.iriToUri(iri);
    }
    public URI create(Page page)
    {
        return page.getPublicRelativeAddress();
    }
    public String serialize(Page page)
    {
        return this.serialize(this.create(page));
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
