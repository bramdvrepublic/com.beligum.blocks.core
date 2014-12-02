package com.beligum.blocks.core.caching;

import com.beligum.blocks.core.config.CacheConstants;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.templates.EntityTemplateClass;
import com.beligum.core.framework.base.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by bas on 07.10.14.
 * Singleton for interacting with the applications page-cache, containing pairs of (page-class, default-page-instance)
 */
public class EntityTemplateClassCache extends AbstractStorablesCache<EntityTemplateClass>
{
    //the instance of this singleton
    private static EntityTemplateClassCache instance = null;

    /**
     * private constructor for singleton-use
     */
    private EntityTemplateClassCache(){
    }

    /**
     * Static method for getting a singleton page-class-cacher
     * @return a singleton instance of PageClassCache
     * @throws NullPointerException if no application cache could be found
     */
    public static EntityTemplateClassCache getInstance() throws CacheException
    {
        try {
            if (instance == null) {
                //if the application-cache doesn't exist, throw exception, else instantiate the application's page-cache with a new empty hashmap
                if (R.cacheManager() != null && R.cacheManager().getApplicationCache() != null) {

                    if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.ENTITY_CLASSES) || !R.cacheManager().getApplicationCache().containsKey(CacheKeys.PAGE_TEMPLATES)) {
                        R.cacheManager().getApplicationCache().put(CacheKeys.ENTITY_CLASSES, new HashMap<String, EntityTemplateClass>());
                        instance = new EntityTemplateClassCache();
                        //TODO save a default instance of all entity-classes to db (under blocks://LOC/entityClassName#propertyName), for remebering history of classes (check if class has changed before saving new instance to db)
                        instance.fillCache();
                    }
                }
                else {
                    throw new NullPointerException("No application-cache found.");
                }
            }
            return instance;
        }
        catch(CacheException e){
            throw new CacheException("Couldn't initialize entity-class-cache.", e);
        }
    }

    /**
     * Get the entity-class with a certain name from the application cache.
     * If that entity-class does not exist, return the default entity-class.
     *
     * @param name the unique name of the entity-class to get
     * @return an entity-class from the application cache
     */
    @Override
    public EntityTemplateClass get(String name) throws CacheException
    {
        Map<String, EntityTemplateClass> applicationCache = this.getCache();
        EntityTemplateClass entityTemplateClass = applicationCache.get(RedisID.renderNewEntityTemplateClassID(name).getUnversionedId());
        if(entityTemplateClass != null) {
            return entityTemplateClass;
        }
        else{
            return applicationCache.get(CacheConstants.DEFAULT_ENTITY_CLASS_NAME);
        }
    }
    /**
     * @param entityTemplateClass the entity-template-class to be added to the applications cache, the key will be the object's unversioned id
     */
    @Override
    public void add(EntityTemplateClass entityTemplateClass) throws CacheException
    {
        //TODO: for an entity the super method should be overwritten, so we can add the same EntityClass twice and choose for the 'bleuprint' to be kept in the cache
        //TODO 2: last version should be fetched from db and when the template has changed a new version should be created and saved to db
        super.add(entityTemplateClass);
    }
    /**
     * This method returns a map with all default page-instances (value) of all present pageClasses (key)
     * @returns a map of all the currently cached page-classes from the application cache
     */
    @Override
    public Map<String, EntityTemplateClass> getCache(){
        return (Map<String, EntityTemplateClass>) R.cacheManager().getApplicationCache().get(CacheKeys.ENTITY_CLASSES);
    }



//    /**
//     *
//     * @param entityClassName
//     * @return a valid ID-object constructed from the (unique) name of this identifiable object
//     * @throws CacheException if no valid ID can be constructed from the specified name
//     */
//    public ID getNewId(String entityClassName) throws CacheException
//    {
//        try{
//            return new ID(new URI(CacheConstants.ENTITY_CLASS_ID_PREFIX + "/" + entityClassName));
//        }
//        catch(URISyntaxException e){
//            throw new CacheException("No valid ID can be constructed from the entity-class name '" + entityClassName + "'.", e);
//        }
//    }
}
