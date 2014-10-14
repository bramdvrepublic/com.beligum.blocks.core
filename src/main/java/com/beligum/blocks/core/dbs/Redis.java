package com.beligum.blocks.core.dbs;

import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.PageClass;
import com.beligum.blocks.core.models.ifaces.Storable;
import com.beligum.blocks.core.models.ifaces.StorableElement;
import com.beligum.blocks.core.models.storables.Block;
import com.beligum.blocks.core.models.storables.Page;
import com.beligum.blocks.core.models.storables.Row;
import redis.clients.jedis.Jedis;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by bas on 09.10.14.
 * Wrapper class for talking to the redis-db
 */
public class Redis
{
    //TODO check/boost redis-performance with http://redis.io/topics/benchmarks


    // TODO BAS: how can be chosen which database-server is requested to read from? (random number?)

    //TODO BAS: get Redis running in --sentinel mode (so a slave can take over when the master fails)

    //TODO BAS: get this from configuration-file
    private static final int MASTER_PORT = 6379;
    private static final String MASTER_HOST = "localhost";
    private static final int SLAVE_PORT = 6380;
    private static final String SLAVE_HOST = "localhost";

    /**
     * Save the page to db, together with it's rows and blocks
     * @param page
     */
    public static void save(Page page){
        Jedis redisClient = new Jedis(MASTER_HOST, MASTER_PORT);
        try {
            /*
             * Save this page-version's id ("<pageId>:<version>") to the db in the list named "<pageId>"
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
             * Save all non-default (non-cached) blocks and rows (with id "<elementId>:<version>") in a set named "<pageId>:<version>"
             * Save content and info of an element in a hash named "<elementId>:<version>"
             */
            Set<StorableElement> defaultElements = page.getPageClass().getElements();
            for(StorableElement element : page.getElements()) {
                if(!defaultElements.contains(element)) {
                    Redis.save(element);
                    Map<String, String> elementHash = new HashMap<>();
                    elementHash.put("content", element.getContent());
                    elementHash.put("appVersion", "test");
                    elementHash.put("creator", "me");
                    redisClient.hmset(element.getVersionedId(), elementHash);
                }
            }
            /*
             * Save other info about the page (like it's page-class) in a hash named "<pageId>:<version>:info"
             */
            Map<String, String> pageInfo = new HashMap<>();
            pageInfo.put("appVersion", "test");
            pageInfo.put("creator", "me");
            pageInfo.put("pageClass", page.getPageClass().getName());
            redisClient.hmset(page.getInfoId(), pageInfo);
        }
        finally{
            try{
                if(redisClient != null){
                    redisClient.close();
                }
            }catch(Exception e){}
        }
    }


    /**
     * Save the element to db
     * @param element
     */
    public static void save(StorableElement element)
    {
        Jedis redisClient = new Jedis(MASTER_HOST, MASTER_PORT);
        try {
            //if their already exists a hash corresponding to the version of this element, throw exception
            if (redisClient.exists(element.getVersionedId())) {
                throw new RuntimeException("The element '" + element.getUnversionedId() + "' already has a version '" + element.getVersion() + "' present in db.");
            }
            else {
                //if another version is present in db, check if this row is more recent
                Long lastVersion = getLastVersion(element, redisClient);
                if (lastVersion == -1 ||  element.getVersion() > lastVersion) {
                    redisClient.lpush(element.getUnversionedId(), element.getVersionedId());
                }
                else {
                    throw new RuntimeException(
                                    "The element '" + element.getUnversionedId() + "' with version '" + element.getVersion() + "' already has a more recent version '" + lastVersion +
                                    "' present in db.");
                }
            }
        }
        finally {
            try {
                if (redisClient != null) {
                    redisClient.close();
                }
            }
            catch (Exception e) {
            }
        }
    }

    /**
     * Get a page and all of it's blocks an rows from the db
     * @param uid the id of the page to fetch
     * @return page from db
     */
    public Page getPage(URI uid) throws URISyntaxException
    {
        //TODO BAS: implement this
        return new Page(null, null);
    }
    /**
     * Get a row from the db
     * @param id the id of the row to fetch
     * @return
     */
    public Row getRow(RedisID id) throws URISyntaxException
    {
        //TODO BAS: implement this
        return new Row(id, "");
    }
    /**
     * Get a block from the db
     * @param id the id of the block to fetch
     * @return
     */
    public Block getBlock(RedisID id) throws URISyntaxException
    {
        //TODO BAS: implement this
        return new Block(id, "");
    }

    /**
     * Get a UID for a new page instance for db-representation and return a new page with that id, copying all default blocks and rows from the page-class
     * @param pageClass the page-class for a new page
     * @return a new page of class 'pageClass'
     */
    public static Page getNewPage(PageClass pageClass)
    {
        Jedis redisClient = new Jedis(SLAVE_HOST, SLAVE_PORT);
        try {
            RedisID newPageID = pageClass.renderNewPageID();
            
            //Check if this page-id (url) is not already present in db, if so, re-render a random page-id
            while (redisClient.get(newPageID.getUnversionedId()) != null) {
                newPageID = pageClass.renderNewPageID();
            }
            Page newPage = new Page(newPageID, pageClass);
            return newPage;
        }
        finally{
            try{
                if(redisClient != null){
                    redisClient.close();
                }
            }catch(Exception e){}
        }
    }

    /**
     * Get the last saved version of a storable
     * @param storable the storable to get the last version of
     * @param redisClient the redis-client the be used to retrieve the last version
     * @return the last version saved in db, -1 if no version is present in db
     */
    private static Long getLastVersion(Storable storable, Jedis redisClient){
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
}
