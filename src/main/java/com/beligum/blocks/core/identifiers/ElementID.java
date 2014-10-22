package com.beligum.blocks.core.identifiers;

import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by bas on 16.10.14.
 */
public class ElementID extends RedisID
{
    //the version of the parent of this element
    private long parentVersion;

    /**
     * Constructor taking a URL. De version of this ID will be constructed using the current system's time.
     * @param url
     * @param parentVersion the version of the parent of the element with this id
     * @throws URISyntaxException - if the given url cannot be properly used as an ID, when the url is not proparly formatted according to RFC 2396 standard
     */
    public ElementID(URL url, long parentVersion) throws URISyntaxException
    {
        super(url);
        this.parentVersion = parentVersion;
    }

    /**
     * Constructor taking a URL and a version.
     * @param url a url representing the unique id of an element
     * @param version the version of the object represented by this id
     * @param parentVersion the version of the parent of the element with this id
     * @throws URISyntaxException
     */
    public ElementID(URL url, long version, long parentVersion) throws URISyntaxException
    {
        super(url, version);
        this.parentVersion = parentVersion;
    }

    public long getParentVersion(){
        return parentVersion;
    }

    @Override
    public String getVersionedId()
    {
        return getUnversionedId() + ":" + parentVersion + ":" + getVersion();
    }
    @Override
    public String toString()
    {
        return getVersionedId();
    }
}
