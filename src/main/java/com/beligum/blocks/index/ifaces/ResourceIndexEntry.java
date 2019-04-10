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

package com.beligum.blocks.index.ifaces;

import com.beligum.blocks.index.entries.AbstractIndexEntry;
import com.beligum.blocks.index.entries.JsonField;

/**
 * This is the general superclass for all entries in any kind of index.
 * Note that this super interface (compared to PageIndexEntry) mainly exists to support more than page searching later on (eg. media metadata indexing/searching)
 *
 * Created by bram on 2/14/16.
 */
public interface ResourceIndexEntry extends ResourceProxy
{
    //-----CONSTANTS-----
    //note: sync these with the getter names below (and the setters of the implementations)
    IndexEntryField uriField = new JsonField("uri")
    {
        @Override
        public String getValue(ResourceIndexEntry indexEntry)
        {
            return indexEntry.getUri();
        }
        @Override
        public boolean hasValue(ResourceIndexEntry indexEntry)
        {
            return ((AbstractIndexEntry)indexEntry).hasUri();
        }
        @Override
        public void setValue(ResourceIndexEntry indexEntry, String value)
        {
            ((AbstractIndexEntry)indexEntry).setUri(value);
        }
    };
    IndexEntryField tokenisedUriField = new JsonField("tokenisedUri")
    {
        @Override
        public String getValue(ResourceIndexEntry indexEntry)
        {
            //the value is the same, but it should be indexed in a different way
            return indexEntry.getUri();
        }
        @Override
        public boolean hasValue(ResourceIndexEntry indexEntry)
        {
            return ((AbstractIndexEntry)indexEntry).hasUri();
        }
        @Override
        public void setValue(ResourceIndexEntry indexEntry, String value)
        {
            //NOOP this is a virtual field, it's value is set with setId()
        }
    };
    IndexEntryField resourceField = new JsonField("resource")
    {
        @Override
        public String getValue(ResourceIndexEntry indexEntry)
        {
            return indexEntry.getResource();
        }
        @Override
        public boolean hasValue(ResourceIndexEntry indexEntry)
        {
            return ((AbstractIndexEntry)indexEntry).hasResource();
        }
        @Override
        public void setValue(ResourceIndexEntry indexEntry, String value)
        {
            ((AbstractIndexEntry)indexEntry).setResource(value);
        }
    };
    IndexEntryField typeOfField = new JsonField("typeOf")
    {
        @Override
        public String getValue(ResourceIndexEntry indexEntry)
        {
            return indexEntry.getTypeOf();
        }
        @Override
        public boolean hasValue(ResourceIndexEntry indexEntry)
        {
            return ((AbstractIndexEntry)indexEntry).hasTypeOf();
        }
        @Override
        public void setValue(ResourceIndexEntry indexEntry, String value)
        {
            ((AbstractIndexEntry)indexEntry).setTypeOf(value);
        }
    };
    IndexEntryField languageField = new JsonField("language")
    {
        @Override
        public String getValue(ResourceIndexEntry indexEntry)
        {
            return indexEntry.getLanguage();
        }
        @Override
        public boolean hasValue(ResourceIndexEntry indexEntry)
        {
            return ((AbstractIndexEntry)indexEntry).hasLanguage();
        }
        @Override
        public void setValue(ResourceIndexEntry indexEntry, String value)
        {
            ((AbstractIndexEntry)indexEntry).setLanguage(value);
        }
    };
    IndexEntryField labelField = new JsonField("label")
    {
        @Override
        public String getValue(ResourceIndexEntry indexEntry)
        {
            return indexEntry.getLabel();
        }
        @Override
        public boolean hasValue(ResourceIndexEntry indexEntry)
        {
            return ((AbstractIndexEntry)indexEntry).hasLabel();
        }
        @Override
        public void setValue(ResourceIndexEntry indexEntry, String value)
        {
            ((AbstractIndexEntry)indexEntry).setLabel(value);
        }
    };
    IndexEntryField descriptionField = new JsonField("description")
    {
        @Override
        public String getValue(ResourceIndexEntry indexEntry)
        {
            return indexEntry.getDescription();
        }
        @Override
        public boolean hasValue(ResourceIndexEntry indexEntry)
        {
            return ((AbstractIndexEntry)indexEntry).hasDescription();
        }
        @Override
        public void setValue(ResourceIndexEntry indexEntry, String value)
        {
            ((AbstractIndexEntry)indexEntry).setDescription(value);
        }
    };
    IndexEntryField imageField = new JsonField("image")
    {
        @Override
        public String getValue(ResourceIndexEntry indexEntry)
        {
            return indexEntry.getImage();
        }
        @Override
        public boolean hasValue(ResourceIndexEntry indexEntry)
        {
            return ((AbstractIndexEntry)indexEntry).hasImage();
        }
        @Override
        public void setValue(ResourceIndexEntry indexEntry, String value)
        {
            ((AbstractIndexEntry)indexEntry).setImage(value);
        }
    };

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----

}
