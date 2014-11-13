package com.beligum.blocks.core.identifiers;

import com.beligum.blocks.core.config.DatabaseConstants;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by bas on 16.10.14.
 */
public class EntityID extends RedisID
{
    /**
     * Constructor taking a URL. De version of this ID will be constructed using the current system's time.
     * @param url a url representing the unique id of the page represented by this id
     * @throws URISyntaxException if the given url cannot be properly used as an ID, when the url is not properly formatted according to RFC 2396 standard
     */
    public EntityID(URL url) throws URISyntaxException
    {
        super(url);
    }

    /**
     * Constructor taking a URL and a version.
     * @param url a url representing the unique id of the page represented by this id
     * @param version
     * @throws URISyntaxException if the given url cannot be properly used as an ID
     */
    public EntityID(URL url, long version) throws URISyntaxException
    {
        super(url, version);
    }

    /**
     * Constructor taking an id retrieved form the Redis db and transforming it into an ID-object
     * @param dbId the id retrieved form db
     * @throws URISyntaxException when the id cannot properly be transformed into a URI, since this class is actually a wrapper around a URI
     * @throws MalformedURLException when the id cannot properly generate a URL, based on the (in the xml-configuration) specified site-domain
     */
    public EntityID(String dbId) throws URISyntaxException, MalformedURLException
    {
        super(dbId);
    }

    /**
     *
     * @return the string-id of the hash containing all page meta-data (info) of the page with this EntityID
     */
    public String getPageInfoId(){
        return getVersionedId() + ":" + DatabaseConstants.ENTITY_INFO_SUFFIX;
    }
}
