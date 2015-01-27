package com.beligum.blocks.core.caching;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.redis.templates.AbstractTemplate;
import com.beligum.blocks.core.models.redis.templates.EntityTemplateClass;
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
                    R.cacheManager().getApplicationCache().put(CacheKeys.ENTITY_CLASSES, new HashMap<String, EntityTemplateClass>());
                    instance = new EntityTemplateClassCache();
                    //insert most basic possible entity-template-class, it is not saved to db
                    EntityTemplateClass entityTemplateClass = new EntityTemplateClass(instance.getDefaultTemplateName(), BlocksConfig.getDefaultLanguage(), "<div " + ParserConstants.TYPE_OF + "=\"" + ParserConstants.DEFAULT_ENTITY_TEMPLATE_CLASS + "\" "+ ParserConstants.CAN_EDIT +"></div>", ParserConstants.DEFAULT_PAGE_TEMPLATE, null, null);
                    instance.getCache().put(instance.getTemplateKey(instance.getDefaultTemplateName()), entityTemplateClass);
                    instance.fillCache();
                }
                else {
                    throw new NullPointerException("No application-cache found.");
                }
            }
            return instance;
        }
        catch(Exception e){
            throw new CacheException("Couldn't initialize entity-class-cache.", e);
        }
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

    @Override
    protected String getTemplateKey(String templateName) throws IDException
    {
        return RedisID.renderUnversionedEntityTemplateClassID(templateName);
    }

    @Override
    protected String getDefaultTemplateName()
    {
        return ParserConstants.DEFAULT_ENTITY_TEMPLATE_CLASS;
    }
}
