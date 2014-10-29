package com.beligum.blocks.core.caching;

import com.beligum.blocks.core.exceptions.PageClassCacheException;
import com.beligum.blocks.core.models.PageClass;
import com.beligum.blocks.core.parsing.PageParser;
import com.beligum.core.framework.base.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by bas on 07.10.14.
 * Singleton for interacting with the applications page-cache, containing pairs of (page-class, default-page-instance)
 */
public class PageClassCache
{
    //the instance of this singleton
    private static PageClassCache instance = null;

    /**
     * private constructor for singleton-use
     */
    private PageClassCache(){
    }

    /**
     * Static method for getting a singleton page-cacher for the specified site
     * @return a singleton instance of PageClassCache
     * @throws NullPointerException if no application cache could be found
     */
    public static PageClassCache getInstance() throws PageClassCacheException
    {
        if(instance == null){
            //if the application-cache doesn't exist, throw exception, else instantiate the application's page-cache with a new empty hashmap
            if(R.cacheManager() != null && R.cacheManager().getApplicationCache() != null) {
                if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.PAGE_CLASSES)) {
                    R.cacheManager().getApplicationCache().put(CacheKeys.PAGE_CLASSES, new HashMap<String, PageClass>());
                    instance = new PageClassCache();
                    instance.fillPageClassCache();
                }
            }
            else{
                throw new NullPointerException("No application-cache found.");
            }
        }
        return instance;
    }

    /**
     * This method returns a map with all default page-instances (value) of all present pageClasses (key)
     * @returns a map of all the currently cached page-classes from the application cache
     */
    public Map<String, PageClass> getPageClassCache(){
        return (Map<String, PageClass>) R.cacheManager().getApplicationCache().get(CacheKeys.PAGE_CLASSES);
    }

    /**
     * Get the page-class with a certain name from the application cache
     * @param pageClassName the name of the page-class to get
     * @return a PageClass-object from the application cache
     */
    public PageClass get(String pageClassName){
        return this.getPageClassCache().get(pageClassName);
    }

    /**
     *
     * @param pageClass the page-class to be added to the applications page-cache, the key will be the page-class-name
     */
    public void add(PageClass pageClass)
    {
        getPageClassCache().put(pageClass.getName(), pageClass);
    }

    /**
     * Fill up the page-cache
     * @return
     * @throws
     */
    private PageClassCache fillPageClassCache() throws PageClassCacheException
    {
        //TODO BAS: this should fill in the pageClassCache at startup using .add(pageClass), by running over all subfolders of "/pages"
        add("default");
        return this;
    }

    /**
     * Add a template page (starting from the pageclass-name) to the page-cache
     * @param pageClassName the page-class-name (f.i. "default" for a pageClass filtered from the file "pages/default/index.html") of the page-class-template to be parsed and added to the cache as a couple (pageClassName, pageClass)
     */
    private void  add(String pageClassName) throws PageClassCacheException
    {
        try {
        /*
         * Get the default rows and blocks out of the template and write them to the application cache
         * We get the local file representing the template, using the files.template-path in the configuration-xml-file of the server
         */
            PageParser parser = new PageParser();
            PageClass pageClass = parser.parsePageClass(pageClassName);

        /*
         * Put the filled page in the cache
         */
            this.add(pageClass);
        }catch(Exception e){
            throw new PageClassCacheException("Could not add page-class '" + pageClassName + "' to the cache.", e);
        }
    }
}
