package com.beligum.blocks.core.dbs;

import com.beligum.blocks.core.caching.PageCache;
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

import javax.ws.rs.NotFoundException;
import java.io.Closeable;
import java.io.IOException;
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
    public void save(Page page){
        try (Jedis redisClient = pool.getResource()){
            /*
             * Save this page-version's id ("[pageId]:[version]") to the db in the list named "<pageId>"
             * holding all the different versions of this page-instance.
             * If their already exist rows or blocks in the element-set of this page-version, throw exception
             */
            if (redisClient.exists(page.getVersionedId())) {
                throw new RuntimeException("The page '" + page.getUnversionedId() + "' already a version '" + page.getVersion() + "' present in db.");
            }
            //if another version is present in db, check if this page is more recent
            Long lastVersion = getLastVersion(page, redisClient);
            if (lastVersion == -1 ||  page.getVersion() > lastVersion) {
                redisClient.lpush(page.getUnversionedId(), page.getVersionedId());
            }
            else {
                throw new RuntimeException(
                                "The page '" + page.getUnversionedId() + "' with version '" + page.getVersion() + "' already has a more recent version '" + lastVersion +
                                "' present in db.");
            }
            redisClient.lpush(page.getUnversionedId(), page.getVersionedId());
            /*
             * Save all non-default (non-cached) blocks and rows (with id "[elementId]:[version]") in a set named "[pageId]:[version]"
             * Save content and info of an element in a hash named "[elementId]:[version]"
             */
            Set<StorableElement> defaultElements = page.getPageClass().getElements();
            for(StorableElement element : page.getElements()) {
                if(!defaultElements.contains(element)) {
                    this.save(element);
                }
            }
            /*
             * Save other info about the page (like it's page-class) in a hash named "[pageId]:[version]:info"
             */
            redisClient.hmset(page.getInfoId(), page.getInfo());
        }
    }


    /**
     * Save the element to db
     * @param element
     */
    public void save(StorableElement element)
    {
        try (Jedis redisClient = pool.getResource()){
            //if their already exists a hash corresponding to the version of this element, throw exception
            if (redisClient.exists(element.getVersionedId())) {
                throw new RuntimeException("The element '" + element.getUnversionedId() + "' already has a version '" + element.getVersion() + "' present in db.");
            }
            else {
                //if another version is present in db, check if this row is more recent
                Long lastVersion = getLastVersion(element, redisClient);
                if (lastVersion == -1 ||  element.getVersion() > lastVersion) {
                    //add this version as the newest version to the versionlist
                    redisClient.lpush(element.getUnversionedId(), element.getVersionedId());
                    //save the content and the meta-data in a hash with id "[elementId]:[version]"
                    redisClient.hmset(element.getVersionedId(), element.toHash());
                }
                else {
                    throw new RuntimeException(
                                    "The element '" + element.getUnversionedId() + "' with version '" + element.getVersion() + "' already has a more recent version '" + lastVersion +
                                    "' present in db.");
                }
            }
        }
    }

    /**
     * Get a page and all of it's blocks an rows from the db
     * @param url to the page to fetch the last version for
     * @return page from db
     */
    public Page fetchPage(URL url) throws URISyntaxException, MalformedURLException
    {
        try (Jedis redisClient = pool.getResource()){
            Page retVal = null;
            PageID wrongVersionId = new PageID(url);
            //get the last version of the page corresponding to the given url
            List<String> pageVersions = redisClient.lrange(wrongVersionId.getUnversionedId(), 0, 0);
            if(!pageVersions.isEmpty()){
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
            }
            else{
                throw new NotFoundException("The page " + url.toString() + " could not be found in the database.");
            }
            return retVal;
        }
    }



    /**
     * Get an element from the db.
     * @param elementId the id of an element to fetch, it can be a versioned or unversioned id
     * @return an element from db, the newest version if it is an unversioned id and the version specified by the id if it is a versioned id, or null if the element is not present in db
     */
    public AbstractElement fetchElement(String elementId) throws URISyntaxException, MalformedURLException
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
                throw new RuntimeException("Unsupported element of redis-type: " + redisType);
            }
            else{
                throw new RuntimeException("Unsupported Redis-type: " + redisType);
            }


            /*
             * With the right version we have now found, fetch all data and return a correct type of java-object.
             */
            RedisID elementRedisId = new RedisID(elementVersionedId);
            Map<String, String> elementHash = redisClient.hgetAll(elementVersionedId);
            if(!elementHash.containsKey("type")){
                throw new RuntimeException("No object-type found for element: " + elementVersionedId);
            }
            else{
                String type = elementHash.get("type");
                if(type.equals(Block.class.getSimpleName())){
                    return new Block(elementRedisId, elementHash.get("content"));
                }
                else if(type.equals(Row.class.getSimpleName())){
                    return new Row(elementRedisId, elementHash.get("content"));
                }
                else{
                    throw new RuntimeException("Unsupported element-type found: " + type);
                }
            }

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
    private PageClass getPageClass(PageID pageID, Jedis redisClient)
    {
        String pageClassName = redisClient.hget(pageID.getPageInfoId(), "pageClass");
        PageClass pageClass = PageCache.getInstance().getPageCache().get(pageClassName);
        if (pageClass != null) {
            return pageClass;
        }
        else {
            throw new NullPointerException("No PageClass: " + pageClassName + "found in cache, this should not happen. Probably something went wrong while loading the page-classes from file.");
        }
    }
}
