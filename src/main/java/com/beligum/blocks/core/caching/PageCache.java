package com.beligum.blocks.core.caching;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.models.AbstractIdentifiableElement;
import com.beligum.blocks.core.models.Page;
import com.beligum.blocks.core.parsing.CacheKeys;
import com.beligum.blocks.core.parsing.ElementParser;
import com.beligum.blocks.core.parsing.ElementParserException;
import com.beligum.core.framework.base.R;

import java.io.File;
import java.io.IOException;
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

    //private constructor for singleton-use
    private PageCache(){

    }

    /**
     *
     * @return a singleton instance of PageCache
     * @throws NullPointerException if no application cache could be found
     */
    public static PageCache getInstance() throws IOException, ElementParserException
    {
        if(instance == null){
            //if the application-cache doesn't exist, throw exception, else instantiate the application's page-cache with a new empty hashmap
            if(R.cacheManager() != null && R.cacheManager().getApplicationCache() != null) {
                if (!R.cacheManager().getApplicationCache().contains(CacheKeys.PAGE_CLASSES)) {
                    R.cacheManager().getApplicationCache().put(CacheKeys.PAGE_CLASSES, new HashMap<String, Page>());
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
    public Map<String, Page> getPageCache(){
        return (Map<String, Page>) R.cacheManager().getApplicationCache().get(CacheKeys.PAGE_CLASSES);
    }

    /**
     *
     * @param page the page to be added to the applications page-cache, the key will be the page-id
     */
    public void add(Page page){
        getPageCache().put(page.getPageClass(), page);
    }

    /**
     * Add a template page (starting from the pageclass-name) to the page-cache
     * @param pageClass the filename of the template to be added to the cache
     * @throws IOException
     * @throws ElementParserException
     */
    private void add(String pageClass) throws IOException, ElementParserException
    {
        /*
         * Get the default rows and blocks out of the template and write them to the application cache
         * We get the local file representing the template, using the files.template-path in the configuration-xml-file of the server
         */
        String templateFilename = BlocksConfig.getTemplateFolder() + "/pages/" + pageClass + "index.html";
        File htmlFile = new File(pageClass);
        ElementParser parser = new ElementParser();
        StringBuilder velocityTemplate = new StringBuilder();
        //parse the file to retrieve a set of rows and blocks and a string containing the velocity template of the page
        Set<AbstractIdentifiableElement> rowsAndBlocks = parser.toVelocity(htmlFile, velocityTemplate);

        /*
         * Create a new unique page-object
         */
        String id = Page.getNewUnversionnedID(pageClass);
        while(this.getPageCache().containsKey(id)){
            id = Page.getNewUnversionnedID(pageClass);
        }
        Page page = new Page(id, pageClass, velocityTemplate.toString());

        /*
         * Put all the row and block info into the page-object
         */
        for (AbstractIdentifiableElement rowOrBlock : rowsAndBlocks) {
            //for setting data-version
            long currentTime = System.currentTimeMillis();
            String uID = rowOrBlock.getUid() + ":" + currentTime;
            //add this row or block to the page
            page.addElement(rowOrBlock);
        }

        /*
         * Put the filled page in the cache
         */
        this.add(page);
    }

    public PageCache fillPageCache() throws IOException, ElementParserException
    {
        //TODO BAS: this should fill in the pageCache at startup using .add(pageClass), by running over all subfolders of "/pages"
        add("default");
        return this;
    }
}
