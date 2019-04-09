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

package com.beligum.blocks.index.entries;

import com.beligum.blocks.index.ifaces.IndexEntry;
import com.beligum.blocks.index.ifaces.IndexEntryField;

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
    // - the booleans is to detect unset fields while supporting null values
    protected String id;
    protected boolean hasId;
    protected String label;
    protected boolean hasLabel;
    protected String description;
    protected boolean hasDescription;
    protected String image;
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
        return id;
    }
    @Override
    public boolean hasId()
    {
        return hasId;
    }
    @Override
    public void setId(String id)
    {
        this.id = id;
        this.hasId = true;
    }
    @Override
    public String getLabel()
    {
        return label;
    }
    @Override
    public boolean hasLabel()
    {
        return hasLabel;
    }
    @Override
    public void setLabel(String label)
    {
        this.label = label;
        this.hasLabel = true;
    }
    @Override
    public String getDescription()
    {
        return description;
    }
    @Override
    public boolean hasDescription()
    {
        return hasDescription;
    }
    @Override
    public void setDescription(String description)
    {
        this.description = description;
        this.hasDescription = true;
    }
    @Override
    public String getImage()
    {
        return image;
    }
    @Override
    public boolean hasImage()
    {
        return hasImage;
    }
    @Override
    public void setImage(String image)
    {
        this.image = image;
        this.hasImage = true;
    }
    @Override
    public String getFieldValue(IndexEntryField field)
    {
        return field.getValue(this);
    }

    //-----PROTECTED METHODS-----

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
