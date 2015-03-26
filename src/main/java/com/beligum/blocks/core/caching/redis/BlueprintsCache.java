package com.beligum.blocks.core.caching.redis;

import com.beligum.blocks.core.base.Blocks;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.identifiers.BlocksID;
import com.beligum.blocks.core.models.redis.templates.AbstractTemplate;
import com.beligum.blocks.core.models.redis.templates.Blueprint;
import com.beligum.core.framework.base.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bas on 07.10.14.
 * Singleton for interacting with the applications blueprint-cache, containing pairs of (blueprint-type, blueprint)
 */
public class BlueprintsCache extends AbstractTemplatesCache<Blueprint>
{
    //the instance of this singleton
    private static BlueprintsCache instance = null;

    /**
     * private constructor for singleton-use
     */
    private BlueprintsCache(){
    }

    /**
     * Static method for getting a singleton blueprint-cacher
     * @return a singleton instance of PageClassCache
     * @throws NullPointerException if no application cache could be found
     */
    synchronized public static BlueprintsCache getInstance() throws Exception
    {
        if (instance == null) {
            //if the application-cache doesn't exist, throw exception, else instantiate the application's page-cache with a new empty hashmap
            if (R.cacheManager() != null && R.cacheManager().getApplicationCache() != null) {
                //TODO: make sure that CacheKeys.BLUEPRINTS isn't deleted by ApplicationCacher
                R.cacheManager().getApplicationCache().put(CacheKeys.BLUEPRINTS, new HashMap<String, Blueprint>());
                instance = new BlueprintsCache();
                //insert most basic possible blueprint, it is not saved to db
                Blueprint
                                blueprint = new Blueprint(instance.getDefaultTemplateName(), Blocks.config().getDefaultLanguage(), "<div " + ParserConstants.BLUEPRINT + "=\"" + ParserConstants.DEFAULT_BLUEPRINT + "\" "+ ParserConstants.CAN_EDIT_PROPERTY +"=\"\"></div>", ParserConstants.DEFAULT_PAGE_TEMPLATE, null, null);
                instance.getCache().put(instance.getTemplateKey(instance.getDefaultTemplateName()), blueprint);
                instance.fillCache();
            }
            else {
                throw new NullPointerException("No application-cache found.");
            }
        }
        return instance;
    }


    /**
     * reset this application-cache, trashing all it's content
     */
    @Override
    public void reset()
    {
        this.instance = null;
    }

    /**
     * This method returns a map with all default page-instances (value) of all present pageClasses (key)
     * @return a map of all the currently cached page-classes from the application cache
     */
    @Override
    protected Map<String, Blueprint> getCache(){
        return (Map<String, Blueprint>) R.cacheManager().getApplicationCache().get(CacheKeys.BLUEPRINTS);
    }

    /**
     * @return the object-class being stored in this cache
     */
    @Override
    public Class<? extends AbstractTemplate> getCachedClass()
    {
        return Blueprint.class;
    }

    @Override
    protected String getTemplateKey(String templateName) throws IDException
    {
        return BlocksID.renderUnversionedEntityTemplateClassID(templateName);
    }

    @Override
    protected String getDefaultTemplateName()
    {
        return ParserConstants.DEFAULT_BLUEPRINT;
    }

    public List<Blueprint> getPageClasses(){
        List<Blueprint> blueprints = this.values();
        List<Blueprint> pageClasses = new ArrayList<>();
        for (Blueprint blueprint : blueprints) {
            if (blueprint.isPageBlock()) {
                pageClasses.add(blueprint);
            }
        }
        return pageClasses;
    }

    public List<Blueprint> getAddableClasses(){
        List<Blueprint> blueprints = this.values();
        List<Blueprint> addableClasses = new ArrayList<>();
        for (Blueprint blueprint : blueprints) {
            if (blueprint.isAddableBlock()) {
                addableClasses.add(blueprint);
            }
        }
        return addableClasses;
    }
}
