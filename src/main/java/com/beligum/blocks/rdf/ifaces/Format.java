package com.beligum.blocks.rdf.ifaces;

import com.beligum.base.resources.MimeTypes;
import com.beligum.base.resources.ifaces.MimeType;

/**
 * Created by bram on 2/20/16.
 */
public enum Format
{
    //-----CONSTANTS-----
    RDFA(MimeTypes.HTML),
    JSONLD(MimeTypes.JSONLD),
    RDF_XML(MimeTypes.RDF_XML),
    NTRIPLES(MimeTypes.NTRIPLES)
    ;

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    private final MimeType mimeType;
    Format(MimeType mimeType)
    {
        this.mimeType = mimeType;
    }

    //-----PUBLIC METHODS-----
    public MimeType getMimeType()
    {
        return mimeType;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
