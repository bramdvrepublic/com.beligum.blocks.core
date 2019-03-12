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

package com.beligum.blocks.templating.blocks.directives;

import com.beligum.blocks.config.Settings;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;

/**
 * Special map that holds property-value mappings where the property name follows the RDF vocabulary rules
 */
public class PropertyMap extends HashMap<String, Object>
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public PropertyMap()
    {
        super();
    }

    //-----PUBLIC METHODS-----
    @Override
    public boolean containsKey(Object key)
    {
        return super.containsKey(this.expandKey(key));
    }
    @Override
    public Object get(Object key)
    {
        return super.get(this.expandKey(key));
    }
    @Override
    public Object put(String key, Object value)
    {
        return super.put(this.expandKey(key), value);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private String expandKey(String key)
    {
        Object retVal = this.expandKey((Object)key);
        return retVal == null ? null : retVal.toString();
    }
    private Object expandKey(Object key)
    {
        Object retVal = key;

        if (key != null) {
            String keyStr = key.toString();
            if (!StringUtils.isEmpty(keyStr)) {
                if (!keyStr.contains(":")) {
                    String prefixUri = Settings.instance().getRdfMainOntologyNamespace().getUri().toString();
                    if (!prefixUri.endsWith("/")) {
                        prefixUri += "/";
                    }
                    String suffix = keyStr;
                    while (suffix.startsWith("/")) {
                        suffix = suffix.substring(1);
                    }
                    retVal = prefixUri + suffix;
                }
            }
        }

        return retVal;
    }
}
