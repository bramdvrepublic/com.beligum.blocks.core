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

package com.beligum.blocks.filesystem.index.ifaces;

import java.io.Serializable;

/**
 * Created by bram on 2/14/16.
 */
public interface IndexEntry extends Serializable
{
    //-----CONSTANTS-----
    interface IndexEntryField
    {
        /**
         * This value is used to index "core" Fields (eg. the ones explicitly implementing IndexEntryField) to support
         * an easy means to search for null fields (eg. search for all fields NOT having this field). Analogue to:
         * https://www.elastic.co/guide/en/elasticsearch/reference/2.1/null-value.html
         */
        String NULL_VALUE = "NULL";

        //since all implementations are enums, this will be implemented by the default enum name() method
        String name();
    }
    enum Field implements IndexEntryField
    {
        id,
        tokenisedId,
        label,
        description,
        image,
    }

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----

    /**
     * The unique ID of this entry. Eg. for a page, this is the public (relative) URI.
     * For resources, the more unique, the better, so often the real client URL is used (instead of the linked, auto-generated resource-URI)
     */
    String getId();

    /**
     * The label of this resource, to be used directly in the HTML that is returned to the client.
     * So, in the right language and format. Mainly used to build eg. search result lists.
     * Try not to return null or "".
     */
    String getLabel();

    /**
     * The description of this resource, to be used directly in the HTML that is returned to the client.
     * So, in the right language and format. Mainly used to build eg. search result lists.
     * Might be empty or null.
     */
    String getDescription();

    /**
     * A link to the main image that describes this resource, mainly used to build eg. search result lists.
     * Might be null.
     */
    String getImage();

}
