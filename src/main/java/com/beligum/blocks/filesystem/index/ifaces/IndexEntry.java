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

import com.beligum.blocks.filesystem.index.entries.JsonField;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

/**
 * Created by bram on 2/14/16.
 */
public interface IndexEntry extends Serializable
{
    //-----CONSTANTS-----
    //note: sync these with the getter names below (and the setters of the implementations)
    IndexEntryField id = new JsonField("id")
    {
        @Override
        public String getValue(IndexEntry indexEntry)
        {
            return indexEntry.getId();
        }
        @Override
        public boolean hasValue(IndexEntry indexEntry)
        {
            return indexEntry.hasId();
        }
    };
    IndexEntryField tokenisedId = new JsonField("tokenisedId")
    {
        @Override
        public String getValue(IndexEntry indexEntry)
        {
            //the value is the same, but it should be indexed in a different way
            return indexEntry.getId();
        }
        @Override
        public boolean hasValue(IndexEntry indexEntry)
        {
            return indexEntry.hasId();
        }
    };
    IndexEntryField label = new JsonField("label")
    {
        @Override
        public String getValue(IndexEntry indexEntry)
        {
            return indexEntry.getLabel();
        }
        @Override
        public boolean hasValue(IndexEntry indexEntry)
        {
            return indexEntry.hasLabel();
        }
    };
    IndexEntryField description = new JsonField("description")
    {
        @Override
        public String getValue(IndexEntry indexEntry)
        {
            return indexEntry.getDescription();
        }
        @Override
        public boolean hasValue(IndexEntry indexEntry)
        {
            return indexEntry.hasDescription();
        }
    };
    IndexEntryField image = new JsonField("image")
    {
        @Override
        public String getValue(IndexEntry indexEntry)
        {
            return indexEntry.getImage();
        }
        @Override
        public boolean hasValue(IndexEntry indexEntry)
        {
            return indexEntry.hasImage();
        }
    };

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    /**
     * The unique ID of this entry. Eg. for a page, this is the public (relative) URI.
     * For resources, the more unique, the better, so often the real client URL is used (instead of the linked, auto-generated resource-URI)
     */
    String getId();

    /**
     * Returns true if this value has been set once (even though it might be null)
     */
    @JsonIgnore
    boolean hasId();

    /**
     * The label of this resource, to be used directly in the HTML that is returned to the client.
     * So, in the right language and format. Mainly used to build eg. search result lists.
     * Try not to return null or "".
     */
    String getLabel();

    /**
     * Returns true if this value has been set once (even though it might be null)
     */
    @JsonIgnore
    boolean hasLabel();

    /**
     * The description of this resource, to be used directly in the HTML that is returned to the client.
     * So, in the right language and format. Mainly used to build eg. search result lists.
     * Might be empty or null.
     */
    String getDescription();

    /**
     * Returns true if this value has been set once (even though it might be null)
     */
    @JsonIgnore
    boolean hasDescription();

    /**
     * A link to the main image that describes this resource, mainly used to build eg. search result lists.
     * Might be null.
     */
    String getImage();

    /**
     * Returns true if this value has been set once (even though it might be null)
     */
    @JsonIgnore
    boolean hasImage();

    /**
     * This should return the list of internal fields, that will be added to the public RDF fields, in order
     * to make this entry function.
     */
    @JsonIgnore
    Iterable<IndexEntryField> getInternalFields();

    /**
     * This is a generic getter to get the value associated with the internal field key
     */
    @JsonIgnore
    String getFieldValue(IndexEntryField field);

}
