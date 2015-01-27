package com.beligum.blocks.core.caching;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.templates.AbstractTemplate;
import com.beligum.blocks.core.models.templates.PageTemplate;
import com.beligum.core.framework.base.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
                if (R.cacheManager() != null && R.cacheManager().getApplicationCache() != null) {R.cacheManager().getApplicationCache().put(CacheKeys.PAGE_TEMPLATES, new HashMap<String, PageTemplate>());
                    instance = new PageTemplateCache();
                    //insert the most basic possible page-template, for fall-back reasons: uses bootstrap
                    List<String> links = new ArrayList<>();
                    List<String> scripts = new ArrayList<>();
                    links.add("<link href=\"" + BlocksConfig.BOOSTRAP_CSS_FILEPATH + "\" rel=\"stylesheet\" />");
                    scripts.add("<script src=\"" + BlocksConfig.BOOTSTRAP_JS_FILEPATH + "\"></script>");
                    //Note: do not remove the comment-tag in the definition of the default page-template. The head should not be empty, if not exceptions will occur when parsing it.
                    PageTemplate pageTemplate = new PageTemplate(instance.getDefaultTemplateName(), BlocksConfig.getDefaultLanguage(), "<!DOCTYPE html>\n" +
                                                                                                    "<html>\n" +
                                                                                                    "<head>\n" +
                                                                                                    "<!--This is a rendered default page-template. If you want to use another page-template, you should overwrite it (template=\"default\").-->\n" +
                                                                                                    "</head>\n" +
                                                                                                    "<body>\n" +
                                                                                                    "<div class=\"container\">\n" +
                                                                                                    //default referencing div
                                                                                                    "<div " + ParserConstants.PAGE_TEMPLATE_CONTENT_ATTR + "=\"\" " + ParserConstants.REFERENCE_TO + "=\""+ParserConstants.PAGE_TEMPLATE_ENTITY_VARIABLE_NAME + "\"></div>\n" +
                                                                                                    "</div>\n" +
                                                                                                    "</body>\n" +
                                                                                                    "</html>\n", links, scripts);
                    instance.getCache().put(instance.getTemplateKey(instance.getDefaultTemplateName()), pageTemplate);
                    instance.fillCache();
                }
                else {
                    throw new NullPointerException("No application-cache found.");
                }
            }
            return instance;
        }
        catch(Exception e){
            throw new CacheException("Couldn't initialize page-template-cache.", e);
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
     * This method returns a map with all present Cachables (value) by name (key)
     *
     * @return a map of all the currently cached Cachables from the application cache
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

    @Override
    protected String getTemplateKey(String templateName) throws IDException
    {
        return RedisID.renderUnversionedPageTemplateID(templateName);
    }

    @Override
    protected String getDefaultTemplateName()
    {
        return ParserConstants.DEFAULT_PAGE_TEMPLATE;
    }
}
