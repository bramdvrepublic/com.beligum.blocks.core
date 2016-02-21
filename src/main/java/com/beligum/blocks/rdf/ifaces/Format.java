package com.beligum.blocks.rdf.ifaces;

import com.beligum.base.resources.ifaces.Resource;

/**
 * Created by bram on 2/20/16.
 */
public enum Format
{
    //-----CONSTANTS-----
    RDFA(Resource.MimeType.HTML),
    JSONLD(Resource.MimeType.JSONLD),
    NTRIPLES(Resource.MimeType.NTRIPLES)
    ;

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    private final Resource.MimeType mimeType;
    Format(Resource.MimeType mimeType)
    {
        this.mimeType = mimeType;
    }

    //-----PUBLIC METHODS-----
    public Resource.MimeType getMimeType()
    {
        return mimeType;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
