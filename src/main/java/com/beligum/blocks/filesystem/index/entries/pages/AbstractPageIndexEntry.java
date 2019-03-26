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
    protected String parentId;
    protected String resource;
    protected String typeOf;
    protected String language;
    protected String canonicalAddress;

    //-----CONSTRUCTORS-----
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
    @Override
    public String getResource()
    {
        return resource;
    }
    @Override
    public String getTypeOf()
    {
        return typeOf;
    }
    @Override
    public String getLanguage()
    {
        return language;
    }
    @Override
    public String getCanonicalAddress()
    {
        return canonicalAddress;
    }

    //-----PROTECTED METHODS-----
    protected void setParentId(String parentId)
    {
        this.parentId = parentId;
    }
    protected void setResource(String resource)
    {
        this.resource = resource;
    }
    protected void setTypeOf(String typeOf)
    {
        this.typeOf = typeOf;
    }
    protected void setLanguage(String language)
    {
        this.language = language;
    }
    protected void setCanonicalAddress(String canonicalAddress)
    {
        this.canonicalAddress = canonicalAddress;
    }

    //-----STATIC METHODS-----
    /**
     * These are a couple of ID factory methods, grouped for overview
     * and make static so they can be used from the constructors
     */
    protected static String generateId(IRI iri)
    {
        return generateId(URI.create(iri.toString()));
    }
    protected static String generateId(Page page)
    {
        return generateId(page.getPublicRelativeAddress());
    }
    protected static String generateId(URI id)
    {
        //since we treat all URIs as relative, we only take the path into account
        return StringFunctions.getRightOfDomain(id).toString();
    }
    protected static String generateId(IndexEntry indexEntry)
    {
        return indexEntry.getId();
    }

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----
}
