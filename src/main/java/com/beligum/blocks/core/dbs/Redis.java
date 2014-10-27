package com.beligum.blocks.core.dbs;

import com.beligum.blocks.core.caching.PageClassCache;
import com.beligum.blocks.core.exceptions.PageClassCacheException;
import com.beligum.blocks.core.exceptions.RedisException;
import com.beligum.blocks.core.identifiers.ElementID;
import com.beligum.blocks.core.identifiers.PageID;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.AbstractElement;
import com.beligum.blocks.core.models.PageClass;
import com.beligum.blocks.core.models.ifaces.Storable;
import com.beligum.blocks.core.models.ifaces.StorableElement;
import com.beligum.blocks.core.models.storables.Block;
import com.beligum.blocks.core.models.storables.Page;
import com.beligum.blocks.core.models.storables.Row;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;

import java.io.Closeable;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

/**
 * Created by bas on 09.10.14.
 * Wrapper class for talking to the redis-db
 * At the end of the application it has to be closed
 */
public class Redis implements Closeable
{

    //TODO check/boost redis-performance with http://redis.io/topics/benchmarks

    //TODO BAS: implement transactions and/or pipelining (https://github.com/xetorthio/jedis/wiki/AdvancedUsage)


    //a thread-save pool for connection to the redis-master-server
    private final JedisSentinelPool pool;

    //the instance of this singleton
    private static Redis instance = null;

    /**
     * private constructor for singleton-use
     */
    private Redis(){
        //TODO: redis-config should come from configuration-file dev.xml (senitels host and port, masterName)
        //create a thread-save pool of Jedis-instances, using default configuration
        Set<String> sentinels = new HashSet<>();
        sentinels.add("localhost:26379");
        sentinels.add("localhost:26380");
        pool = new JedisSentinelPool("mymaster", sentinels);
    }

    /**
     * Static method for getting a singleton redis-wrapper
     * @return a singleton instance of Redis
     */
    public static Redis getInstance()
    {
        if(instance == null){
            instance = new Redis();
        }
        return instance;
    }


    /**
     * Save the page to db, together with it's rows and blocks
     * @param page
     */
    //TODO BAS: possibly you could write unit test for this basic method: making a new default-page should always result in the store and returnal of the exact html in the index.html of a default page
    public void save(Page page) throws RedisException
    {
        try (Jedis redisClient = pool.getResource()){
            /*
             * Save all non-default, non-final block- and row-ids (with id "[elementId]:[version]" or "[elementId]") in a new set named "[pageId]:[version]"
             * Save all new versions of blocks and rows "[elementId]:[version]" in a list named "[elementId]"
             */
            //the page stored in db, will be null when no such page is present in db
            Page storedPage = this.fetchPage(page.getUrl());
            //elements of the page-class present in the cache that may not be altered
            HashSet<StorableElement> finalElements = page.getFinalElements();
            //elements of the last version of this page stored in db that can be altered
            HashSet<StorableElement> storedElements = (storedPage != null) ? storedPage.getNonFinalElements() : new HashSet<StorableElement>();
            //elements of the page-class present in the cache that can be altered
            HashSet<StorableElement> cachedElements = page.getCachedElements();
            //elements of the page we want to save, which need to be compared to the above maps of elements
            Set<StorableElement> pageElements = page.getElements();
            //for all elements received from the page to be saved, check if they have to be saved to db, or if they are already present in the cache
            for(StorableElement pageElement : pageElements) {
                if(!finalElements.contains(pageElement)) {
                    if(!storedElements.contains(pageElement)){
                        if(!cachedElements.contains(pageElement)) {
                            this.save(pageElement);
                            //TODO BAS: sometimes (like with calendar-blocks) here we will need to save the pageElementId's unversioned form
                            redisClient.sadd(page.getVersionedId(), pageElement.getVersionedId());
                        }
                        else{
                            //nothing has to be done with unaltered, cached elements when saving to db (they are already present in the application cache)
                        }
                    }
                    else {
                        //nothing has to be done with unaltered, already stored elements when saving to db (they are already present in db)
                    }
                }
                else {
                    //nothing has to be done with final elements when saving to db (they are already present in the application cache and may not be altered anyhow)
                }
            }

            /*
             * Save this page-version's id ("[pageId]:[version]") to the db in the list named "<pageId>"
             * holding all the different versions of this page-instance.
             * If their already exist an element-set for this page-version, throw exception
             */
            if (redisClient.exists(page.getVersionedId())) {
                throw new RedisException("The page '" + page.getUnversionedId() + "' already has a version '" + page.getVersion() + "' present in db.");
            }
            //if another version is present in db, check if this page is more recent
            Long lastVersion = getLastVersion(page, redisClient);
            if (lastVersion == -1 ||  page.getVersion() > lastVersion) {
                redisClient.lpush(page.getUnversionedId(), page.getVersionedId());
            }
            else {
                throw new RedisException(
                                "The page '" + page.getUnversionedId() + "' with version '" + page.getVersion() + "' already has a more recent version '" + lastVersion +
                                "' present in db.");
            }

            /*
             * Save other info about the page (like it's page-class) in a hash named "[pageId]:[version]:info"
             */
            redisClient.hmset(page.getInfoId(), page.getInfo());
        }
    }


    /**
     * Save the element to db: save content and info of an element in a hash named "[elementId]:[version]"
     * @param element
     */
    public void save(StorableElement element) throws RedisException
    {
        try (Jedis redisClient = pool.getResource()){
            //if their already exists a hash corresponding to the version of this element, throw exception
            if (redisClient.exists(element.getVersionedId())) {
                throw new RedisException("The element '" + element.getUnversionedId() + "' already has a version '" + element.getVersion() + "' present in db.");
            }
            else {
                //if another version is present in db, check if this element is more recent
                Long lastVersion = getLastVersion(element, redisClient);
                if (lastVersion == -1 ||  element.getVersion() > lastVersion) {
                    //add this version as the newest version to the version-list
                    redisClient.lpush(element.getUnversionedId(), element.getVersionedId());
                    //save the content and the meta-data in a hash with id "[elementId]:[version]"
                    redisClient.hmset(element.getVersionedId(), element.toHash());
                }
                //if the same version is present in db, check if their content is equal, if not, throw exception
                else if(lastVersion == element.getVersion()){
                    StorableElement storedElement = this.fetchElement(element.getVersionedId());
                    if(!element.equals(storedElement)){
                        throw new RedisException("Trying to save element '" + element.getUnversionedId() + "' with version '" + element.getVersion() + "' which already exists in db, but has other content than found in db.");
                    }
                }
                else {
                    throw new RedisException(
                                    "The element '" + element.getUnversionedId() + "' with version '" + element.getVersion() + "' already has a more recent version '" + lastVersion +
                                    "' present in db.");
                }
            }
        }
    }

    /**
     * Get the last version of a page and all of it's blocks an rows from the db
     * @param url to the page to fetch the last version for
     * @return page from db
     */
    public Page fetchPage(URL url) throws RedisException
    {
        try (Jedis redisClient = pool.getResource()){
            Page retVal = null;
            PageID wrongVersionId = new PageID(url);
            if(!redisClient.exists(wrongVersionId.getUnversionedId())){
                return null;
            }
            //get the last version of the page corresponding to the given url
            List<String> pageVersions = redisClient.lrange(wrongVersionId.getUnversionedId(), 0, 0);
            if(pageVersions.isEmpty()) {
                return null;
            }
            else{
                String lastVersionStringId = pageVersions.get(0);
                PageID lastVersionPageId = new PageID(lastVersionStringId);
                PageClass pageClass = getPageClass(lastVersionPageId, redisClient);
                retVal = new Page(lastVersionPageId, pageClass);

                Set<String> elements = redisClient.smembers(lastVersionStringId);
                if(!elements.isEmpty()){
                    for(String elementId : elements){
                        retVal.addElement(fetchElement(elementId));
                    }
                }
                else{
                    //no non-default rows or blocks were returned, so do nothing
                }
                //TODO BAS: page-info has to be read in here too!, and then it is possible to return an "empty" Page, instead of 'null' like it is now, (with a pageClass and an ID, but with no rows or blocks in it (ohter than the once from it's page-class)
            }
            return retVal;
        }
        catch(Exception e){
            throw new RedisException("Could not fetch page with url '" + url + "' from db.", e);
        }
    }



    /**
     * Get an element from the db.
     * @param elementId the id of an element to fetch, it can be a versioned or unversioned id
     * @return an element from db, the newest version if it is an unversioned id and the version specified by the id if it is a versioned id, or null if the element is not present in db
     */
    public StorableElement fetchElement(String elementId) throws RedisException
    {
        try(Jedis redisClient = pool.getResource()) {

            /*
             * Look for the right version of this element. If this id is a redis-list, then take the newest version, if it is a redis-string, take this specific version
             */
            String redisType = redisClient.type(elementId);
            String elementVersionedId = "";
            if (redisType.equals("string")) {
                //the versioned id was specified as a parameter
                elementVersionedId = elementId;
            }
            else if(redisType.equals("list")){
                List<String> versions = redisClient.lrange(elementId, 0, 0);
                if(versions.isEmpty()) {
                    return null;
                }
                else{
                    //we found the versioned id in the list
                    elementVersionedId = versions.get(0);
                }
            }
            else if(redisType.equals("none")){
                return null;
            }
            else if(redisType.equals("set") || redisType.equals("zset") || redisType.equals("zset") || redisType.equals("hash")){
                throw new RedisException("Unsupported element of redis-type: " + redisType);
            }
            else{
                throw new RedisException("Unsupported Redis-type: " + redisType);
            }


            /*
             * With the right version we have now found, fetch all data and return a correct type of java-object.
             */
            ElementID elementRedisId = new ElementID(elementVersionedId);
            Map<String, String> elementHash = redisClient.hgetAll(elementVersionedId);
            if(!elementHash.containsKey("type")){
                throw new RedisException("No object-type found for element: " + elementVersionedId);
            }
            else{
                String type = elementHash.get("type");
                if(type.equals(Block.class.getSimpleName())){
                    //a block from db is always changeable, thus isFinal is set to true
                    return new Block(elementRedisId, elementHash.get("content"), true);
                }
                else if(type.equals(Row.class.getSimpleName())){
                    //a row from db is always changeable, thus isFinal is set to true
                    return new Row(elementRedisId, elementHash.get("content"), true);
                }
                else{
                    throw new RedisException("Unsupported element-type found: " + type);
                }
            }
        }
        catch(RedisException e){
            throw e;
        }
        catch(Exception e){
            throw new RedisException("Error while fetching element from db.", e);
        }
    }

    /**
     * Get a UID for a new page instance for db-representation and return a new page with that id, copying all default blocks and rows from the page-class
     * @param pageClass the page-class for a new page
     * @return a new page of class 'pageClass'
     */
    public Page getNewPage(PageClass pageClass)
    {
        try(Jedis redisClient = pool.getResource()) {
            PageID newPageID = pageClass.renderNewPageID();

            //Check if this page-id (url) is not already present in db, if so, re-render a random page-id
            while (redisClient.get(newPageID.getUnversionedId()) != null) {
                newPageID = pageClass.renderNewPageID();
            }
            Page newPage = new Page(newPageID, pageClass);
            return newPage;
        }
    }

    @Override
    public void close()
    {
        pool.destroy();
    }

    /**
     * Get the last saved version of a storable
     * @param storable the storable to get the last version of
     * @param redisClient the redis-client the be used to retrieve the last version
     * @return the last version saved in db, -1 if no version is present in db
     */
    private Long getLastVersion(Storable storable, Jedis redisClient){
        //get last saved version from db
        List<String> versions = redisClient.lrange(storable.getUnversionedId(), 0, 0);
        if(!versions.isEmpty()) {
            String[] splitted = versions.get(0).split(":");
            return Long.valueOf(splitted[splitted.length - 1]);
        }
        else{
            return new Long(-1);
        }
    }

    /**
     * Get the pageclass for a specified page. Note that the version of the page is going to be used to get the page's info.
     * @param pageID the ID of the page to get the pageclass-from
     * @return
     */
    private PageClass getPageClass(PageID pageID, Jedis redisClient) throws PageClassCacheException
    {
        String pageClassName = redisClient.hget(pageID.getPageInfoId(), "pageClass");
        PageClass pageClass = PageClassCache.getInstance().getPageClassCache().get(pageClassName);
        if (pageClass != null) {
            return pageClass;
        }
        else {
            throw new PageClassCacheException("No PageClass: '" + pageClassName + "' found in cache, this should not happen. Probably something went wrong while loading the page-classes from file.");
        }
    }
}
