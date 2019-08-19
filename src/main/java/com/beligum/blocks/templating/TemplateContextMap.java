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

package com.beligum.blocks.templating;

import com.beligum.base.cache.CacheFunction;
import com.beligum.base.cache.CacheKey;
import com.beligum.base.cache.CacheKeyString;
import com.beligum.base.server.R;
import com.beligum.base.templating.ifaces.TemplateContext;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.templating.directives.PropertyMap;

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
        templateContext.set(TAG_TEMPLATE_PROPERTIES_VARIABLE, new PropertyMap(templateContext.getTemplate().getUri()));
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    public static Map<String, TemplateController> getTemplateControllers()
    {
        return R.cacheManager().getApplicationCache().getAndInitIfAbsent(CacheKeys.TAG_TEMPLATE_CONTROLLERS, new CacheFunction<CacheKey, Map<String, TemplateController>>()
        {
            @Override
            public Map<String, TemplateController> apply(CacheKey cacheKey)
            {
                Map<String, Class<?>> mapping = new HashMap<>();
                TemplateCache cachedTemplates = TemplateCache.instance();
                for (HtmlTemplate tagTemplate : cachedTemplates.getAllTemplates()) {
                    if (tagTemplate.getControllerClass() != null) {
                        mapping.put(tagTemplate.getTemplateName(), tagTemplate.getControllerClass());
                    }
                }

                return new RequestLoadedControllerMap(mapping);
            }
        });
    }
    static class RequestLoadedControllerMap extends HashMap<String, TemplateController>
    {
        private Map<String, Class<?>> mapping;

        public RequestLoadedControllerMap(Map<String, Class<?>> mapping)
        {
            this.mapping = mapping;
        }

        @Override
        public TemplateController get(Object objKey)
        {
            TemplateController retVal = null;

            final CacheKey key = objKey == null ? null : new CacheKeyString(String.valueOf(objKey));

            if (key != null) {
                retVal = R.requestManager().getCurrentRequest().getRequestCache().getAndInitIfAbsent(key, new CacheFunction<CacheKey, TemplateController>()
                {
                    @Override
                    public TemplateController apply(CacheKey cacheKey)
                    {
                        TemplateController retVal = null;

                        // Note: don't use key, but objKey instead because we store the controllers based on the name of their tag name
                        // and we wrap it above to be able to use the cache interface
                        Class<TemplateController> controllerClass = (Class<TemplateController>) mapping.get(objKey);
                        if (controllerClass != null) {
                            try {
                                retVal = controllerClass.newInstance();
                            }
                            catch (Exception e) {
                                Logger.error("Error while instantiating tag template controller class " + controllerClass, e);
                            }
                        }

                        return retVal;
                    }
                });
            }

            return retVal;
        }
    }
}
