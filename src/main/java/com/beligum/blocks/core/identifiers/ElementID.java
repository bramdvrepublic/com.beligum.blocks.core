package com.beligum.blocks.core.identifiers;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by bas on 16.10.14.
 */
public class ElementID extends RedisID
{

    /**
     * Constructor taking a URL. De version of this ID will be constructed using the current system's time.
     * @param url
     * @throws URISyntaxException - if the given url cannot be properly used as an ID, when the url is not proparly formatted according to RFC 2396 standard
     */
    public ElementID(URL url) throws URISyntaxException
    {
        super(url);
    }

    /**
     * Constructor taking a URL and a version.
     * @param url a url representing the unique id of an element
     * @param version the version of the object represented by this id
     * @throws URISyntaxException
     */
    public ElementID(URL url, long version) throws URISyntaxException
    {
        super(url, version);
    }

    /**
     * Constructor taking an id retrieved form the Redis db and transforming it into an ID-object
     * @param dbId the id retrieved form db (must be of the form "[objectName]:[version]"
     * @throws URISyntaxException when the id cannot properly be transformed into a URI, since this class is actually a wrapper around a URI
     * @throws MalformedURLException when the id cannot properly generate a URL, based on the (in the xml-configuration) specified site-domain
     */
    public ElementID(String dbId) throws MalformedURLException, URISyntaxException
    {
        super(dbId);
    }

    /**
     *
     * @return the fragment of the url to this element, this is everything after the '#' in "[site-domain]/[pageId]#[elementId]" and is the id found in the html-file containing this element
     * @throws URISyntaxException
     */
    public String getElementIdFromFragment()
    {
        try{
            return this.getUrl().toURI().getFragment();
        }
        catch(URISyntaxException e){
            throw new RuntimeException("Bad uri found. This should not happen!", e);
        }
    }
}
