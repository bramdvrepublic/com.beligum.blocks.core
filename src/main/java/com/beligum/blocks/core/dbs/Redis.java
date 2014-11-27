package com.beligum.blocks.core.dbs;

import com.beligum.blocks.core.config.DatabaseConstants;
import com.beligum.blocks.core.exceptions.RedisException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.classes.EntityClass;
import com.beligum.blocks.core.models.storables.Entity;
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
     * Save the entity to db, together with it's rows and blocks
     * @param entity
     */
    //TODO BAS: possibly you could write unit test for this basic method: making a new default-entity should always result in the store and returnal of the exact html in the index.html of a default entity
    public void save(Entity entity) throws RedisException
    {
        try (Jedis redisClient = pool.getResource()){
            //if their already exists an element-set for this entity-version, throw exception
            if (redisClient.exists(entity.getVersionedId())) {
                throw new RedisException("The entity '" + entity.getUnversionedId() + "' already has a version '" + entity.getVersion() + "' present in db.");
            }

            //pipeline this block of queries to retrieve all read-data at the end of the pipeline
            Pipeline pipelinedSaveTransaction = redisClient.pipelined();
            //use the redis 'MULTI'-command to start using a transaction (which is atomic)
            pipelinedSaveTransaction.multi();
            try {
                /*
                 * Save all non-default, non-final block- and row-ids (with id "[elementId]:[version]" or "[elementId]") in a new set named "[entityId]:[version]"
                 * Save all new versions of blocks and rows "[elementId]:[version]" in a list named "[elementId]"
                 */
                //the entity stored in db, will be null when no such entity is present in db
                Entity storedEntity = this.fetchEntity(entity.getId(), true, true);
                //elements of the entity-class present in the cache that may not be altered, keys = html-id's of the elements, values = element-objects
                Map<String, Entity> finalChildren = entity.getAllFinalChildren();
                //elements of the last version of this entity stored in db that can be altered
                HashSet<Entity> storedChildren = (storedEntity != null) ? storedEntity.getNotCachedNonFinalChildren() : new HashSet<Entity>();
                //elements of the entity-class present in the cache that can be altered
                HashSet<Entity> cachedChildren = entity.getViewableClass().getAllNonFinalChildren();
                //elements of the entity we want to save, which need to be compared to the above maps of elements
                Set<Entity> entityChildren = entity.getAllChildren();
                //for all elements received from the entity to be saved, check if they have to be saved to db, or if they are already present in the cache
                for (Entity entityChild : entityChildren) {
                    if (!finalChildren.containsKey(entityChild.getHtmlId())) {
                        //TODO BAS: we also want to save id's of previous entities in db, so if we change the class-template and then return to the original template, we can easily return the original stored elements, this can be done by checking which storedElments haven't been accessed by a entityElement yet and then save the id's of those elements to
                        if (!storedChildren.contains(entityChild)) {
                            if (!cachedChildren.contains(entityChild)) {
                                this.save(entityChild);
                                //TODO BAS: sometimes (like with calendar-blocks) here we will need to save the entityElementId's unversioned form
                                pipelinedSaveTransaction.sadd(entity.getVersionedId(), entityChild.getVersionedId());
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
                        Entity finalChild = finalChildren.get(entityChild.getHtmlId());
                        if (!finalChild.equals(entityChild)) {
                            throw new RedisException("Entity with id '" + finalChild.getHtmlId() + "' is final, so it cannot be changed from A) to B) \n \n A) \n"
                                                     + finalChild.getTemplate() +  "\n \n B) \n " + entityChild.getTemplate() + "\n \n");
                        }
                        //nothing has to be done with unaltered final elements when saving to db (they are already present in the application cache and may not be altered anyhow)
                    }
                }

                /*
                 * Save this entity-version's id ("[entityId]:[version]") to the db in the list named "<entityId>"
                 * holding all the different versions of this entity-instance.
                 */
                //if another version is present in db, check if this entity is more recent
                Long lastVersion = this.getLastVersion(entity.getId());
                if (lastVersion == -1 || entity.getVersion() > lastVersion) {
                    pipelinedSaveTransaction.lpush(entity.getUnversionedId(), entity.getVersionedId());
                }
                else {
                    throw new RedisException(
                                    "The entity '" + entity.getUnversionedId() + "' with version '" + entity.getVersion() + "' already has a more recent version '" + lastVersion +
                                    "' present in db.");
                }

                /*
                 * Save other info about the entity (like it's entity-class) in a hash named "[entityId]:[version]:info"
                 */
                pipelinedSaveTransaction.hmset(entity.getHashId(), entity.toHash());

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


//    /**
//     * Save the block to db: save template and info of a block in a hash named "[blockId]:[version]:hash"
//     * @param row
//     */
//    public void save(Entity row) throws RedisException
//    {
//        try (Jedis redisClient = pool.getResource()){
//            //if their already exists a hash corresponding to the version of this element, throw exception
//            if (redisClient.exists(row.getVersionedId())) {
//                throw new RedisException("The element '" + row.getUnversionedId() + "' already has a version '" + row.getVersion() + "' present in db.");
//            }
//            else {
//                //pipeline this block of queries to retrieve all read-data at the end of the pipeline
//                Pipeline pipelinedSaveTransaction = redisClient.pipelined();
//                //use the redis 'MULTI'-command to start using a transaction (which is atomic)
//                pipelinedSaveTransaction.multi();
//                try {
//                    //if another version is present in db, check if this element is more recent
//                    Long lastVersion = this.getLastVersion(row.getId());
//                    if (lastVersion == -1 || row.getVersion() > lastVersion) {
//                        //TODO BAS: here all child-rows should be added to a set, maybe only the id's should be saved, that's probably enough
//                        //add this version as the newest version to the version-list
//                        pipelinedSaveTransaction.lpush(row.getUnversionedId(), row.getVersionedId());
//                        //save the template and the meta-data in a hash with id "[elementId]:[version]"
//                        pipelinedSaveTransaction.hmset(row.getHashId(), row.toHash());
//                    }
//                    //if the same version is present in db, check if their template is equal, if not, throw exception
//                    else if (lastVersion == row.getVersion()) {
//                        Storable storedElement = this.fetchRow(row.getVersionedId());
//                        if (!row.equals(storedElement)) {
//                            throw new RedisException("Trying to save element '" + row.getUnversionedId() + "' with version '" + row.getVersion() +
//                                                     "' which already exists in db, but has other content than found in db.");
//                        }
//                    }
//                    else {
//                        throw new RedisException(
//                                        "The element '" + row.getUnversionedId() + "' with version '" + row.getVersion() + "' already has a more recent version '" + lastVersion +
//                                        "' present in db.");
//                    }
//                    //execute the transaction
//                    pipelinedSaveTransaction.exec();
//                    //do all the reads in this pipeline
//                    pipelinedSaveTransaction.sync();
//                }
//                catch(Exception e){
//                    //if an exception has been thrown while writing to, discard the transaction
//                    pipelinedSaveTransaction.discard();
//                    throw e;
//                }
//            }
//        }
//    }

    /**
     * Get the specified version of a entity.
     * @param id the id of the entity
     * @param fetchLastVersion true if the last version of the entity must be fetched, false if the version of the redis-id must be used
     * @param fetchChildren true if the children of this entity must be fetched from db too, or false if not (and an entity with an empty children-set is returned)
     * @return entity from db
     */
    public Entity fetchEntity(RedisID id, boolean fetchLastVersion, boolean fetchChildren) throws RedisException
    {
        try (Jedis redisClient = pool.getResource()){
            Entity retVal = null;
            if(!redisClient.exists(id.getUnversionedId())){
                return null;
            }
            //get the last version of the entity corresponding to the given url, or the one specified by the redis-id
            Long lastVersion = fetchLastVersion ? this.getLastVersion(id) : id.getVersion();
            if(lastVersion < 0) {
                return null;
            }
            else{
                RedisID lastVersionId = new RedisID(id.getUrl(), lastVersion);
                Map<String, String> entityInfoHash = redisClient.hgetAll(lastVersionId.getHashId());
                Set<Entity> childrenFromDB = new HashSet<>();
                if(fetchChildren) {
                    Set<String> childIds = redisClient.smembers(lastVersionId.getVersionedId());
                    for (String childStringId : childIds) {
                        RedisID childRedisId = new RedisID(childStringId);
                        childrenFromDB.add(this.fetchEntity(childRedisId, fetchLastVersion, fetchChildren));
                    }
                }
                String entityClassName = entityInfoHash.get(DatabaseConstants.VIEWABLE_CLASS);
                //TODO BAS: what should we do when the entity-info hash holds more info than use for this constructor? do we need a method entity.addHashInfo(Map<String, String>) or something of the sort?
                retVal = new Entity(lastVersionId, childrenFromDB, entityClassName, entityInfoHash.get(DatabaseConstants.APP_VERSION), entityInfoHash.get(DatabaseConstants.CREATOR));
            }
            return retVal;
        }
        catch(Exception e){
            throw new RedisException("Could not fetch entity with id '" + id + "' from db.", e);
        }
    }



//    /**
//     * Get an element from the db.
//     * @param elementId the id of an element to fetch, it can be a versioned or unversioned id
//     * @return an element from db, the newest version if it is an unversioned id and the version specified by the id if it is a versioned id, or null if the element is not present in db
//     */
//    public Entity fetchRow(String elementId) throws RedisException
//    {
//        try(Jedis redisClient = pool.getResource()) {
//            /*
//             * Get the right version of the element-id, i.d. the last version if it is an unversioned id or the specified version if it is.
//             * With the right version found, fetch all data and return a correct type of java-object.
//             */
//            String elementVersionedId = this.getVersionedId(redisClient, elementId);
//            RedisID elementRedisId = new RedisID(elementVersionedId);
//            Map<String, String> elementHash = redisClient.hgetAll(elementRedisId.getHashId());
//            if(!elementHash.containsKey(DatabaseConstants.ROW_TYPE)){
//                throw new RedisException("No object-type found for element: " + elementVersionedId);
//            }
//            else{
//                /*
//                 * Retrieve field-values from the element's hash and construct a new element with those values
//                 */
//                String type = elementHash.get(DatabaseConstants.ROW_TYPE);
//                String content = elementHash.get(DatabaseConstants.TEMPLATE);
//                String viewableClassName = elementHash.get(DatabaseConstants.VIEWABLE_CLASS);
//                String applicationVersion = elementHash.get(DatabaseConstants.APP_VERSION);
//                String creator = elementHash.get(DatabaseConstants.CREATOR);
//                //an element that previously was saved in db, has to be changable (non-final), if not it would not have been saved in db
//                boolean isFinal = false;
//                if(type.equals(Block.class.getSimpleName())){
//                    return new Block(elementRedisId, content, viewableClassName, isFinal, applicationVersion, creator);
//                }
//                else if(type.equals(Entity.class.getSimpleName())){
//                    //TODO BAS: hier moet een set met kinderen opgehaald worden
//                    return new Entity(elementRedisId, content, null, isFinal, applicationVersion, creator);
//                }
//                else{
//                    throw new RedisException("Unsupported element-type found: " + type);
//                }
//            }
//        }
//        catch(RedisException e){
//            throw e;
//        }
//        catch(Exception e){
//            throw new RedisException("Error while fetching element from db.", e);
//        }
//    }

    /**
     * Get a UID for a new entity instance for db-representation and return a new entity with that id, copying all default blocks and rows from the entity-class
     * @param entityClass the entity-class for a new entity
     * @return a new entity of class 'entityClass'
     */
    public Entity getNewEntity(EntityClass entityClass)
    {
        try(Jedis redisClient = pool.getResource()) {
            RedisID newEntityID = entityClass.renderNewEntityID();

            //Check if this entity-id (url) is not already present in db, if so, re-render a random entity-id
            while (redisClient.get(newEntityID.getUnversionedId()) != null) {
                newEntityID = entityClass.renderNewEntityID();
            }
            Entity newEntity = new Entity(newEntityID, entityClass);
            return newEntity;
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
            return elementId + ":" + DatabaseConstants.HASH_SUFFIX;
        }
        else if(redisType.contentEquals(RedisType.LIST.toString())){
            List<String> versions = redisClient.lrange(elementId, 0, 0);
            if(versions.isEmpty()) {
                return null;
            }
            else{
                //we found the versioned id in the list
                return  versions.get(0) + ":" + DatabaseConstants.HASH_SUFFIX;
            }
        }
        else if(redisType.contentEquals(RedisType.NONE.toString())){
            return elementId + ":" + DatabaseConstants.HASH_SUFFIX;
        }
        else if(redisType.contentEquals(RedisType.SET.toString())
                || redisType.contentEquals(RedisType.ORDERED_SET.toString())  || redisType.contentEquals(RedisType.STRING.toString())){
            throw new RedisException("Unsupported element of redis-type: " + redisType);
        }
        else{
            throw new RedisException("Unsupported or unexisting Redis-type: " + redisType);
        }
    }




}
