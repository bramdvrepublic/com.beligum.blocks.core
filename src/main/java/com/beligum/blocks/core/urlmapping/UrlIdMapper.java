package com.beligum.blocks.core.urlmapping;

import com.beligum.blocks.core.exceptions.UrlIdMappingException;
import com.beligum.blocks.core.identifiers.BlocksID;

import java.net.URL;

/**
 * Created by bas on 23.02.15.
 */
public interface UrlIdMapper
{
    /**
     *
     * @param url
     * @return The id this url is paired to (including language) or if no pairing exists, returns an id (with a new version) holding the specified url as internal url.
     * @throws UrlIdMappingException
     */
    public BlocksID getId(URL url) throws UrlIdMappingException;

    /**
     *
     * @param id
     * @return The url this id is paired to (including language) or if no pairing exists, returns the id's internal url.
     * @throws UrlIdMappingException
     */
    public URL getUrl(BlocksID id) throws UrlIdMappingException;

    /**
     * Add a url-id pair to the mapping. If a previous pairing was present, it will be replaced.
     * @param id id holding language information
     * @param url url to be put as the key for the id using the language specified in the id
     * @return The {@link URL} and {@link BlocksID} that were replaced, wrapped in a {@link UrlIdPair}. Both the {@link URL} or {@link BlocksID} will be null if none were replaced
     */
    public UrlIdPair put(BlocksID id, URL url) throws UrlIdMappingException;

    /**
     * Remove the translation corresponding to the specified id and it's language from the mapping.
     * @param languagedId an id holding language information
     * @return the url the id was attached to or null if it was not present to begin with (in that language)
     */
    public URL remove(BlocksID languagedId) throws UrlIdMappingException;

    /**
     * Remove the url from the mapping.
     * @param languagedUrl an url holding language information
     * @return the id the url was attached to or null if it was not present to begin with
     * @throws UrlIdMappingException if no language information is present in the specified url
     */
    public BlocksID remove(URL languagedUrl) throws UrlIdMappingException;

    /**
     *
     * @param url
     * @return The last id that was previously mapped to this url, or null if this url was never paired to an id before.
     * @throws UrlIdMappingException
     */
    public BlocksID getLastId(URL url) throws UrlIdMappingException;

    /**
     * Remove the mapping from cache.
     */
    public void reset();

    public SiteMap renderSiteMap(String language) throws UrlIdMappingException;
}
