package com.beligum.blocks.endpoints.utils;

import com.beligum.base.resources.MimeTypes;
import com.beligum.base.resources.ResourceInputStream;
import com.beligum.base.resources.ifaces.MimeType;
import com.beligum.base.resources.ifaces.Resource;
import com.beligum.base.resources.mappers.FilteredResource;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.rdf.ifaces.Format;

import java.io.*;

public class PageRdfResource extends FilteredResource
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private Page page;
    private Format rdfFormat;

    //-----CONSTRUCTORS-----
    public PageRdfResource(Page page, Format rdfFormat)
    {
        super(page);

        this.page = page;
        this.rdfFormat = rdfFormat;
    }

    //-----PUBLIC METHODS-----
    @Override
    public ResourceInputStream newInputStream() throws IOException
    {
        //don't think there are too many options except for rendering it to a local byte buffer and returning from there
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        this.page.createExporter(this.rdfFormat).exportModel(this.page.readRdfModel(), output);

        return new ResourceInputStream(output.toByteArray());
    }
    /**
     * We override the mime type of the source resource because the parsed will probably change it.
     * @return the mime type of the resulting data stream, after parsing.
     */
    @Override
    public MimeType getMimeType()
    {
        // Note that we changed this from .getContentType() to .getMimeType() to better indicate to the client
        // what kind of content is returned. The previous philosophy was to stipulate the serialized data type in the
        // response, but actually data got lost because of generalization. We now expect the client knows how to
        // deal with RDF-specific mime types (because it asked for it)
        //return this.rdfFormat.getContentType();
        return this.rdfFormat.getMimeType();
    }
    @Override
    public boolean isReadOnly()
    {
        //since we're a parsed resource, we can't write back to it
        return true;
    }
    @Override
    public long getSize() throws IOException
    {
        //we assume we don't know the size of the parsed resource stream beforehand
        return -1;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
