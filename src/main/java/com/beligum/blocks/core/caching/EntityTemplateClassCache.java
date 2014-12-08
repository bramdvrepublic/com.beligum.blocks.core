package com.beligum.blocks.core.caching;

import com.beligum.blocks.core.config.CacheConstants;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.templates.AbstractTemplate;
import com.beligum.blocks.core.models.templates.EntityTemplateClass;
import com.beligum.core.framework.base.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by bas on 07.10.14.
 * Singleton for interacting with the applications page-cache, containing pairs of (page-class, default-page-instance)
 */
public class EntityTemplateClassCache extends AbstractTemplatesCache<EntityTemplateClass>
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
     * Get the entity-template-class with a certain name from the application cache.
     * If that entity-template-class does not exist, return the default entity-class.
     *
     * @param name the unique name of the entity-class to get
     * @return an entity-class from the application cache
     */
    @Override
    public EntityTemplateClass get(String name) throws CacheException
    {
        Map<String, EntityTemplateClass> applicationCache = this.getCache();
        if(name == null) {
            //TODO BAS: when is the default-class added and how does it look like? this should be implemented!
            return applicationCache.get(CacheConstants.DEFAULT_ENTITY_CLASS_NAME);
        }
        else {
            try {
                EntityTemplateClass entityTemplateClass = applicationCache.get(RedisID.renderNewEntityTemplateClassID(name).getUnversionedId());
                if (entityTemplateClass != null) {
                    return entityTemplateClass;
                }
                else {
                    return applicationCache.get(CacheConstants.DEFAULT_ENTITY_CLASS_NAME);
                }
            }
            catch (IDException e) {
                throw new CacheException("Could not get " + EntityTemplateClass.class.getSimpleName() + "' from cache.", e);
            }
        }
    }
    /**
     * This method returns a map with all default page-instances (value) of all present pageClasses (key)
     * @returns a map of all the currently cached page-classes from the application cache
     */
    @Override
    protected Map<String, EntityTemplateClass> getCache(){
        return (Map<String, EntityTemplateClass>) R.cacheManager().getApplicationCache().get(CacheKeys.ENTITY_CLASSES);
    }

    /**
     * @return the object-class being stored in this cache
     */
    @Override
    public Class<? extends AbstractTemplate> getCachedClass()
    {
        return EntityTemplateClass.class;
    }
}
