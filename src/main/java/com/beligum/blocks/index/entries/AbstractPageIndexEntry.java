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

import com.beligum.blocks.index.ifaces.PageIndexEntry;

/**
 * Simple implementation that provides accessors to all the required fields and offers a basic equals() implementation
 *
 * Created by bram on 2/14/16.
 */
public abstract class AbstractPageIndexEntry extends AbstractIndexEntry implements PageIndexEntry
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    //Notes:
    // - the underscore is to differentiate this field from the constant field definitions in IndexEntry
    // - the booleans is to detect unset fields while supporting null values
    protected String parentId;
    protected boolean hasParentId;

    //-----CONSTRUCTORS-----
    protected AbstractPageIndexEntry()
    {
        super();
    }
    protected AbstractPageIndexEntry(String id)
    {
        super(id);
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getParentId()
    {
        return parentId;
    }
    public boolean hasParentId()
    {
        return hasParentId;
    }
    public void setParentId(String parentId)
    {
        this.parentId = parentId;
        this.hasParentId = true;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----
}
