package com.beligum.blocks.core.dbs;

import com.beligum.blocks.core.caching.PageClassCache;
import com.beligum.blocks.core.config.DatabaseFieldNames;
import com.beligum.blocks.core.exceptions.RedisException;
import com.beligum.blocks.core.identifiers.ElementID;
import com.beligum.blocks.core.identifiers.PageID;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.PageClass;
import com.beligum.blocks.core.models.ifaces.StorableElement;
import com.beligum.blocks.core.models.storables.Block;
import com.beligum.blocks.core.models.storables.Page;
import com.beligum.blocks.core.models.storables.Row;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.Pipeline;

import java.io.Closeable;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by bas on 09.10.14.
 * Wrapper class for talking to the redis-db
 * At the end of the application it has to be closed
 */
public class Redis implements Closeable
{
    private enum RedisType{
        STRING ("string"),
        LIST ("list"),
        NONE ("none"),
        SET ("set"),
        ORDERED_SET ("zset"),
        HASH ("hash");

        private final String name;

        RedisType(String name){
            this.name = name;
        }

        public String toString(){
            return name;
        }
    }

    //TODO check/boost redis-performance with http://redis.io/topics/benchmarks

    //TODO BAS SH: as a second thing today: implement transactions and/or pipelining (https://github.com/xetorthio/jedis/wiki/AdvancedUsage) and try running this on the Raspberry Pies


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
            //if their already exists an element-set for this page-version, throw exception
            if (redisClient.exists(page.getVersionedId())) {
                throw new RedisException("The page '" + page.getUnversionedId() + "' already has a version '" + page.getVersion() + "' present in db.");
            }

            //pipeline this block of queries to retrieve all read-data at the end of the pipeline
            Pipeline pipelinedSaveTransaction = redisClient.pipelined();
            //use the redis 'MULTI'-command to start using a transaction (which is atomic)
            pipelinedSaveTransaction.multi();
            try {
                /*
                 * Save all non-default, non-final block- and row-ids (with id "[elementId]:[version]" or "[elementId]") in a new set named "[pageId]:[version]"
                 * Save all new versions of blocks and rows "[elementId]:[version]" in a list named "[elementId]"
                 */
                //the page stored in db, will be null when no such page is present in db
                Page storedPage = this.fetchPage(page.getUrl());
                //elements of the page-class present in the cache that may not be altered, keys = html-id's of the elements, values = element-objects
                Map<String, StorableElement> finalElements = page.getPageClass().getFinalElements();
                //elements of the last version of this page stored in db that can be altered
                HashSet<StorableElement> storedElements = (storedPage != null) ? storedPage.getNonFinalElements() : new HashSet<StorableElement>();
                //elements of the page-class present in the cache that can be altered
                HashSet<StorableElement> cachedElements = page.getPageClass().getNonFinalElements();
                //elements of the page we want to save, which need to be compared to the above maps of elements
                Set<StorableElement> pageElements = page.getElements();
                //for all elements received from the page to be saved, check if they have to be saved to db, or if they are already present in the cache
                for (StorableElement pageElement : pageElements) {
                    if (!finalElements.containsKey(pageElement.getHtmlId())) {
                        if (!storedElements.contains(pageElement)) {
                            if (!cachedElements.contains(pageElement)) {
                                this.save(pageElement);
                                //TODO BAS: sometimes (like with calendar-blocks) here we will need to save the pageElementId's unversioned form
                                pipelinedSaveTransaction.sadd(page.getVersionedId(), pageElement.getVersionedId());
                            }
                            else {
                                //nothing has to be done with unaltered, cached elements when saving to db (they are already present in the application cache)
                            }
                        }
                        else {
                            //nothing has to be done with unaltered, already stored elements when saving to db (they are already present in db)
                        }
                    }
                    else {
                        StorableElement finalElement = finalElements.get(pageElement.getHtmlId());
                        if (!finalElement.equals(pageElement)) {
                            throw new RedisException("Final elements cannot be altered: element with id '" + finalElement.getHtmlId() + "' is final, so it cannot be changed to \n \n "
                                                     + pageElement.getContent() + "\n \n");
                        }
                        //nothing has to be done with unaltered final elements when saving to db (they are already present in the application cache and may not be altered anyhow)
                    }
                }

                /*
                 * Save this page-version's id ("[pageId]:[version]") to the db in the list named "<pageId>"
                 * holding all the different versions of this page-instance.
                 */
                //if another version is present in db, check if this page is more recent
                Long lastVersion = this.getLastVersion(page.getId());
                if (lastVersion == -1 || page.getVersion() > lastVersion) {
                    pipelinedSaveTransaction.lpush(page.getUnversionedId(), page.getVersionedId());
                }
                else {
                    throw new RedisException(
                                    "The page '" + page.getUnversionedId() + "' with version '" + page.getVersion() + "' already has a more recent version '" + lastVersion +
                                    "' present in db.");
                }

                /*
                 * Save other info about the page (like it's page-class) in a hash named "[pageId]:[version]:info"
                 */
                pipelinedSaveTransaction.hmset(page.getInfoId(), page.getInfo());

                //execute the transaction
                pipelinedSaveTransaction.exec();
                //do all the reads in this pipeline
                pipelinedSaveTransaction.sync();

            }
            catch(Exception e){
                //if an exception has been thrown while writing to, discard the transaction
                pipelinedSaveTransaction.discard();
                //do all the reads in this pipeline (not sure if this actually is necessary)
                pipelinedSaveTransaction.sync();
                throw e;
            }
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
                //pipeline this block of queries to retrieve all read-data at the end of the pipeline
                Pipeline pipelinedSaveTransaction = redisClient.pipelined();
                //use the redis 'MULTI'-command to start using a transaction (which is atomic)
                pipelinedSaveTransaction.multi();
                try {
                    //if another version is present in db, check if this element is more recent
                    Long lastVersion = this.getLastVersion(element.getId());
                    if (lastVersion == -1 || element.getVersion() > lastVersion) {
                        //add this version as the newest version to the version-list
                        pipelinedSaveTransaction.lpush(element.getUnversionedId(), element.getVersionedId());
                        //save the content and the meta-data in a hash with id "[elementId]:[version]"
                        pipelinedSaveTransaction.hmset(element.getVersionedId(), element.toHash());
                    }
                    //if the same version is present in db, check if their content is equal, if not, throw exception
                    else if (lastVersion == element.getVersion()) {
                        StorableElement storedElement = this.fetchElement(element.getVersionedId());
                        if (!element.equals(storedElement)) {
                            throw new RedisException("Trying to save element '" + element.getUnversionedId() + "' with version '" + element.getVersion() +
                                                     "' which already exists in db, but has other content than found in db.");
                        }
                    }
                    else {
                        throw new RedisException(
                                        "The element '" + element.getUnversionedId() + "' with version '" + element.getVersion() + "' already has a more recent version '" + lastVersion +
                                        "' present in db.");
                    }
                    //execute the transaction
                    pipelinedSaveTransaction.exec();
                    //do all the reads in this pipeline
                    pipelinedSaveTransaction.sync();
                }
                catch(Exception e){
                    //if an exception has been thrown while writing to, discard the transaction
                    pipelinedSaveTransaction.discard();
                    throw e;
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
            Long lastVersion = this.getLastVersion(wrongVersionId);
            if(lastVersion == -1) {
                return null;
            }
            else{
                PageID lastVersionId = new PageID(wrongVersionId.getUrl(), lastVersion);
                Map<String, String> pageInfoHash = redisClient.hgetAll(lastVersionId.getPageInfoId());
                PageClass pageClass = PageClassCache.getInstance().getPageClassCache().get(pageInfoHash.get(DatabaseFieldNames.PAGE_CLASS));
                if (pageClass == null) {
                    throw new RedisException("Db returned a page-class name '" + pageInfoHash.get(DatabaseFieldNames.PAGE_CLASS) + "' for page '" + url + "', but no such PageClass could be found in the application cache.");
                }
                Set<String> elementIds = redisClient.smembers(lastVersionId.getVersionedId());
                //if elements have been found in db, save them in the page
                if(!elementIds.isEmpty()){
                    Set<StorableElement> elements = new HashSet<>();
                    for(String elementId : elementIds){
                        elements.add(this.fetchElement(elementId));
                    }
                    //TODO BAS: what should we do when the page-info hash holds more info than use for this constructor? do we need a method page.addHashInfo(Map<String, String>) or something of the sort?
                    retVal = new Page(lastVersionId, elements, pageClass, pageInfoHash.get(DatabaseFieldNames.APP_VERSION), pageInfoHash.get(DatabaseFieldNames.CREATOR));
                }
                //if no elements have been found, use the default elements from the page-class
                else{
                    //TODO BAS: what should we do when the page-info hash holds more info than use for this constructor? do we need a method page.addHashInfo(Map<String, String>) or something of the sort?
                    retVal = new Page(lastVersionId, pageClass, pageInfoHash.get(DatabaseFieldNames.APP_VERSION), pageInfoHash.get(DatabaseFieldNames.CREATOR));
                }
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
             * Get the right version of the element-id, i.d. the last version if it is an unversioned id or the specified version if it is.
             * With the right version found, fetch all data and return a correct type of java-object.
             */
            String elementVersionedId = this.getVersionedId(redisClient, elementId);
            ElementID elementRedisId = new ElementID(elementVersionedId);
            Map<String, String> elementHash = redisClient.hgetAll(elementVersionedId);
            if(!elementHash.containsKey(DatabaseFieldNames.ELEMENT_CLASS_TYPE)){
                throw new RedisException("No object-type found for element: " + elementVersionedId);
            }
            else{
                /*
                 * Retrieve field-values from the element's hash and construct a new element with those values
                 */
                String type = elementHash.get(DatabaseFieldNames.ELEMENT_CLASS_TYPE);
                String content = elementHash.get(DatabaseFieldNames.CONTENT);
                String pageClassName = elementHash.get(DatabaseFieldNames.PAGE_CLASS);
                String applicationVersion = elementHash.get(DatabaseFieldNames.APP_VERSION);
                String creator = elementHash.get(DatabaseFieldNames.CREATOR);
                //an element that previously was saved in db, has to be changable (non-final), if not it would not have been saved in db
                boolean isFinal = false;
                if(type.equals(Block.class.getSimpleName())){
                    return new Block(elementRedisId, content, pageClassName, isFinal, applicationVersion, creator);
                }
                else if(type.equals(Row.class.getSimpleName())){
                    return new Row(elementRedisId, content, pageClassName, isFinal, applicationVersion, creator);
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
     * Get the last saved version of a storable with a certain id. If the id is a versioned one, this method will not take into account that version, it always fetches the last saved version from db.
     * @param storableID the id of the storable to get the last version of
     * @return the last version-number saved in db, -1 if no version is present in db
     */
    private Long getLastVersion(RedisID storableID){
        try(Jedis redisClient = pool.getResource()) {
            //get last saved version from db
            List<String> versions = redisClient.lrange(storableID.getUnversionedId(), 0, 0);
            if (!versions.isEmpty()) {
                String[] splitted = versions.get(0).split(":");
                return Long.valueOf(splitted[splitted.length - 1]);
            }
            else {
                return new Long(-1);
            }
        }
    }

    /**
     * Look for the right version of this element. If this element-id points to a redis-list, then take the newest version, if it is a redis-hash, return the specified version
     * @param redisClient
     * @param elementId string representing an element's id
     * @return a string representing a versioned id, be it the last version, or the version specified by the specified element-id
     * @throws RedisException if an unsupported element-id is specified (i.d. an element-id that is not a redis-'hash' or -'list')
     */
    private String getVersionedId(Jedis redisClient, String elementId) throws RedisException
    {
        String redisType = redisClient.type(elementId);
        if (redisType.contentEquals(RedisType.HASH.toString())) {
            //the versioned id was specified as a parameter
            return elementId;
        }
        else if(redisType.contentEquals(RedisType.LIST.toString())){
            List<String> versions = redisClient.lrange(elementId, 0, 0);
            if(versions.isEmpty()) {
                return null;
            }
            else{
                //we found the versioned id in the list
                return  versions.get(0);
            }
        }
        else if(redisType.contentEquals(RedisType.NONE.toString())  || redisType.contentEquals(RedisType.SET.toString())
                || redisType.contentEquals(RedisType.ORDERED_SET.toString())  || redisType.contentEquals(RedisType.STRING.toString())){
            throw new RedisException("Unsupported element of redis-type: " + redisType);
        }
        else{
            throw new RedisException("Unsupported or unexisting Redis-type: " + redisType);
        }
    }


}
