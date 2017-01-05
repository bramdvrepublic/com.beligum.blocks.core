package com.beligum.blocks.rdf.ifaces;

import com.beligum.base.resources.RegisteredMimeType;
import com.beligum.base.resources.ifaces.MimeType;

/**
 * Created by bram on 2/20/16.
 */
public enum Format
{
    //-----CONSTANTS-----
    RDFA(RegisteredMimeType.HTML),
    JSONLD(RegisteredMimeType.JSONLD),
    NTRIPLES(RegisteredMimeType.NTRIPLES)
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
