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

import com.beligum.base.utils.toolkit.StringFunctions;
import com.beligum.blocks.filesystem.index.ifaces.IndexEntry;
import com.beligum.blocks.filesystem.index.ifaces.PageIndexEntry;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
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
    //Note: the underscore is to differentiate this field from the constant field definitions in IndexEntry
    protected String _parentId;
    protected String _resource;
    protected String _typeOf;
    protected String _language;
    protected String _canonicalAddress;

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
    public String getResource()
    {
        return _resource;
    }
    @Override
    public String getTypeOf()
    {
        return _typeOf;
    }
    @Override
    public String getLanguage()
    {
        return _language;
    }
    @Override
    public String getCanonicalAddress()
    {
        return _canonicalAddress;
    }

    //-----PROTECTED METHODS-----
    protected void setParentId(String parentId)
    {
        this._parentId = parentId;
    }
    protected void setResource(String resource)
    {
        this._resource = resource;
    }
    protected void setTypeOf(String typeOf)
    {
        this._typeOf = typeOf;
    }
    protected void setLanguage(String language)
    {
        this._language = language;
    }
    protected void setCanonicalAddress(String canonicalAddress)
    {
        this._canonicalAddress = canonicalAddress;
    }

    //-----STATIC METHODS-----
    /**
     * These are a couple of ID factory methods, grouped for overview
     * and make static so they can be used from the constructors
     */
    protected static URI generateId(IRI iri)
    {
        return generateId(URI.create(iri.toString()));
    }
    protected static URI generateId(Page page)
    {
        return generateId(page.getPublicRelativeAddress());
    }
    protected static URI generateId(URI id)
    {
        //since we treat all URIs as relative, we only take the path into account
        return StringFunctions.getRightOfDomain(id);
    }

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----
}
