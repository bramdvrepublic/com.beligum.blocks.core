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

import com.beligum.base.utils.toolkit.StringFunctions;
import com.beligum.blocks.filesystem.index.ifaces.IndexEntry;
import com.beligum.blocks.filesystem.index.ifaces.PageIndexEntry;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import org.eclipse.rdf4j.model.IRI;

import java.net.URI;

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
    protected String _parentId;
    protected boolean hasParentId;
    protected String _resource;
    protected boolean hasResource;
    protected String _typeOf;
    protected boolean hasTypeOf;
    protected String _language;
    protected boolean hasLanguage;
    protected String _canonicalAddress;
    protected boolean hasCanonicalAddress;

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
        return _parentId;
    }
    @Override
    public boolean hasParentId()
    {
        return hasParentId;
    }
    @Override
    public String getResource()
    {
        return _resource;
    }
    @Override
    public boolean hasResource()
    {
        return hasResource;
    }
    @Override
    public String getTypeOf()
    {
        return _typeOf;
    }
    @Override
    public boolean hasTypeOf()
    {
        return hasTypeOf;
    }
    @Override
    public String getLanguage()
    {
        return _language;
    }
    @Override
    public boolean hasLanguage()
    {
        return hasLanguage;
    }
    @Override
    public String getCanonicalAddress()
    {
        return _canonicalAddress;
    }
    @Override
    public boolean hasCanonicalAddress()
    {
        return hasCanonicalAddress;
    }

    //-----PROTECTED METHODS-----
    protected void setParentId(String parentId)
    {
        this._parentId = parentId;
        this.hasParentId = true;
    }
    protected void setResource(String resource)
    {
        this._resource = resource;
        this.hasResource = true;
    }
    protected void setTypeOf(String typeOf)
    {
        this._typeOf = typeOf;
        this.hasTypeOf = true;
    }
    protected void setLanguage(String language)
    {
        this._language = language;
        this.hasLanguage = true;
    }
    protected void setCanonicalAddress(String canonicalAddress)
    {
        this._canonicalAddress = canonicalAddress;
        this.hasCanonicalAddress = true;
    }

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----
}
