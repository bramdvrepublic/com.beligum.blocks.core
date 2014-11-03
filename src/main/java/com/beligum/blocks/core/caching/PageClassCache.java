package com.beligum.blocks.core.caching;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.models.PageClass;
import com.beligum.blocks.core.parsing.CachableClassParser;
import com.beligum.blocks.core.parsing.PageParser;
import com.beligum.core.framework.base.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by bas on 07.10.14.
 * Singleton for interacting with the applications page-cache, containing pairs of (page-class, default-page-instance)
 */
public class PageClassCache extends AbstractCachableClassCache<PageClass>
{
    //the instance of this singleton
    private static PageClassCache instance = null;

    /**
     * private constructor for singleton-use
     */
    private PageClassCache(){
    }

    /**
     * Static method for getting a singleton page-class-cacher
     * @return a singleton instance of PageClassCache
     * @throws NullPointerException if no application cache could be found
     */
    public static PageClassCache getInstance() throws CacheException
    {
        try {
            if (instance == null) {
                //if the application-cache doesn't exist, throw exception, else instantiate the application's page-cache with a new empty hashmap
                if (R.cacheManager() != null && R.cacheManager().getApplicationCache() != null) {
                    if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.PAGE_CLASSES)) {
                        R.cacheManager().getApplicationCache().put(CacheKeys.PAGE_CLASSES, new HashMap<String, PageClass>());
                        instance = new PageClassCache();
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
            throw new CacheException("Couldn't initialize page-class-cache.", e);
        }
    }

    /**
     * This method returns a map with all default page-instances (value) of all present pageClasses (key)
     * @returns a map of all the currently cached page-classes from the application cache
     */
    @Override
    public Map<String, PageClass> getCache(){
        return (Map<String, PageClass>) R.cacheManager().getApplicationCache().get(CacheKeys.PAGE_CLASSES);
    }

    @Override
    protected String getClassRootFolder()
    {
        return BlocksConfig.getPagesFolder();
    }

    @Override
    protected CachableClassParser<PageClass> getParser()
    {
        return new PageParser();
    }
}
