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

package com.beligum.blocks.rdf.ifaces;

import com.beligum.blocks.config.InputType;
import com.beligum.blocks.config.InputTypeAdapter;
import com.beligum.blocks.config.InputTypeConfig;
import com.beligum.blocks.endpoints.ifaces.RdfQueryEndpoint;
import com.beligum.blocks.filesystem.index.entries.RdfIndexer;
import org.eclipse.rdf4j.model.Value;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;

/**
 * Created by bram on 2/26/16.
 */
public interface RdfProperty extends RdfClass
{
    /**
     * The full datatype (can also be XSD) of this property. This is used by the client side code, together with the WidgetType (see below),
     * to instance an appropriate input method and validation for entering a value for this property.
     * Eg. http://www.w3.org/2001/XMLSchema#integer
     */
    RdfClass getDataType();

    /**
     * This widget-type to be used in the admin sidebar (or just inline, eg. in the case of the editor)
     * to enter a value for an instance of this property.
     * Eg. InlineEditor
     *
     * Note: we serialize this (eg. to JS client side) as it's constant, so we can easily check it's value client side
     */
    @XmlJavaTypeAdapter(InputTypeAdapter.class)
    InputType getWidgetType();

    /**
     * A map of key/value entries that contain specific settings for the input widget type
     */
    InputTypeConfig getWidgetConfig();

    /**
     * A map of key/value entries that contain specific settings for the input widget type
     */
    void setWidgetConfig(InputTypeConfig config);

    /**
     * An instance (eg. for enums) of an endpoint to use while looking up possible values of this property.
     */
    void setEndpoint(RdfQueryEndpoint endpoint);

    /**
     * This method gets called when this property is being indexed by our custom (currently only Lucene) indexer.
     * It should call the right method on the indexer to index the property value as closely as possible.
     * @return the value-object as it was indexed
     */
    RdfIndexer.IndexResult indexValue(RdfIndexer indexer, URI resource, Value value, Locale language, RdfQueryEndpoint.SearchOption... options) throws IOException;

    /**
     * Converts the supplied value to an object to be used during index lookups
     */
    Object prepareIndexValue(String value, Locale language) throws IOException;
}
