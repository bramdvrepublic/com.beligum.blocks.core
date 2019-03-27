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

import com.beligum.blocks.filesystem.index.entries.IndexEntryFieldImpl;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

/**
 * Created by bram on 2/14/16.
 */
public interface IndexEntry extends Serializable
{
    //-----CONSTANTS-----
    //note: sync these with the getter names below (and the setters of the implementations)
    IndexEntryField id = new IndexEntryFieldImpl("id")
    {
        @Override
        public String getValue(IndexEntry indexEntry)
        {
            return indexEntry.getId();
        }
    };
    IndexEntryField tokenisedId = new IndexEntryFieldImpl("tokenisedId")
    {
        @Override
        public String getValue(IndexEntry indexEntry)
        {
            //is this okay? The value of this field is basically also the id, right?
            return indexEntry.getId();
        }
    };
    IndexEntryField label = new IndexEntryFieldImpl("label")
    {
        @Override
        public String getValue(IndexEntry indexEntry)
        {
            return indexEntry.getLabel();
        }
    };
    IndexEntryField description = new IndexEntryFieldImpl("description")
    {
        @Override
        public String getValue(IndexEntry indexEntry)
        {
            return indexEntry.getDescription();
        }
    };
    IndexEntryField image = new IndexEntryFieldImpl("image")
    {
        @Override
        public String getValue(IndexEntry indexEntry)
        {
            return indexEntry.getImage();
        }
    };

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----

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
