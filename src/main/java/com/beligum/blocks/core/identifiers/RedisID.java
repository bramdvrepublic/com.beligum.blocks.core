package com.beligum.blocks.core.identifiers;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.CacheConstants;
import com.beligum.blocks.core.config.DatabaseConstants;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.models.templates.EntityTemplateClass;
import org.apache.commons.configuration.ConfigurationRuntimeException;
import redis.clients.jedis.Jedis;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Random;

/**
 * Created by bas on 13.10.14.
 * ID for identifiying database-objects (with Redis), represents a string of the form "[site-alias]/[page-name]#[object-id]:[version]"
 */
public class RedisID extends ID
{
    /**long representing the versioning stamp*/
    private long version = -1;
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
        this.idUri = initializeLanguage(url);
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
        this.idUri = initializeLanguage(url);
        this.url = url;
        this.version = version;
    }

    /**
     * Constructor taking an id retrieved form the Redis db and transforming it into an ID-object
     * @param unversionedDbId the id retrieved form db (must be of the form "blocks://[siteDomainAlias]/[objectName]") without a version attached
     * @throws URISyntaxException when the id cannot properly be transformed into a URI, since this class is actually a wrapper around a URI
     * @throws MalformedURLException when the id cannot properly generate a URL, based on the (in the xml-configuration) specified site-domain
     */
    public RedisID(String unversionedDbId, long version) throws MalformedURLException, URISyntaxException
    {
        this(unversionedDbId + ":" + version);
    }

    /**
     * Constructor taking an id retrieved form the Redis db and transforming it into an ID-object
     * @param versionedDbId the id retrieved form db (must be of the form "blocks://[siteDomainAlias]/[objectName]:[version]"
     * @throws URISyntaxException when the id cannot properly be transformed into a URI, since this class is actually a wrapper around a URI
     * @throws MalformedURLException when the id cannot properly generate a URL, based on the (in the xml-configuration) specified site-domain
     */
    public RedisID(String versionedDbId) throws URISyntaxException, MalformedURLException
    {
        super(null);
        /*
         * Split the string into "objectId" and "version"
         * Note: "objectId" could hold ":"-signs
         */
        String[] splitted = versionedDbId.split(":");
        if(splitted[splitted.length-1].contentEquals(DatabaseConstants.HASH_SUFFIX)){
            //TODO BAS: generalize this when using internationalization
            this.version = Long.parseLong(splitted[splitted.length - 2]);
            int lastDoublePoint = versionedDbId.lastIndexOf(':');
            String versionedId = versionedDbId.substring(0, lastDoublePoint);
            int oneButLastDoublePoint = versionedId.lastIndexOf(':');
            String id = versionedId.substring(0, oneButLastDoublePoint);
            this.idUri = new URI(id);
        }
        else {
            this.version = Long.parseLong(splitted[splitted.length - 1]);
            int lastDoublePoint = versionedDbId.lastIndexOf(':');
            String id = versionedDbId.substring(0, lastDoublePoint);
            this.idUri = new URI(id);
        }

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

    public String getLanguage()
    {
        return language;
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

    /**
     *
     * @return the string-id of the hash containing all page meta-data (info) of the page with this EntityID
     */
    public String getHashId(){
        return getVersionedId() + ":" + DatabaseConstants.HASH_SUFFIX;
    }


    @Override
    /**
     *
     * @return a string-representation of this id, of the form "[site-db-alias]/[object-path]:[version>"
     */
    public String toString(){
        return getUnversionedId() + ":" + this.version;
    }

    /**
     * Initialize the language-field of this object, parsed from the url. If no language is present, use the site's preferred language.
     * Also change the site-domain of the url to the site-alias specified in the configuration-file.
     * @param url the url to be parsed
     * @return a uri corresponding to the specified url, with the language-information removed
     */
    private URI initializeLanguage(URL url) throws URISyntaxException
    {
        String[] languages = BlocksConfig.getLanguages();
        String uriPath = idUri.getPath();
        String[] splitted = uriPath.split("/");
        //the uri-path always starts with "/", so the first index in the splitted-array always will be empty ""
        if(splitted.length > 1) {
            String foundLanguage = splitted[1];
            boolean urlHasKnownLanguage = false;
            int i = 0;
            while (i < languages.length) {
                if (languages[i].contentEquals(foundLanguage)) {
                    urlHasKnownLanguage = true;
                }
                i++;
            }
            if (urlHasKnownLanguage) {
                this.language = foundLanguage;
                //remove the language-information from the middle of the id
                uriPath = "";
                for (int j = 2; j < splitted.length; j++) {
                    uriPath += splitted[j];
                }
            }
            else {
                this.language = languages[0];
            }
        }
        else{
            this.language = languages[0];
        }
        //change the site-domain in the id-uri to it's shorter aliasString[] languages = BlocksConfig.getLanguages();
        return new URI(BlocksConfig.SCHEME_NAME, BlocksConfig.getSiteDBAlias(), uriPath, idUri.getQuery(), idUri.getFragment());
    }

    //__________________STATIC METHODS FOR ID-RENDERING______________________
    /**
     * Method for getting a new randomly determined entity-uid (with versioning) for a entityTemplate-instance of an entityTemplateClass
     * @return a randomly generated entity-id of the form "[site-domain]/[entityTemplateClassName]/[randomInt]"
     */
    public static RedisID renderNewEntityTemplateID(EntityTemplateClass entityTemplateClass){
        return Redis.getInstance().renderNewEntityTemplateID(entityTemplateClass);
    }

    /**
     * Method for getting a new template-class uid for a certain class.
     * @param entityTemplateClassName
     * @return A versioned id of the form "blocks://[db-alias]/[entityTemplateClassName]"
     */
    public static RedisID renderNewEntityTemplateClassID(String entityTemplateClassName){
        //we're not actually going to the db to determine a new redis-id for a class, it will use a new versioning (current time millis) to get a new version, so we don't actually need to check for that version in db
        try{
            return new RedisID(new URL(BlocksConfig.getSiteDomain() + "/" + entityTemplateClassName));
        }catch(MalformedURLException e){
            throw new ConfigurationRuntimeException("Specified site-domain doesn't seem to be a correct url: " + BlocksConfig.getSiteDomain(), e);
        }catch(URISyntaxException e){
            throw new ConfigurationRuntimeException("Cannot use this site-domain for id-rendering: " + BlocksConfig.getSiteDomain(), e);
        }
    }

    /**
     * Method for getting a new template-class uid for a certain class.
     * @param pageTemplateName
     * @return A versioned id of the form "blocks://[db-alias]/pageTemplates/[pageTemplateName]"
     */
    public static RedisID renderNewPageTemplateID(String pageTemplateName){
        try{
            return new RedisID(new URL(BlocksConfig.getSiteDomain() + "/" + CacheConstants.PAGE_TEMPLATE_ID_PREFIX + "/" + pageTemplateName));
        }catch(MalformedURLException e){
            throw new ConfigurationRuntimeException("Specified site-domain doesn't seem to be a correct url: " + BlocksConfig.getSiteDomain(), e);
        }catch(URISyntaxException e){
            throw new ConfigurationRuntimeException("Cannot use this site-domain for id-rendering: " + BlocksConfig.getSiteDomain(), e);
        }
    }
}
