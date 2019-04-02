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

package com.beligum.blocks.filesystem.index.entries;

import com.beligum.blocks.filesystem.index.ifaces.IndexEntry;
import com.beligum.blocks.filesystem.index.ifaces.IndexEntryField;
import com.google.common.collect.Sets;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;

/**
 * Simple implementation that provides accessors to all the required fields and offers a basic equals() implementation
 *
 * Created by bram on 2/14/16.
 */
public abstract class AbstractIndexEntry implements IndexEntry
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    //Notes:
    // - the underscore is to differentiate this field from the constant field definitions in IndexEntry
    // - the booleans is to detect unset fields while supporting null values
    protected String _id;
    protected boolean hasId;
    protected String _label;
    protected boolean hasLabel;
    protected String _description;
    protected boolean hasDescription;
    protected String _image;
    protected boolean hasImage;

    //-----CONSTRUCTORS-----
    protected AbstractIndexEntry()
    {
    }
    protected AbstractIndexEntry(String id)
    {
        this.setId(id);
    }

    //-----PUBLIC METHODS-----

    @Override
    public String getId()
    {
        return _id;
    }
    @Override
    public boolean hasId()
    {
        return hasId;
    }
    @Override
    public String getLabel()
    {
        return _label;
    }
    @Override
    public boolean hasLabel()
    {
        return hasLabel;
    }
    @Override
    public String getDescription()
    {
        return _description;
    }
    @Override
    public boolean hasDescription()
    {
        return hasDescription;
    }
    @Override
    public String getImage()
    {
        return _image;
    }
    @Override
    public boolean hasImage()
    {
        return hasImage;
    }
    @Override
    public String getFieldValue(IndexEntryField field)
    {
        return field.getValue(this);
    }

    //-----PROTECTED METHODS-----
    protected void setId(String id)
    {
        this._id = id;
        this.hasId = true;
    }
    protected void setLabel(String label)
    {
        this._label = label;
        this.hasLabel = true;
    }
    protected void setDescription(String description)
    {
        this._description = description;
        this.hasDescription = true;
    }
    protected void setImage(String image)
    {
        this._image = image;
        this.hasImage = true;
    }

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----
    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (!(o instanceof AbstractIndexEntry))
            return false;

        AbstractIndexEntry that = (AbstractIndexEntry) o;

        return getId() != null ? getId().equals(that.getId()) : that.getId() == null;

    }
    @Override
    public int hashCode()
    {
        return getId() != null ? getId().hashCode() : 0;
    }
}
