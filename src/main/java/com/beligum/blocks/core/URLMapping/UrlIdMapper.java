package com.beligum.blocks.core.URLMapping;

import com.beligum.blocks.core.exceptions.UrlIdMappingException;
import com.beligum.blocks.core.identifiers.BlocksID;

import java.net.URL;

/**
 * Created by bas on 23.02.15.
 */
public interface UrlIdMapper
{
    public BlocksID getId(URL url);

    public URL getUrl(BlocksID id) throws UrlIdMappingException;

    /**
     * Add a url-id pair to the mapping.
     * If no language information can be found in the specified url, the language of the id is used to add a translation to the mapping.
     * @param url
     * @param id
     */
    public void add(URL url, BlocksID id) throws UrlIdMappingException;

    public void remove(BlocksID id);

    /**
     * Remove the mapping from cache.
     */
    public void reset();
}
