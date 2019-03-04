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

package com.beligum.blocks.rdf.ontologies.local;

import com.beligum.base.server.R;
import com.beligum.blocks.endpoints.ifaces.ResourceInfo;
import com.beligum.blocks.filesystem.index.entries.pages.PageIndexEntry;

import java.net.URI;
import java.util.Locale;

/**
 * Created by bram on 3/12/16.
 */
public class WrappedPageResourceInfo implements ResourceInfo
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private PageIndexEntry indexEntry;

    //-----CONSTRUCTORS-----
    //For json deserialization
    private WrappedPageResourceInfo()
    {
        this(null);
    }
    public WrappedPageResourceInfo(PageIndexEntry indexEntry)
    {
        this.indexEntry = indexEntry;
    }

    //-----PUBLIC METHODS-----
    @Override
    public URI getResourceUri()
    {
        return this.indexEntry.getResource() == null ? null : URI.create(this.indexEntry.getResource());
    }
    @Override
    public URI getResourceType()
    {
        if (this.indexEntry.getTypeOf() != null) {
            return URI.create(this.indexEntry.getTypeOf());
        }
        else {
            return null;
        }
    }
    @Override
    public String getLabel()
    {
        return this.indexEntry.getTitle();
    }
    @Override
    public URI getLink()
    {
        //note: the ID of a page is the public URL
        return this.indexEntry.getId() == null ? null : URI.create(this.indexEntry.getId());
    }
    @Override
    public boolean isExternalLink()
    {
        //local resources aren't external
        return false;
    }
    @Override
    public URI getImage()
    {
        return this.indexEntry.getImage() == null ? null : URI.create(this.indexEntry.getImage());
    }
    @Override
    public String getName()
    {
        return this.indexEntry.getTitle() == null ? null : this.indexEntry.getTitle();
    }
    @Override
    public Locale getLanguage()
    {
        return this.indexEntry.getLanguage() == null ? null : R.configuration().getLocaleForLanguage(this.indexEntry.getLanguage());
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
