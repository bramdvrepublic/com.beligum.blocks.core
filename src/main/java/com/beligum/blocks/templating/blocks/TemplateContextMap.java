/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.beligum.blocks.templating.blocks;

import com.beligum.base.server.R;
import com.beligum.base.templating.ifaces.TemplateContext;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.templating.blocks.directives.PropertyMap;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by bram on 5/21/15.
 */
public class TemplateContextMap implements com.beligum.base.templating.ifaces.TemplateContextMap
{
    //-----CONSTANTS-----
    public static final String TAG_TEMPLATE_PROPERTIES_VARIABLE = "PROPERTY";
    public static final String TAG_TEMPLATE_TEMPLATE_VARIABLE = "TEMPLATE";
    public static final String TAG_TEMPLATE_CONTROLLER_VARIABLE = "CONTROLLER";
    public static final String TEMPLATE_STACK_VARIABLE = "CONTROLLER_STACK";

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
        templateContext.set(TAG_TEMPLATE_PROPERTIES_VARIABLE, new PropertyMap());
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    public static Map<String, TemplateController> getTemplateControllers()
    {
        Map<String, TemplateController> retVal = (Map<String, TemplateController>) R.cacheManager().getApplicationCache().get(CacheKeys.TAG_TEMPLATE_CONTROLLERS);
        if (retVal == null) {
            Map<String, Class<?>> mapping = new HashMap<>();
            TemplateCache cachedTemplates = TemplateCache.instance();
            for (HtmlTemplate tagTemplate : cachedTemplates.values()) {
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
            if (!R.requestContext().getRequestCache().containsKey(key)) {
                if (key != null) {
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

                R.requestContext().getRequestCache().put(key, retVal);
            }
            else {
                retVal = (TemplateController) R.requestContext().getRequestCache().get(key);
            }

            return retVal;
        }
    }
}
