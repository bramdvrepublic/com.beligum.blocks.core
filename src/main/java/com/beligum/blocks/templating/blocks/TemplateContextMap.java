package com.beligum.blocks.templating.blocks;

import com.beligum.base.server.R;
import com.beligum.base.server.RequestContext;
import com.beligum.base.templating.ifaces.TemplateContext;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.caching.CacheKeys;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by bram on 5/21/15.
 */
public class TemplateContextMap implements com.beligum.base.templating.ifaces.TemplateContextMap
{
    //-----CONSTANTS-----
    public static final String TAG_TEMPLATE_PROPERTIES_VARIABLE = "property";
    public static final String TAG_TEMPLATE_CONTROLLER_VARIABLE = "controller";
    public static final String TEMPLATE_STACK_VARIABLE = "controllerStack";

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    //needed to auto-instantiate this object
    public TemplateContextMap()
    {
    }

    //-----PUBLIC METHODS-----
    @Override
    public void fillTemplateContext(TemplateContext templateContext)
    {
        templateContext.set(TAG_TEMPLATE_PROPERTIES_VARIABLE, new OverloadedPropertyMap());
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    public static Map<String, TemplateController> getTemplateControllers()
    {
        Map<String, TemplateController> retVal = (Map<String, TemplateController>) R.cacheManager().getApplicationCache().get(CacheKeys.TAG_TEMPLATE_CONTROLLERS);
        if (retVal == null) {
            Map<String, Class<?>> mapping = new HashMap<>();
            TemplateCache cachedTemplates = HtmlParser.getCachedTemplates();
            for (Map.Entry<Path, HtmlTemplate> e : cachedTemplates.entrySet()) {
                HtmlTemplate tagTemplate = e.getValue();
                mapping.put(tagTemplate.getTemplateName(), tagTemplate.getControllerClass());
            }

            R.cacheManager().getApplicationCache().put(CacheKeys.TAG_TEMPLATE_CONTROLLERS, retVal = new RequestLoadedControllerMap(mapping));
        }

        return retVal;
    }
    static class RequestLoadedControllerMap extends HashMap<String, TemplateController>
    {
        private Map<String, Class<?>> mapping;

        public RequestLoadedControllerMap(Map<String, Class<?>> mapping)
        {
            this.mapping = mapping;
        }

        @Override
        public TemplateController get(Object key)
        {
            TemplateController retVal = null;
            if (!RequestContext.getRequestCache().containsKey(key)) {
                if (key!=null) {
                    Class<TemplateController> controllerClass = (Class<TemplateController>) this.mapping.get(key);
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
                retVal = (TemplateController) RequestContext.getRequestCache().get(key);
            }

            return retVal;
        }
    }
    static class OverloadedPropertyMap extends HashMap<String, Object>
    {
    }
}