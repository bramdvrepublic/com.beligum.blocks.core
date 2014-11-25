package com.beligum.blocks.core.caching;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.models.classes.EntityClass;
import com.beligum.core.framework.base.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by bas on 07.10.14.
 * Singleton for interacting with the applications page-cache, containing pairs of (page-class, default-page-instance)
 */
public class EntityClassCache extends AbstractIdentifiableObjectCache<EntityClass>
{
    //the instance of this singleton
    private static EntityClassCache instance = null;

    /**
     * private constructor for singleton-use
     */
    private EntityClassCache(){
    }

    /**
     * Static method for getting a singleton page-class-cacher
     * @return a singleton instance of PageClassCache
     * @throws NullPointerException if no application cache could be found
     */
    public static EntityClassCache getInstance() throws CacheException
    {
        try {
            if (instance == null) {
                //if the application-cache doesn't exist, throw exception, else instantiate the application's page-cache with a new empty hashmap
                if (R.cacheManager() != null && R.cacheManager().getApplicationCache() != null) {

                    if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.ENTITY_CLASSES) || !R.cacheManager().getApplicationCache().containsKey(CacheKeys.PAGE_TEMPLATES)) {
                        R.cacheManager().getApplicationCache().put(CacheKeys.ENTITY_CLASSES, new HashMap<String, EntityClass>());
                        instance = new EntityClassCache();
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
     * @param identifiableObject the identifiable object to be added to the applications cache, the key will be the object's id
     */
    @Override
    public void add(EntityClass identifiableObject) throws CacheException
    {
        //TODO: for an entity the super method should be overwritten, so we can add the same EntityClass twice and choose for the 'bleuprint' to be kept in the cache
        super.add(identifiableObject);
    }
    /**
     * This method returns a map with all default page-instances (value) of all present pageClasses (key)
     * @returns a map of all the currently cached page-classes from the application cache
     */
    @Override
    public Map<String, EntityClass> getCache(){
        return (Map<String, EntityClass>) R.cacheManager().getApplicationCache().get(CacheKeys.ENTITY_CLASSES);
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
