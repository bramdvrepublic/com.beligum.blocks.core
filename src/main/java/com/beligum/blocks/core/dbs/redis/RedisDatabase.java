package com.beligum.blocks.core.dbs.redis;

import com.beligum.blocks.core.base.Blocks;
import com.beligum.blocks.core.config.DatabaseConstants;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.exceptions.DatabaseException;
import com.beligum.blocks.core.identifiers.redis.BlocksID;
import com.beligum.blocks.core.internationalization.Languages;
import com.beligum.blocks.core.models.redis.Storable;
import com.beligum.blocks.core.models.redis.templates.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Created by bas on 09.10.14.
 * Wrapper class for talking to the redis-db
 * At the end of the application it has to be closed
 */
public class RedisDatabase implements Database<AbstractTemplate>
{
    /*
     * Note: check/boost redis-performance with http://redis.io/topics/benchmarks
     */


    //a thread-save pool for connection to the redis-master-server
    //        TODO: put Redis back to Sentinel-state
    // private final JedisSentinelPool pool;
    private final JedisPool pool;

    //the instance of this singleton
    private static RedisDatabase instance = null;

    /**
     * private constructor for singleton-use
     */
    private RedisDatabase(){
        //create a thread-save pool of Jedis-instances, using default configuration
        //        TODO: put Redis back to Sentinel-state
        //        String[] sentinelHostsAndPorts = Blocks.config().getRedisSentinels();
        //        Set<String> sentinels = new HashSet<>();
        //        for(int i = 0; i<sentinelHostsAndPorts.length; i++){
        //            sentinels.add(sentinelHostsAndPorts[i]);
        //        }
        //        pool = new JedisSentinelPool(Blocks.config().getRedisMasterName(), sentinels);
        pool = null; //new JedisPool(Blocks.config().getRedisMasterHost(), Integer.parseInt(Blocks.config().getRedisMasterPort()));
    }

    /**
     * Static method for getting a singleton redis-wrapper
     * @return a singleton instance of Redis
     */
    public static Database getInstance()
    {
        if(instance == null){
            instance = new RedisDatabase();
        }
        return instance;
    }

    /**
     * Save a new version of a template to db. This method does NOT check if the new version is actually different from the old one.
     * It also takes all previously stored languages and copies them (the id's) to the new version.
     * @param template
     * @throws com.beligum.blocks.core.exceptions.DatabaseException if no previous version is present in db
     */
    @Override
    public void update(AbstractTemplate template) throws DatabaseException
    {
        try(Jedis redisClient = pool.getResource()){
            if(!redisClient.exists(template.getUnversionedId())){
                throw new DatabaseException("Cannot update an entity that is not present in db. Try creating it first. '" + template.getUnversionedId() + "'.");
            }
        }
        /*
         * Make sure the update- and creation-fields are correctly initialized before save.
         */
        if(template.getCreatedBy() == null || template.getCreatedAt() == null){
            AbstractTemplate lastVersion = this.fetchLastVersion(template.getId(), template.getClass());
            if(lastVersion == null) throw new DatabaseException("Cannot update an entity that does not have a previous version in db: '" + template.getUnversionedId() + "'.");
            if(template.getCreatedBy() == null) template.setCreatedBy(lastVersion.getCreatedBy());
            if(template.getCreatedAt() == null) template.setCreatedAt(lastVersion.getCreatedAt());
        }
        Storable.setUpdate(template);
        this.save(template);
    }

    /**
     * Save a new template to db.
     * @param template
     * @throws com.beligum.blocks.core.exceptions.DatabaseException if the template already is present in db
     */
    @Override
    public void create(AbstractTemplate template) throws DatabaseException
    {
        try(Jedis redisClient = pool.getResource()){
            if(redisClient.exists(template.getUnversionedId())){
                throw new DatabaseException("Cannot create an entity that already exists. Try updating it instead. '" + template.getUnversionedId() + "'.");
            }
        }
        this.save(template);
    }

    /**
     * Save a new version of a template to db. This method does NOT check if the new version is actually different from the old one.
     * It also takes all previously stored languages and copies them (the id's) to the new version.
     * @param template
     * @throws com.beligum.blocks.core.exceptions.DatabaseException
     */
    private void save(AbstractTemplate template) throws DatabaseException
    {
        try (Jedis redisClient = pool.getResource()) {
            //            if their already exists an element-set for this entity-version, throw exception
            if (redisClient.exists(template.getVersionedId())) {
                throw new DatabaseException("The entity-template '" + template.getUnversionedId() + "' already has a version '" + template.getVersion() + "' present in db.");
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
                lastVersion = this.getLastVersionNumber(template.getUnversionedId());
            }
            //pipeline this block of queries to retrieve all read-data at the end of the pipeline
            Pipeline pipelinedSaveTransaction = redisClient.pipelined();
            try{
                //use the redis 'MULTI'-command to start using a transaction (which is atomic)
                pipelinedSaveTransaction.multi();
                try {
                    if (lastVersion == BlocksID.NO_VERSION || template.getVersion() > lastVersion) {
                        pipelinedSaveTransaction.lpush(template.getUnversionedId(), template.getVersionedId());
                        //an EntityTemplate should also be saved in a set named after it's blueprintType
                        if (template instanceof EntityTemplate) {
                            String blueprintType = ((EntityTemplate) template).getEntityTemplateClass().getName();
                            pipelinedSaveTransaction.sadd(BlocksID.getBlueprintTypeSetId(blueprintType), template.getUnversionedId());
                        }
                    }
                    else {
                        throw new DatabaseException(
                                        "The entity-template '" + template.getUnversionedId() + "' with version '" + template.getVersion() + "' already has a more recent version '" + lastVersion +
                                        "' present in db.");
                    }

                    /*
                     * Save the entity's languaged-template-ids and other info (like it's entity-class) in a hash named "[entityId]:[version]"
                     * Make sure the id's of languages that were present in the previous version but not in this one, are copied to the new version.
                     */
                    if(storedTemplate != null) {
                        Map<BlocksID, String> storedTemplates = storedTemplate.getTemplates();
                        for (BlocksID languageId : storedTemplates.keySet()) {
                            template.add(languageId, storedTemplates.get(languageId));
                        }
                    }
                    Map<String, String> templateHash = template.toHash();
                    Map<BlocksID, String> languageTemplates = template.getTemplates();
                    for(BlocksID languageId : languageTemplates.keySet()){
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
            throw new DatabaseException("Could not save template '" + template.getId() + "' to db.", e);
        }
    }

    /**
     *
     * @param blueprintType
     * @return all entity-templates of the specified blueprint
     * @throws com.beligum.blocks.core.exceptions.DatabaseException
     */
    public Set<EntityTemplate> getEntityTemplatesOfBlueprint(String blueprintType) throws DatabaseException
    {
        try (Jedis redisClient = pool.getResource()){
            Set<String> entityIds = redisClient.smembers(blueprintType);
            Set<EntityTemplate> entities = new HashSet<>();
            //TODO: can we use pipelines (or transactions) here?
            for(String entityId : entityIds){
                EntityTemplate entityTemplate = (EntityTemplate) this.fetch(new BlocksID(entityId, this.getLastVersionNumber(entityId), BlocksID.PRIMARY_LANGUAGE), EntityTemplate.class);
                entities.add(entityTemplate);
            }
            return entities;
        }catch(IDException e){
            throw new DatabaseException("Could not construct an good id from the entity-template class-name '" + blueprintType + "'", e);
        }
    }

    /**
     * Get the specified version of a template.
     * @param id the id of the template in db
     * @param type The sort of template to be fetched
     * @return a template of the specified type, or null if no such template is present in db, or if no language information is present in the id
     * @throws com.beligum.blocks.core.exceptions.DatabaseException
     */
    @Override
    public AbstractTemplate fetch(BlocksID id, Class<? extends AbstractTemplate> type) throws DatabaseException
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
            throw new DatabaseException("Could not fetch entity-template with id '" + id + "' from db.", e);
        }
    }

    /**
     * Method for fetching exactly the string corresponding with the specified id from db.
     * Note: This method will throw Jedis-related exceptions, if no string-value is stored at that id.
     * @param id
     * @return The string stored in Redis with key the specified id, or null if no such key exists.
     */
    public String fetchStringForId(BlocksID id){
        try(Jedis redisClient = pool.getResource()){
            return redisClient.get(id.toString());
        }
    }

    /**
     * Fetch all language alternatives present in db for a template with a certain id.
     * This looks for alternative languages within the same version of the template.
     * @param id
     * @return a set with all language alternatives, or an empty one if no alternatives were found
     */
    public Set<String> fetchLanguageAlternatives(BlocksID id){
        try(Jedis redisClient = pool.getResource()){
            if(!redisClient.exists(id.getUnversionedId()) || !redisClient.exists(id.getVersionedId())){
                return new HashSet<>();
            }
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
    @Override
    public AbstractTemplate fetchLastVersion(BlocksID id, Class<? extends AbstractTemplate> type) throws DatabaseException
    {
        try(Jedis redisClient = pool.getResource()) {
            if(id == null){
                return null;
            }
            if (!redisClient.exists(id.getUnversionedId())) {
                return null;
            }
            BlocksID lastVersion = BlocksID.renderLanguagedId(id.getUrl(), BlocksID.LAST_VERSION, BlocksID.PRIMARY_LANGUAGE);
            if(!redisClient.exists(lastVersion.getVersionedId())){
                return null;
            }
            Map<String, String> entityHash = redisClient.hgetAll(lastVersion.getVersionedId());
            if(entityHash.isEmpty()){
                return null;
            }
            else if(this.isOfType(entityHash, type))
            {
                return TemplateFactory.createInstanceFromHash(lastVersion, entityHash, type);
            }
            else{
                return null;
            }
        }catch (Exception e){
            throw new DatabaseException("Could not fetch last version from db: " + id, e);
        }
    }

    /**
     * Empty database completely. ALL DATA WILL BE LOST!!! Use with care!
     */
    @Override
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
    public Long getLastVersionNumber(String unversionedId){
        try(Jedis redisClient = pool.getResource()) {
            if(!redisClient.exists(unversionedId)){
                return new Long(BlocksID.NO_VERSION);
            }
            //get last saved version from db
            List<String> versions = redisClient.lrange(unversionedId, 0, 0);
            if (!versions.isEmpty()) {
                String[] splitted = versions.get(0).split(":");
                return Long.valueOf(splitted[splitted.length - 1]);
            }
            else {
                return new Long(BlocksID.NO_VERSION);
            }
        }
    }

    /**
     * Method for getting a new randomly determined entity-uid (with versioning) for a entityInstance of an entityClass, used by RedisID to render a new, random and unique id.
     * @param language the language this new id should use
     * @return a randomly generated entity-id of the form "[site-domain]/[entityClassName]/[randomInt]"
     */
    public BlocksID renderNewEntityTemplateID(Blueprint blueprint, String language) throws IDException
    {
        try (Jedis redisClient = pool.getResource()){
            Random randomGenerator = new Random();
            int positiveNumber = Math.abs(randomGenerator.nextInt());
            String url = Blocks.config().getSiteDomain();
            if(Languages.isNonEmptyLanguageCode(language)){
                url += "/" + language;
            }
            else{
                url += "/" + blueprint.getLanguage();
            }
            url += "/" + blueprint.getName() + "/" + positiveNumber;
            BlocksID retVal = new BlocksID(new URL(url), BlocksID.NEW_VERSION, false);
            //Check if this entity-id (url) is not already present in db, if so, re-render a random entity-id
            while (redisClient.get(retVal.getUnversionedId()) != null) {
                positiveNumber = Math.abs(randomGenerator.nextInt());
                url = Blocks.config().getSiteDomain();
                if(Languages.isNonEmptyLanguageCode(language)){
                    url += "/" + language;
                }
                else{
                    url += "/" + blueprint.getLanguage();
                }
                url += "/" + blueprint.getName() + "/" + positiveNumber;
                retVal = new BlocksID(new URL(url), BlocksID.NEW_VERSION, false);
            }
            if(!retVal.hasLanguage()){
                retVal = new BlocksID(retVal, blueprint.getLanguage());
            }
            return retVal;
        }catch(MalformedURLException e){
            throw new IDException("Cannot render proper id with blueprint '" + blueprint.getName() +" and site-domain '" + Blocks.config().getSiteDomain() + "'.", e);
        }
    }

    /**
     * Trashes the specified id.
     * @param id
     * @return the last version of the template that has been trashed
     * @throws DatabaseException
     */
    @Override
    public AbstractTemplate trash(BlocksID id) throws DatabaseException
    {
        try{
            URL url = id.getUrl();
            /*
             * Since we want to block all access to all versions of the entity at this url,
             * we need only the path of the url specified.
             * That is why we redefine the entity-url starting from it's domain-name and path.
             */
            URL entityUrl = url;
            entityUrl = new URL(entityUrl, entityUrl.getPath());
            EntityTemplate storedVersion = (EntityTemplate) this.fetchLastVersion(new BlocksID(entityUrl, BlocksID.LAST_VERSION, true), EntityTemplate.class);
            if(storedVersion == null){
                throw new NullPointerException("Cannot trash '" + id + "', since no previous version of that entity was found in db.");
            }
            BlocksID newId = new BlocksID(url, BlocksID.NEW_VERSION, true);
            //if the language of the url to be trashed is not present in db, we're dealing with a not yet saved language of an entity, so we trash the whole entity
            if(storedVersion.getTemplate(newId.getLanguage()) == null){
                newId = new BlocksID(newId, storedVersion.getLanguage());
            }
            EntityTemplate newVersion = EntityTemplate.copyToNewId(storedVersion, newId);
            newVersion.setDeleted(true);
            this.update(newVersion);
            return storedVersion;
        }catch (Exception e){
            throw new DatabaseException("Could not trash entity at '" + id + "'.", e);
        }
    }

    @Override
    public List<AbstractTemplate> fetchVersionList(BlocksID id, Class<? extends AbstractTemplate> type) throws DatabaseException
    {
        if(id == null){
            return new ArrayList<>();
        }
        try(Jedis redisClient = pool.getResource()){
            List<AbstractTemplate> versionList = new ArrayList<>();
            List<String> versionedIds = redisClient.lrange(id.getUnversionedId(), 0, -1);
            for(String versionStringId : versionedIds){
                BlocksID versionId = new BlocksID(versionStringId, BlocksID.PRIMARY_LANGUAGE);
                AbstractTemplate template = this.fetch(versionId, type);
                versionList.add(template);
            }
            return versionList;
        }
        catch (Exception e){
            throw new DatabaseException("Could not get version-list for '" + id + "' from db.", e);
        }
    }

    private boolean isOfType(Map<String, String> hash, Class<? extends AbstractTemplate> type){
        if(type.equals(EntityTemplate.class)){
            return hash.containsKey(DatabaseConstants.BLUEPRINT_TYPE);
        }
        else{
            return true;
        }
    }

    public AbstractTemplate createOrUpdate(BlocksID templateId, AbstractTemplate newVersion, Class<? extends AbstractTemplate> type) throws DatabaseException
    {
        AbstractTemplate lastStoredVersion = (AbstractTemplate) RedisDatabase.getInstance().fetchLastVersion(templateId, type);
        if(lastStoredVersion == null) {
            RedisDatabase.getInstance().create(newVersion);
        }
        else if(!newVersion.equals(lastStoredVersion)){
            RedisDatabase.getInstance().update(newVersion);
        }
        else{
            //if this template was already stored in db, we should cache the db-version, since it has the correct time-stamp
            //TODO: properties should be read from db, for now we use the properties of the newVersion object
            lastStoredVersion.setProperties(newVersion.getProperties());
            newVersion = lastStoredVersion;
        }
        return newVersion;

    }



}
