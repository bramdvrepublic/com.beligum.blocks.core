package com.beligum.blocks.templating.blocks;

import com.beligum.base.server.R;
import com.beligum.base.server.RequestContext;
import com.beligum.base.templating.ifaces.TemplateContext;
import com.beligum.base.templating.ifaces.TemplateContextMap;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.caching.CacheKeys;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by bram on 5/21/15.
 */
public class TagTemplateContextMap implements TemplateContextMap
{
    //-----CONSTANTS-----
    public static final String TAG_TEMPLATE_CONTROLLERS_VARIABLE = "templateControllers";

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    //needed to auto-instantiate this object
    public TagTemplateContextMap()
    {
    }

    //-----PUBLIC METHODS-----
    @Override
    public void fillTemplateContext(TemplateContext templateContext)
    {
        templateContext.set(TAG_TEMPLATE_CONTROLLERS_VARIABLE, this.getTemplateControllers());
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    public Map<String, Object> getTemplateControllers()
    {
        Map<String, Object> retVal = (Map<String, Object>) R.cacheManager().getApplicationCache().get(CacheKeys.TAG_TEMPLATE_CONTROLLERS);
        if (retVal == null) {
            retVal = new LazyLoadedControllerMap();
            TagTemplateCache cachedTemplates = HtmlParser.getCachedTemplates();
            for (Map.Entry<Path, TagTemplate> e : cachedTemplates.entrySet()) {
                TagTemplate tagTemplate = e.getValue();
                retVal.put(tagTemplate.getTemplateName(), tagTemplate.getControllerClass());
            }

            R.cacheManager().getApplicationCache().put(CacheKeys.TAG_TEMPLATE_CONTROLLERS, retVal);
        }

        return retVal;
    }
    class LazyLoadedControllerMap extends HashMap<String, Object>
    {
        @Override
        public Object get(Object key)
        {
            Object retVal = null;
            if (!RequestContext.getRequestCache().containsKey(key)) {
                if (key!=null) {
                    Class controllerClass = (Class) super.get(key);
                    if (controllerClass != null) {
                        try {
                            retVal = controllerClass.newInstance();
                        }
                        catch (Exception e) {
                            Logger.error("Error while instantiating tag template controller class " + controllerClass, e);
                        }
                    }
                }

                RequestContext.getRequestCache().put(key, retVal);
            }
            else {
                retVal = RequestContext.getRequestCache().get(key);
            }

            return retVal;
        }
    }
}
