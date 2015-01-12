package com.beligum.blocks.core.dbs;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.exceptions.RedisException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.internationalization.Languages;
import com.beligum.blocks.core.models.templates.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
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
    //        TODO: put Redis back to Sentinel-state
    // private final JedisSentinelPool pool;
    private final JedisPool pool;

    //the instance of this singleton
    private static Redis instance = null;

    /**
     * private constructor for singleton-use
     */
    private Redis(){
        //create a thread-save pool of Jedis-instances, using default configuration
//        TODO: put Redis back to Sentinel-state
//        String[] sentinelHostsAndPorts = BlocksConfig.getRedisSentinels();
//        Set<String> sentinels = new HashSet<>();
//        for(int i = 0; i<sentinelHostsAndPorts.length; i++){
//            sentinels.add(sentinelHostsAndPorts[i]);
//        }
//        pool = new JedisSentinelPool(BlocksConfig.getRedisMasterName(), sentinels);
        pool = new JedisPool(BlocksConfig.getRedisMasterHost(), Integer.parseInt(BlocksConfig.getRedisMasterPort()));
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
     * It also takes all previously stored languages and copies them (the id's) to the new version.
     * @param template
     * @throws com.beligum.blocks.core.exceptions.RedisException
     */
    public void save(AbstractTemplate template) throws RedisException
    {
        try (Jedis redisClient = pool.getResource()) {
            //            if their already exists an element-set for this entity-version, throw exception
            if (redisClient.exists(template.getVersionedId())) {
                throw new RedisException("The entity-template '" + template.getUnversionedId() + "' already has a version '" + template.getVersion() + "' present in db.");
            }
            /*
             * Save this entity-version's id ("[entityId]:[version]") to the db in the list named "[entityId]"
             * holding all the different versions of this entity-instance.
             */
            AbstractTemplate storedTemplate = this.fetchLastVersion(template.getId(), template.getClass());
            long lastVersion;
            if(storedTemplate != null) {
                lastVersion = storedTemplate.getVersion();
            }
            else{
                lastVersion = this.getLastVersion(template.getUnversionedId());
            }
            //pipeline this block of queries to retrieve all read-data at the end of the pipeline
            Pipeline pipelinedSaveTransaction = redisClient.pipelined();
            try{
                //use the redis 'MULTI'-command to start using a transaction (which is atomic)
                pipelinedSaveTransaction.multi();
                try {
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
                     * Save the entity's languaged-template-ids and other info (like it's entity-class) in a hash named "[entityId]:[version]"
                     * Make sure the id's of languages that were present in the previous version but not in this one, are copied to the new version.
                     */
                    if(storedTemplate != null) {
                        Map<RedisID, String> storedTemplates = storedTemplate.getTemplates();
                        for (RedisID languageId : storedTemplates.keySet()) {
                            template.add(languageId, storedTemplates.get(languageId));
                        }
                    }
                    Map<String, String> templateHash = template.toHash();
                    Map<RedisID, String> languageTemplates = template.getTemplates();
                    for(RedisID languageId : languageTemplates.keySet()){
                        pipelinedSaveTransaction.set(languageId.toString(), languageTemplates.get(languageId));
                    }
                    pipelinedSaveTransaction.hmset(template.getVersionedId(), templateHash);

                    //execute the transaction
                    pipelinedSaveTransaction.exec();
                }
                catch(Exception e){
                    //if an exception has been thrown while writing transaction, discard the transaction
                    pipelinedSaveTransaction.discard();
                    throw e;
                }
                //do all the reads in this pipeline
                pipelinedSaveTransaction.sync();
            }
            catch(Exception e){
                //do all the reads in this pipeline (not sure if this actually is necessary)
                pipelinedSaveTransaction.sync();
                throw e;
            }
        }catch(Exception e){
            throw new RedisException("Could not save template '" + template.getId() + "' to db.", e);
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
                EntityTemplate entityTemplate = this.fetchEntityTemplate(new RedisID(entityId, this.getLastVersion(entityId), RedisID.PRIMARY_LANGUAGE));
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
     * @return a template of the specified type, or null if no such template is present in db, or if no language information is present in the id
     * @throws RedisException
     */
    public AbstractTemplate fetchTemplate(RedisID id, Class<? extends AbstractTemplate> type) throws RedisException
    {
        try (Jedis redisClient = pool.getResource()) {
            if (!redisClient.exists(id.getUnversionedId())) {
                return null;
            }
            //if no such language is present in db, the template cannot be fetched
            if(this.fetchStringForId(id) == null){
                return null;
            }
            Map<String, String> entityHash = redisClient.hgetAll(id.getVersionedId());
            if(entityHash.isEmpty()){
                return null;
            }
            return TemplateFactory.createInstanceFromHash(id, entityHash, type);
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
     * Method for fetching exactly the string corresponding with the specified id from db.
     * Note: This method will throw Jedis-related exceptions, if no string-value is stored at that id.
     * @param id
     * @return The string stored in Redis with key the specified id, or null if no such key exists.
     */
    public String fetchStringForId(RedisID id){
        try(Jedis redisClient = pool.getResource()){
            return redisClient.get(id.toString());
        }
    }

    /**
     * Fetch all language alternatives present in db for a template with a certain id.
     * This looks for alternative languages within the same version of the template.
     * @param id
     * @return
     */
    public Set<String> fetchLanguageAlternatives(RedisID id){
        try(Jedis redisClient = pool.getResource()){
            Map<String, String> hash = redisClient.hgetAll(id.getVersionedId());
            Set<String> permittedLanguages = Languages.getPermittedLanguageCodes();
            Set<String> alternativeLangugaes = new HashSet<>();
            for(String key : hash.keySet()){
                if(permittedLanguages.contains(key)){
                    alternativeLangugaes.add(key);
                }
            }
            return alternativeLangugaes;
        }
    }

    /**
     *
     * @return the last version of an entity-template, or null if not present
     */
    public AbstractTemplate fetchLastVersion(RedisID id, Class<? extends AbstractTemplate> type) throws RedisException {
        try {
            RedisID lastVersion = RedisID.renderLanguagedId(id.getUrl(), RedisID.LAST_VERSION, RedisID.PRIMARY_LANGUAGE);
            return this.fetchTemplate(lastVersion, type);
        }catch (Exception e){
            throw new RedisException("Could not fetch last version from db: " + id, e);
        }
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

    /**
     * Empty database completely. ALL DATA WILL BE LOST!!! Use with care!
     * @return
     */
    public void flushDB(){
        try(Jedis redisClient = pool.getResource()){
            redisClient.flushDB();
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
        RedisID wrongVersionId = new RedisID(entityUrl, RedisID.NO_VERSION, false);
        return getLastVersion(wrongVersionId.getUnversionedId());
    }

    /**
     * Method for getting a new randomly determined entity-uid (with versioning) for a entityInstance of an entityClass, used by RedisID to render a new, random and unique id.
     * @param language the language this new id should use
     * @return a randomly generated entity-id of the form "[site-domain]/[entityClassName]/[randomInt]"
     */
    public RedisID renderNewEntityTemplateID(EntityTemplateClass entityTemplateClass, String language) throws IDException
    {
        try (Jedis redisClient = pool.getResource()){
            Random randomGenerator = new Random();
            int positiveNumber = Math.abs(randomGenerator.nextInt());
            String url = BlocksConfig.getSiteDomain();
            if(Languages.isNonEmptyLanguageCode(language)){
                url += "/" + language;
            }
            else{
                url += "/" + entityTemplateClass.getLanguage();
            }
            url += "/" + entityTemplateClass.getName() + "/" + positiveNumber;
            RedisID retVal = new RedisID(new URL(url), RedisID.NEW_VERSION, false);
            //Check if this entity-id (url) is not already present in db, if so, re-render a random entity-id
            while (redisClient.get(retVal.getUnversionedId()) != null) {
                positiveNumber = Math.abs(randomGenerator.nextInt());
                url = BlocksConfig.getSiteDomain();
                if(Languages.isNonEmptyLanguageCode(language)){
                    url += "/" + language;
                }
                else{
                    url += "/" + entityTemplateClass.getLanguage();
                }
                url += "/" + entityTemplateClass.getName() + "/" + positiveNumber;
                retVal = new RedisID(new URL(url), RedisID.NEW_VERSION, false);
            }
            if(!retVal.hasLanguage()){
                retVal = new RedisID(retVal, entityTemplateClass.getLanguage());
            }
            return retVal;
        }catch(MalformedURLException e){
            throw new IDException("Cannot render proper id with entity-template-class '" + entityTemplateClass.getName() +" and site-domain '" + BlocksConfig.getSiteDomain() + "'.", e);
        }
    }



}
