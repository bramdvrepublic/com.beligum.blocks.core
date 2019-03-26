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

package com.beligum.blocks.filesystem.index.entries.pages;

import com.beligum.blocks.filesystem.index.ifaces.IndexEntry;

/**
 * Simple implementation that provides accessors to all the required fields and offers a basic equals() implementation
 *
 * Created by bram on 2/14/16.
 */
public abstract class AbstractIndexEntry implements IndexEntry
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    protected String id;
    protected String label;
    protected String description;
    protected String image;

    //-----CONSTRUCTORS-----
    protected AbstractIndexEntry(String id)
    {
        this.id = id;
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getId()
    {
        return id;
    }
    @Override
    public String getLabel()
    {
        return label;
    }
    @Override
    public String getDescription()
    {
        return description;
    }
    @Override
    public String getImage()
    {
        return image;
    }

    //-----PROTECTED METHODS-----
    protected void setId(String id)
    {
        this.id = id;
    }
    protected void setLabel(String label)
    {
        this.label = label;
    }
    protected void setDescription(String description)
    {
        this.description = description;
    }
    protected void setImage(String image)
    {
        this.image = image;
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
