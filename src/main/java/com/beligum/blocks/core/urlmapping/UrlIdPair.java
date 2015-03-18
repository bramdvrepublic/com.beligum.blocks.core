package com.beligum.blocks.core.urlmapping;

import com.beligum.blocks.core.identifiers.BlocksID;

import java.net.URL;

/**
 * Created by bas on 26.02.15.
 */
public class UrlIdPair
{
    private BlocksID id;
    private URL url;
    public UrlIdPair(URL url, BlocksID id)
    {
        this.id = id;
        this.url = url;
    }
    public BlocksID getId()
    {
        return id;
    }
    public URL getUrl()
    {
        return url;
    }
}
