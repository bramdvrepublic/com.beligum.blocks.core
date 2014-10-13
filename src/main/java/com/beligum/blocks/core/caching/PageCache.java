package com.beligum.blocks.core.caching;

import com.beligum.blocks.core.models.AbstractElement;
import com.beligum.blocks.core.models.PageClass;
import com.beligum.blocks.core.parsing.PageParser;
import com.beligum.blocks.core.parsing.PageParserException;
import com.beligum.core.framework.base.R;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by bas on 07.10.14.
 * Singleton for interacting with the applications page-cache, containing pairs of (page-class, default-page-instance)
 */
public class PageCache
{
    //the instance of this singleton
    private static PageCache instance = null;

    /**
     * private constructor for singleton-use
     */
    private PageCache(){
    }

    /**
     * Static method for getting a singleton page-cacher for the specified site
     * @return a singleton instance of PageCache
     * @throws NullPointerException if no application cache could be found
     */
    public static PageCache getInstance() throws IOException, PageParserException
    {
        if(instance == null){
            //if the application-cache doesn't exist, throw exception, else instantiate the application's page-cache with a new empty hashmap
            if(R.cacheManager() != null && R.cacheManager().getApplicationCache() != null) {
                if (!R.cacheManager().getApplicationCache().contains(CacheKeys.PAGE_CLASSES)) {
                    R.cacheManager().getApplicationCache().put(CacheKeys.PAGE_CLASSES, new HashMap<String, PageClass>());
                    instance = new PageCache();
                    instance.fillPageCache();
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
    public Map<String, PageClass> getPageCache(){
        return (Map<String, PageClass>) R.cacheManager().getApplicationCache().get(CacheKeys.PAGE_CLASSES);
    }

    /**
     *
     * @param pageClass the page-class to be added to the applications page-cache, the key will be the page-class-name
     */
    public void add(PageClass pageClass){
        getPageCache().put(pageClass.getName(), pageClass);

        //TODO BAS: remove this DEBUG feature
        System.out.println("IN CACHE:");
        for(AbstractElement element : pageClass.getElements()) {
            System.out.println("\n (" + element.getId() + ", " + element.getContent() + ") \n");
        }
    }

    /**
     * Fill up the page-cache
     * @return
     * @throws IOException
     * @throws PageParserException
     */
    private PageCache fillPageCache() throws IOException, PageParserException
    {
        try{
            //TODO BAS: this should fill in the pageCache at startup using .add(pageClass), by running over all subfolders of "/pages"

            add("default");
        }catch(URISyntaxException e){
            throw new PageParserException("Bad URI encountered while filling page-cache", e);
        }
        return this;
    }

    /**
     * Add a template page (starting from the pageclass-name) to the page-cache
     * @param pageClassName the page-class-name (f.i. "default" for a pageClass filtered from the file "pages/default/index.html") of the page-class-template to be parsed and added to the cache as a couple (pageClassName, pageClass)
     * @throws IOException
     * @throws com.beligum.blocks.core.parsing.PageParserException
     */
    private void  add(String pageClassName) throws IOException, PageParserException, URISyntaxException
    {
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
    }
}
