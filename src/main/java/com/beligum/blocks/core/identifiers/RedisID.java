package com.beligum.blocks.core.identifiers;

import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.CacheConstants;
import com.beligum.blocks.core.config.DatabaseConstants;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.exceptions.RedisException;
import com.beligum.blocks.core.internationalization.Languages;
import com.beligum.blocks.core.models.templates.EntityTemplateClass;
import org.apache.commons.lang3.StringUtils;
import sun.org.mozilla.javascript.ast.Block;

import javax.print.URIException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;

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






    /**constant used to indicate a redis-id has no version attached*/
    public static final long NO_VERSION = -1;
    /**constant that can be given to RedisID-constructors, indicating the RedisID should point to the last version of an storable object saved in redis-db*/
    public static final long LAST_VERSION = -2;
    /**constant used to indicate a new verion should be made for a RedisID, using the current system's time*/
    public static final long NEW_VERSION = -3;





    /**
     * Constructor taking a URL and a version.
     * @param url a url representing the unique id of an object that can be versioned
     * @throws URISyntaxException if the given url cannot be properly used as an ID
     */
    public RedisID(URL url, long version) throws IDException
    {
        super(url);
        this.idUri = initializeLanguageAndUrl(url, true);
        if(version == LAST_VERSION){
            this.version = Redis.getInstance().getLastVersion(url);
        }
        else if(version == NEW_VERSION){
            this.version = System.currentTimeMillis();
        }
        else {
            this.version = version;
        }
    }

    /**
     * Constructor taking an id retrieved form the Redis db and transforming it into an ID-object
     * @param unversionedDbId the id retrieved form db (must be of the form "blocks://[siteDomainAlias]/[objectName]") without a version attached
     * @param version the version this RedisID should have, use RedisID.LAST_VERSION to set the last version present in redis-db
     * @throws IDException when the id cannot properly be generated from the specified string and version
     */
    public RedisID(String unversionedDbId, long version) throws IDException
    {
        this(unversionedDbId + ":" + version);
    }

    /**
     * Constructor taking an id retrieved form the Redis db and transforming it into an ID-object.
     * If no version is present in the specified id, the last version is returned.
     * @param versionedDbId the id retrieved form db (must be of the form "blocks://[siteDomainAlias]/[objectName]:[version]"
     * @throws URISyntaxException when the id cannot properly be transformed into a URI, since this class is actually a wrapper around a URI
     * @throws IDException when no versioned id is specified or when the id cannot properly be generated from the specified string
     */
    public RedisID(String versionedDbId) throws IDException
    {
        super((URI) null);
        try {
            /*
             * Check if the specified string has a good redis-id form
             */
            URI versionedDbUri = new URI(versionedDbId);
            if(versionedDbUri.getScheme() == null || !versionedDbUri.getScheme().equals(DatabaseConstants.SCHEME_NAME)){
                throw new IDException("Uncorrect db-id (uncorrect scheme) '" + versionedDbId + "'.");
            }
            if(!versionedDbUri.getSchemeSpecificPart().contains(BlocksConfig.getSiteDBAlias())){
                throw new IDException("Uncorrect db-id (uncorrect site-alias) '" + versionedDbId + "'.");
            }
            if(!versionedDbUri.getSchemeSpecificPart().contains(":")){
                throw new IDException("Found unversioned id '" + versionedDbId + "', but I need a versioned one.");
            }

            /*
             * Split the string into "objectId" and "version"
             * Note: "objectId" could hold ":"-signs
             */
            String[] splitted = versionedDbId.split(":");
            long version = NO_VERSION;
            String language = Languages.NO_LANGUAGE;
            String unversionedId = "";
            //if only two parts have been splitted of, this is not a versioned id and we look for the last version
            if(splitted.length == 2){
                version = LAST_VERSION;
                unversionedId = versionedDbId;
            }
            else {
                String[] splitted2 = splitted[splitted.length - 1].split("/");
                //if two parts have been splitted off of the information after the last ":", we're dealing with a languaged id
                if(splitted2.length == 2){
                    version = Long.parseLong(splitted2[0]);
                    language = splitted2[1];
                }
                else{
                    version = Long.parseLong(splitted2[0]);
                }
                int lastDoublePoint = versionedDbId.lastIndexOf(':');
                unversionedId = versionedDbId.substring(0, lastDoublePoint);
            }
            /*
             * Initialize version
             */
            if (version == LAST_VERSION) {
                this.version = Redis.getInstance().getLastVersion(unversionedId);
            }
            else if(version == NEW_VERSION) {
                this.version = System.currentTimeMillis();
            }
            else {
                this.version = version;
            }
            /*
             * Construct a url from the db-id and use it to initialize language, url and id-uri fields
             */
            URI urlUri = new URI(unversionedId);
            String urlPath = urlUri.getPath();
            //remove the beginning-"/" from the path
            String relativePath = urlPath.substring(1);
            URL url = new URL(new URL(BlocksConfig.getSiteDomain()), relativePath);
            this.idUri = initializeLanguageAndUrl(url, false);
        }catch(Exception e){
            throw new IDException("Bad id found. Only id's coming from db should be used.", e);
        }
    }

    /**
     * Constructor copying the baseId, but taking another language
     * @param baseId
     * @param language
     * @throws IDException
     */
    public RedisID(RedisID baseId, String language) throws IDException
    {
        this(baseId.getVersionedId());
        this.language = language;
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
        return getUnversionedId() + ":" + this.version;
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
        return getVersionedId() + "/" + this.language;
    }

    /**
     * Initialize the language-field of this object, parsed from the url. If no language is present, this method can use the site's preferred language.
     * Also change the site-domain of the url to the site-alias specified in the configuration-file.
     * @param url the url to be parsed
     * @param useSitesPreferredLanguage set to true if you want this method to use the site's preferred languages if no language can be parsed from the url
     * @return a uri corresponding to the specified url, with the language-information removed, or null if something went wrong
     */
    private URI initializeLanguageAndUrl(URL url, boolean useSitesPreferredLanguage) throws IDException
    {
        try{
            this.language = null;
            Set<String> permittedLanguages = Languages.getPermittedLanguageCodes();
            String urlPath = url.getPath();
            String[] splitted = urlPath.split("/");
            //the uri-path always starts with "/", so the first index in the splitted-array always will be empty ""
            if(splitted.length > 1) {
                String foundLanguage = splitted[1];
                if (permittedLanguages.contains(foundLanguage)) {
                    this.language = foundLanguage;
                    //remove the language-information from the middle of the id
                    urlPath = "";
                    for (int j = 2; j < splitted.length; j++) {
                        urlPath += splitted[j];
                    }
                }
            }
            //if no language could be successfully parsed from the url, we use the site's preferred languages
            if(this.language == null) {
                if (useSitesPreferredLanguage) {
                    String[] preferredLanguages = BlocksConfig.getLanguages();
                    this.language = preferredLanguages[0];
                }
                else {
                    this.language = Languages.NO_LANGUAGE;
                }
            }
            /*
             * Construct the url for this id, using the site-domain specified in the configuration-xml and the previously parsed path (no more language-info is present in it)
             */
            URL siteDomain = new URL(BlocksConfig.getSiteDomain());
            this.url = new URL(siteDomain.getProtocol(), siteDomain.getHost(), siteDomain.getPort(), urlPath);
            return new URI(DatabaseConstants.SCHEME_NAME, BlocksConfig.getSiteDBAlias() + urlPath, null);
        }catch(Exception e){
            throw new IDException("Could not initialize language and url while constructing a '" + RedisID.class.getSimpleName() +"'.", e);
        }
    }


    //___________________________STATIC METHODS FOR ID-RENDERING_______________________________

    /**
     * Method for getting a new randomly determined entity-uid (with versioning) for a entityTemplate-instance of an entityTemplateClass
     * @return a randomly generated entity-id of the form "[site-domain]/[entityTemplateClassName]/[randomInt]"
     */
    public static RedisID renderNewEntityTemplateID(EntityTemplateClass entityTemplateClass) throws IDException
    {
        return Redis.getInstance().renderNewEntityTemplateID(entityTemplateClass);
    }

    /**
     * Method for getting a new template-class uid for a certain class.
     * @param entityTemplateClassName the name for this entity-class
     * @param language the language this entity-class is written in
     * @return A versioned id of the form "blocks://[db-alias]/[entityTemplateClassName]"
     */
    public static RedisID renderNewEntityTemplateClassID(String entityTemplateClassName, String language) throws IDException
    {
        //we're not actually going to the db to determine a new redis-id for a class, it will use a new versioning (current time millis) to get a new version, so we don't actually need to check for that version in db
        try{
            RedisID newID = new RedisID(new URL(BlocksConfig.getSiteDomain() +  "/" + entityTemplateClassName), NEW_VERSION);
            while(Redis.getInstance().fetchEntityTemplateClass(newID) != null){
                newID = new RedisID(new URL(BlocksConfig.getSiteDomain() + "/" + entityTemplateClassName), NEW_VERSION);
            }
            newID.language = language;
            return newID;
        }catch(MalformedURLException |RedisException e){
            throw new IDException("Could not construct id from site-domain '" + BlocksConfig.getSiteDomain() + "', name '" + entityTemplateClassName + "' and language '" + language + "'.", e);
        }
    }

    /**
     *
     * @param entityTemplateClassName
     * @return an unversioned id for an entity-class
     * @throws IDException
     */
    public static String renderUnversionedEntityTemplateClassID(String entityTemplateClassName) throws IDException
    {
        try{
            return new RedisID(new URL(BlocksConfig.getSiteDomain() +  "/" + entityTemplateClassName), NO_VERSION).getUnversionedId();
        }catch(MalformedURLException e){
            throw new IDException("Could not construct id from site-domain '" + BlocksConfig.getSiteDomain() + "' and name '" + entityTemplateClassName + "'.", e);
        }
    }

    /**
     * Method for getting a new template-class uid for a certain class.
     * @param pageTemplateName
     * @return A versioned id of the form "blocks://[db-alias]/pageTemplates/[pageTemplateName]"
     */
    public static RedisID renderNewPageTemplateID(String pageTemplateName, String language) throws IDException
    {
        try{
            RedisID newId = new RedisID(new URL(BlocksConfig.getSiteDomain() + "/" + CacheConstants.PAGE_TEMPLATE_ID_PREFIX + "/" + pageTemplateName), NEW_VERSION);
            while(Redis.getInstance().fetchPageTemplate(newId) != null){
                newId = new RedisID(new URL(BlocksConfig.getSiteDomain() + "/" + CacheConstants.PAGE_TEMPLATE_ID_PREFIX + "/" + pageTemplateName), NEW_VERSION);
            }
            newId.language = language;
            return newId;
        }catch(MalformedURLException | RedisException e){
            throw new IDException("Could not construct id from site-domain '" + BlocksConfig.getSiteDomain() + "' and name '" + pageTemplateName + "'.", e);
        }
    }

    /**
     *
     * @param pageTemplateName
     * @return an unversioned id for an page-template
     * @throws IDException
     */
    public static String renderUnversionedPageTemplateID(String pageTemplateName) throws IDException
    {
        try{
            return new RedisID(new URL(BlocksConfig.getSiteDomain() + "/" + CacheConstants.PAGE_TEMPLATE_ID_PREFIX + "/" + pageTemplateName), NO_VERSION).getUnversionedId();
        }catch(MalformedURLException e){
            throw new IDException("Could not construct id from site-domain '" + BlocksConfig.getSiteDomain() + "' and name '" + pageTemplateName + "'.", e);
        }
    }

    /**
     * Method for getting a new property-id for a certain property (blocks://[db-alias]/[parent-typeof]#[property-name]:[version]
     * @param owningEntityClassName
     * @param property the value of the property-attribute (for <div property="something" name="some_thing"></div> this is "something")
     * @param propertyName the name of the property, used to guarantee it's uniqueness on class-level (for <div property="something" name="some_thing"></div> this is "some_thing")
     * @return a new property-id to be used as a reference in a entity-class
     * @throws IDException
     */
    public static RedisID renderNewPropertyId(String owningEntityClassName, String property, String propertyName) throws IDException
    {
        try{
            if(owningEntityClassName == null){
                owningEntityClassName = "";
            }
            if(property == null){
                property = "";
            }
            String url = BlocksConfig.getSiteDomain() + "/" + owningEntityClassName + "#" + property;
            if(!StringUtils.isEmpty(propertyName)){
                url += "/" + propertyName;
            }
            RedisID newID = new RedisID(new URL(url), NEW_VERSION);
            while(Redis.getInstance().fetchEntityTemplate(newID) != null){
                newID = new RedisID(new URL(url), NEW_VERSION);
            }
            EntityTemplateClass entityTemplateClass = EntityTemplateClassCache.getInstance().get(owningEntityClassName);
            newID.language = entityTemplateClass.getLanguage();
            return newID;
        }catch(Exception  e){
            throw new IDException("Couldn't construct proper id with '" + BlocksConfig.getSiteDomain() + "/" + owningEntityClassName + "#" + property + "/" + propertyName + "'", e);
        }
    }

    /**
     * Returns the name of the set where all instance-ids of the same entity-template-class are stored, f.i. "blocks://[db-alias]/waterwells" for class typeof="waterwell".
     * @param entityTemplateClassName
     * @return An id of a set of entity-template-class-instances, which uses the plural form of the name of the entity-template-class.
     */
    public static String getEntityTemplateClassSetId(String entityTemplateClassName) throws IDException
    {
        try {
            String entityTemplateClassSetName = entityTemplateClassName + DatabaseConstants.ENTITY_TEMPLATE_CLASS_SET_SUFFIX;
            URI entityTemplateClassSetId = new URI(DatabaseConstants.SCHEME_NAME, BlocksConfig.getSiteDBAlias(), "/" + entityTemplateClassSetName, null);
            return entityTemplateClassSetId.toString();
        }catch(URISyntaxException e){
            throw new IDException("Cannot use entity-template class '" + entityTemplateClassName + "' for id-rendering.", e);
        }
    }

    /**
     *
     * @param id
     * @return whether or not the specified string is a reference to an entity or not
     */
    public static boolean isRedisId(String id){
        try{
            new RedisID(id);
            return true;
        }catch(Exception e){
            return false;
        }
    }
}
