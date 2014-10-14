package com.beligum.blocks.core.identifiers;

import com.beligum.blocks.core.config.BlocksConfig;

import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by bas on 13.10.14.
 * ID for identifiying database-objects (with Redis), represents a string of the form "<site-alias>/<page-name>#<object-id>:<version>"
 * Ideally suited for managing
 */
public class RedisID extends ID
{
    //long representing the versioning stamp
    private long version;
    //the url this id is based on
    private URL url;

    /**
     * Constructor taking a URL. De version of this ID will be constructed using the current system's time.
     * @param url a url representing the unique id of an object that can be versioned
     * @throws URISyntaxException if the given url cannot be properly used as an ID, since the url is not proparly formatted according to RFC 2396 standard
     */
    public RedisID(URL url) throws URISyntaxException
    {
        super(url.toURI());
        this.url = url;
        this.version = System.currentTimeMillis();
    }

    /**
     * Constructor taking a URL and a version.
     * @param url a url representing the unique id of an object that can be versioned
     * @throws URISyntaxException if the given url cannot be properly used as an ID
     */
    public RedisID(URL url, long version) throws URISyntaxException
    {
        super(url.toURI());
        this.url = url;
        this.version = version;
    }

    public long getVersion()
    {
        return version;
    }

    public URL getURL(){
        return url;
    }

    /**
     *
     * @return the name of the version-list of this id in the Redis-db
     */
    public String getUnversionedId(){
        if(id.getFragment() != null) {
            return BlocksConfig.getSiteDBAlias() + id.getPath() + "#" + id.getFragment();
        }
        else{
            return BlocksConfig.getSiteDBAlias() + id.getPath();
        }
    }


    /**
     *
     * @return a string-representation of this id, versioning included (of the form "<site-db-alias>/<object-path>:<version>")
     */
    public String getVersionedId(){
        return toString();
    }

    @Override
    /**
     *
     * @return a string-representation of this id, of the form "<site-db-alias>/<object-path>:<version>"
     */
    public String toString(){
        return getUnversionedId() + ":" + this.version;
    }
}
