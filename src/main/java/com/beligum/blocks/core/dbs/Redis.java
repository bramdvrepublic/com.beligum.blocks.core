package com.beligum.blocks.core.dbs;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.exceptions.RedisException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.templates.AbstractTemplate;
import com.beligum.blocks.core.models.templates.EntityTemplate;
import com.beligum.blocks.core.models.templates.EntityTemplateClass;
import com.beligum.blocks.core.models.templates.PageTemplate;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.Pipeline;

import java.io.Closeable;
import java.net.MalformedURLException;
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


    //a thread-save pool for connection to the redis-master-server
    private final JedisSentinelPool pool;

    //the instance of this singleton
    private static Redis instance = null;

    /**
     * private constructor for singleton-use
     */
    private Redis(){
        //create a thread-save pool of Jedis-instances, using default configuration
        String[] sentinelHostsAndPorts = BlocksConfig.getRedisSentinels();
        Set<String> sentinels = new HashSet<>();
        for(int i = 0; i<sentinelHostsAndPorts.length; i++){
            sentinels.add(sentinelHostsAndPorts[i]);
        }
        pool = new JedisSentinelPool(BlocksConfig.getRedisMasterName(), sentinels);
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
     * Save a new version of a template to db. This method does NOT check if the new version is actually different from the old one.
     * @param template
     * @throws RedisException
     */
    public void save(AbstractTemplate template) throws RedisException
    {
        try (Jedis redisClient = pool.getResource()) {
            //            if their already exists an element-set for this entity-version, throw exception
            if (redisClient.exists(template.getVersionedId())) {
                throw new RedisException("The entity-template '" + template.getUnversionedId() + "' already has a version '" + template.getVersion() + "' present in db.");
            }
            //pipeline this block of queries to retrieve all read-data at the end of the pipeline
            Pipeline pipelinedSaveTransaction = redisClient.pipelined();
            try{
                //use the redis 'MULTI'-command to start using a transaction (which is atomic)
                pipelinedSaveTransaction.multi();
                /*
                 * Save this entity-version's id ("[entityId]:[version]") to the db in the list named "<entityId>"
                 * holding all the different versions of this entity-instance.
                 */
                //if another version is present in db, check if this entity is more recent
                Long lastVersion = this.getLastVersion(template.getUnversionedId());
                if (lastVersion == RedisID.NO_VERSION || template.getVersion() > lastVersion) {
                    pipelinedSaveTransaction.lpush(template.getUnversionedId(), template.getVersionedId());
                    //an EntityTemplate should also be saved in a set named after it's entityTemplateClassName
                    if (template instanceof EntityTemplate) {
                        String entityTemplateClassName = ((EntityTemplate) template).getEntityTemplateClass().getName();
                        pipelinedSaveTransaction.sadd(RedisID.getEntityTemplateClassSetId(entityTemplateClassName), template.getUnversionedId());
                    }
                }
                else {
                    throw new RedisException(
                                    "The entity-template '" + template.getUnversionedId() + "' with version '" + template.getVersion() + "' already has a more recent version '" + lastVersion +
                                    "' present in db.");
                }

                /*
                 * Save the entity's template and other info (like it's entity-class) in a hash named "[entityId]:[version]"
                 */
                pipelinedSaveTransaction.hmset(template.getVersionedId(), template.toHash());

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
                throw new RedisException("Could not save template '" + template.getId() + "' to db.", e);
            }
        }
    }

    /**
     *
     * @param entityTemplateClassName
     * @return all entity-templates of the specified entity-template-class
     * @throws RedisException
     */
    public Set<EntityTemplate> getEntityTemplatesOfClass(String entityTemplateClassName) throws RedisException
    {
        try (Jedis redisClient = pool.getResource()){
            Set<String> entityIds = redisClient.smembers(entityTemplateClassName);
            Set<EntityTemplate> entities = new HashSet<>();
            //TODO BAS: can we use pipelines (or transactions) here?
            for(String entityId : entityIds){
                EntityTemplate entityTemplate = this.fetchEntityTemplate(new RedisID(entityId, this.getLastVersion(entityId)));
                entities.add(entityTemplate);
            }
            return entities;
        }catch(IDException e){
            throw new RedisException("Could not construct an good id from the entity-template class-name '" + entityTemplateClassName + "'", e);
        }
    }

    /**
     * Get the specified version of a template.
     * @param id the id of the template in db
     * @param type The sort of template to be fetched
     * @return a template of the specified type
     * @throws RedisException
     */
    public AbstractTemplate fetchTemplate(RedisID id, Class<? extends AbstractTemplate> type) throws RedisException
    {
        try (Jedis redisClient = pool.getResource()) {
            if (!redisClient.exists(id.getUnversionedId())) {
                return null;
            }
            Map<String, String> entityHash = redisClient.hgetAll(id.getVersionedId());
            if(entityHash.isEmpty()){
                return null;
            }
            if(type == EntityTemplate.class){
                return EntityTemplate.createInstanceFromHash(id, entityHash);
            }
            else if(type == EntityTemplateClass.class){
                return EntityTemplateClass.createInstanceFromHash(id, entityHash);
            }
            else if(type == PageTemplate.class){
                return PageTemplate.createInstanceFromHash(id, entityHash);
            }
            else{
                throw new RedisException("Unsupported template-type: " + type.getName() + "'.");
            }
        }
        catch(RedisException e){
            throw e;
        }
        catch(Exception e){
            throw new RedisException("Could not fetch entity-template with id '" + id + "' from db.", e);
        }
    }

    /**
     * Get the specified version of a entity.
     * @param id the id of the entity
     * @return entity from db
     */
    public EntityTemplate fetchEntityTemplate(RedisID id) throws RedisException
    {
        return (EntityTemplate) fetchTemplate(id, EntityTemplate.class);
    }

    /**
     * Get the specified version of a entity-template-class
     * @param id the id of the entity-template-class
     * @return entity-template-class from db
     */
    public EntityTemplateClass fetchEntityTemplateClass(RedisID id) throws RedisException
    {
        return (EntityTemplateClass) fetchTemplate(id, EntityTemplateClass.class);
    }

    /**
     * Get the specified version of a page-template
     * @param id the id of the page-template
     * @return page-template from db
     */
    public PageTemplate fetchPageTemplate(RedisID id) throws RedisException
    {
        return (PageTemplate) fetchTemplate(id, PageTemplate.class);
    }

    /**
     *
     * @param id
     * @return the version from an id, or -1 if no version could be found in the id
     */
    private long getVersionFromId(String id){
        String[] splitted = id.split(":");
        //if an id is of the form "blocks://[entityId]:[version]", at least 3 parts should be splitted out of the id-string
        if(splitted.length >= 3){
            return new Long(splitted[splitted.length-1]);
        }
        else{
            return RedisID.NO_VERSION;
        }
    }


    @Override
    public void close()
    {
        pool.destroy();
    }

    /**
     * Get the last saved version of a storable with a certain id.
     * @param unversionedId the unversioned id of the storable to get the last version of
     * @return the last version-number saved in db, -1 if no version is present in db
     */
    public Long getLastVersion(String unversionedId){
        try(Jedis redisClient = pool.getResource()) {
            //get last saved version from db
            List<String> versions = redisClient.lrange(unversionedId, 0, 0);
            if (!versions.isEmpty()) {
                String[] splitted = versions.get(0).split(":");
                return Long.valueOf(splitted[splitted.length - 1]);
            }
            else {
                return new Long(RedisID.NO_VERSION);
            }
        }
    }

    /**
     * Get the last saved version of a entity with a certain url.
     * @param entityUrl the url of the entity to get the last version of
     * @return the last version-number saved in db, -1 if no version is present in db
     */
    public Long getLastVersion(URL entityUrl) throws IDException
    {
        RedisID wrongVersionId = new RedisID(entityUrl, RedisID.NO_VERSION);
        return getLastVersion(wrongVersionId.getUnversionedId());
    }

    /**
     * Method for getting a new randomly determined entity-uid (with versioning) for a entityInstance of an entityClass, used by RedisID to render a new, random and unique id.
     * @return a randomly generated entity-id of the form "[site-domain]/[entityClassName]/[randomInt]"
     */
    public RedisID renderNewEntityTemplateID(EntityTemplateClass entityTemplateClass) throws IDException
    {
        try (Jedis redisClient = pool.getResource()){
            Random randomGenerator = new Random();
            int positiveNumber = Math.abs(randomGenerator.nextInt());
            RedisID retVal = new RedisID(new URL(BlocksConfig.getSiteDomain() + "/" + entityTemplateClass.getName() + "/" + positiveNumber), RedisID.NEW_VERSION);
            //Check if this entity-id (url) is not already present in db, if so, re-render a random entity-id
            while (redisClient.get(retVal.getUnversionedId()) != null) {
                positiveNumber = Math.abs(randomGenerator.nextInt());
                retVal = new RedisID(new URL(BlocksConfig.getSiteDomain() + "/" + entityTemplateClass.getName() + "/" + positiveNumber), RedisID.NEW_VERSION);
            }
            return retVal;
        }catch(MalformedURLException e){
            throw new IDException("Cannot render proper id with entity-template-class '" + entityTemplateClass.getName() +" and site-domain '" + BlocksConfig.getSiteDomain() + "'.", e);
        }
    }



}
