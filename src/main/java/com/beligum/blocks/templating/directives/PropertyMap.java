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

package com.beligum.blocks.templating.directives;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.templating.HtmlRdfContext;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;

/**
 * Special map that holds property-value mappings where the property name follows the RDF vocabulary rules.
 * Eg. it expands all uses of a property to it's full blown URI before comparing/setting/getting them from the map
 * so we can mix-and-mingle the default vocab, prefixed vocabs or full URIs.
 * WATCH OUT: the default ontology is always the main one and isn't looked up from the <html> @vocab attribute
 * (or any child inside it's body that might have changed the default vocab), see the exception in HtmlRdfContext.pushVocabulary()
 */
public class PropertyMap extends HashMap<String, Object>
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    protected HtmlRdfContext rdfContext;

    //-----CONSTRUCTORS-----
    public PropertyMap(URI sourceUri)
    {
        super();

        this.rdfContext = new HtmlRdfContext(sourceUri);
    }

    //-----PUBLIC METHODS-----
    @Override
    public boolean containsKey(Object key)
    {
        //note: this cast should always work, see the superclass
        return super.containsKey(this.expandKey((String) key));
    }
    @Override
    public Object get(Object key)
    {
        //note: this cast should always work, see the superclass
        return super.get(this.expandKey((String) key));
    }
    @Override
    public Object put(String key, Object value)
    {
        return super.put(this.expandKey(key), value);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    /**
     * Expands the key to it's full blown ontology-prefixed URI counterpart
     */
    private String expandKey(String key)
    {
        String retVal = null;

        try {
            retVal = this.rdfContext.normalizeProperty(key);
        }
        catch (IOException e) {
            Logger.error("Error happened while normalizing property key '" + key + "' in " + this.rdfContext.getSourceUri(), e);
        }

        return retVal;
    }
}
