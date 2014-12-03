package com.beligum.blocks.core.caching;

import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.templates.AbstractTemplate;
import com.beligum.blocks.core.models.templates.PageTemplate;
import com.beligum.core.framework.base.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by bas on 25.11.14.
 */
public class PageTemplateCache extends AbstractTemplatesCache<PageTemplate>
{
    //the instance of this singleton
    private static PageTemplateCache instance = null;

    /**
     * private constructor for singleton-use
     */
    private PageTemplateCache(){

    }

    /**
     * Static method for getting a singleton page-class-cacher
     * @return a singleton instance of PageClassCache
     * @throws NullPointerException if no application cache could be found
     */
    public static PageTemplateCache getInstance() throws CacheException
    {
        try {
            if (instance == null) {
                //if the application-cache doesn't exist, throw exception, else instantiate the application's page-cache with a new empty hashmap
                if (R.cacheManager() != null && R.cacheManager().getApplicationCache() != null) {

                    if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.PAGE_TEMPLATES) || !R.cacheManager().getApplicationCache().containsKey(CacheKeys.PAGE_TEMPLATES)) {
                        R.cacheManager().getApplicationCache().put(CacheKeys.PAGE_TEMPLATES, new HashMap<String, PageTemplate>());
                        instance = new PageTemplateCache();
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
    public PageTemplate get(String name) throws CacheException
    {
        try {
            Map<String, PageTemplate> applicationCache = this.getCache();
            PageTemplate pageTemplate = applicationCache.get(RedisID.renderNewPageTemplateID(name).getUnversionedId());
            return pageTemplate;
            //TODO BAS: This needs a default value to fall back to. How do we define a default? Do we use the framework-default or something?
        }catch(IDException e){
            throw new CacheException("Could not get "+ PageTemplate.class.getSimpleName() + " '" + name + "' from cache.", e);
        }
    }

    /**
     * This method returns a map with all present Cachables (value) by name (key)
     *
     * @returns a map of all the currently cached Cachables from the application cache
     */
    @Override
    protected Map<String, PageTemplate> getCache()
    {
        return (Map<String, PageTemplate>) R.cacheManager().getApplicationCache().get(CacheKeys.PAGE_TEMPLATES);
    }

    @Override
    public Class<? extends AbstractTemplate> getCachedClass()
    {
        return PageTemplate.class;
    }
}
