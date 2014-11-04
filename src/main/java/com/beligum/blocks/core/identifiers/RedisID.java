package com.beligum.blocks.core.identifiers;

import com.beligum.blocks.core.config.BlocksConfig;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by bas on 13.10.14.
 * ID for identifiying database-objects (with Redis), represents a string of the form "[site-alias]/[page-name]#[object-id]:[version]"
 */
public class RedisID extends ID
{
    /**long representing the versioning stamp*/
    private long version;
    /**the url this id is based on*/
    private URL url;
    /**the preferred language of the object using this id*/
    private String language;

    /**
     * Constructor taking a URL. De version of this ID will be constructed using the current system's time.
     * @param url a url representing the unique id of an object that can be versioned
     * @throws URISyntaxException if the given url cannot be properly used as an ID, when the url is not properly formatted according to RFC 2396 standard
     */
    public RedisID(URL url) throws URISyntaxException
    {
        super(url.toURI());
        //change the site-domain in the id-uri to it's shorter alias
        this.idUri = new URI(BlocksConfig.SCHEME_NAME, BlocksConfig.getSiteDBAlias(), idUri.getPath(), idUri.getQuery(), idUri.getFragment());
        this.url = url;
        this.version = System.currentTimeMillis();
        //TODO BAS: parse the url and remove the internationlisation-information from the object's direct ID. use this class to 'getLanguage()'
        this.language = "en";
    }

    /**
     * Constructor taking a URL and a version.
     * @param url a url representing the unique id of an object that can be versioned
     * @throws URISyntaxException if the given url cannot be properly used as an ID
     */
    public RedisID(URL url, long version) throws URISyntaxException
    {
        super(url.toURI());
        //change the site-domain in the id-uri to it's shorter alias
        this.idUri = new URI(BlocksConfig.SCHEME_NAME, BlocksConfig.getSiteDBAlias(), idUri.getPath(), idUri.getQuery(), idUri.getFragment());
        this.url = url;
        this.version = version;
        //TODO BAS: parse the url and remove the internationlisation-information from the object's direct ID. use this class to 'getLanguage()'
        this.language = "en";
    }

    /**
     * Constructor taking an id retrieved form the Redis db and transforming it into an ID-object
     * @param dbId the id retrieved form db (must be of the form "blocks://[siteDomainAlias]/[objectName]:[version]"
     * @throws URISyntaxException when the id cannot properly be transformed into a URI, since this class is actually a wrapper around a URI
     * @throws MalformedURLException when the id cannot properly generate a URL, based on the (in the xml-configuration) specified site-domain
     */
    public RedisID(String dbId) throws URISyntaxException, MalformedURLException
    {
        super(null);
        /*
         * Split the string into "objectId" and "version"
         * Note: "objectId" could hold ":"-signs
         */
        String[] splitted = dbId.split(":");
        this.version = Long.parseLong(splitted[splitted.length-1]);
        int lastDoublePoint = dbId.lastIndexOf(':');
        String id = dbId.substring(0, lastDoublePoint);
        this.idUri = new URI(id);

        /*
         * Construct the url for this id, using the site-domain specified in the configuration-xml and the path specified by the database-id
         */
        URL siteDomain = new URL(BlocksConfig.getSiteDomain());
        String urlPath =  this.idUri.getPath();
        if(this.idUri.getFragment() != null){
            urlPath += "#" + this.idUri.getFragment();
        }
        if(this.idUri.getQuery() != null){
            urlPath += "?" + this.idUri.getQuery();
        }
        this.url = new URL(siteDomain.getProtocol(), siteDomain.getHost(), siteDomain.getPort(), urlPath);

        //TODO BAS: parse the url and remove the internationlisation-information from the object's direct ID. use this class to 'getLanguage()'
        this.language = "en";
    }

    public long getVersion()
    {
        return version;
    }
    public void setVersion(long version) {
        this.version = version;
    }

    public URL getUrl(){
        return url;
    }

    /**
     *
     * @return the authority of the uri-representation of this id (f.i. returns 'MOT' if this id would be 'blocks://MOT/pageID#elementId')
     */
    public String getAuthority(){
        return this.idUri.getAuthority();
    }

    /**
     *
     * @return the name of the version-list of this id in the Redis-db
     */
    public String getUnversionedId(){
        return idUri.toString();
    }


    /**
     *
     * @return a string-representation of this id, versioning included (of the form "[site-db-alias]/[object-path]:[version]")
     */
    public String getVersionedId(){
        return toString();
    }

    @Override
    /**
     *
     * @return a string-representation of this id, of the form "[site-db-alias]/[object-path]:[version>"
     */
    public String toString(){
        return getUnversionedId() + ":" + this.version;
    }
}
