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
import com.beligum.blocks.rdf.ifaces.RdfClass;
import org.eclipse.rdf4j.model.IRI;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;

/**
 * Created by bram on 2/13/16.
 */
public class SimplePageIndexEntry extends AbstractIndexEntry implements PageIndexEntry
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private String parentId;
    private String resource;
    private String typeOf;
    private String language;
    private String canonicalAddress;

    //-----CONSTRUCTORS-----
    // private, only for serialization
    private SimplePageIndexEntry() throws IOException
    {
        this((String) null, (String) null, (URI) null, (RdfClass) null, (String) null, (Locale) null, (URI) null, (String) null, (URI) null);
    }
    public SimplePageIndexEntry(String id, String parentId, URI resource, RdfClass typeOf, String title, Locale language, URI canonicalAddress, String description, URI image) throws IOException
    {
        super(id);

        this.setParentId(parentId);
        this.setResource(resource == null ? null : resource.toString());
        this.setTypeOf(typeOf == null ? null : typeOf.getCurieName().toString());
        this.setLabel(title);
        this.setLanguage(language == null ? null : language.getLanguage());
        this.setCanonicalAddress(canonicalAddress == null ? null : canonicalAddress.toString());
        this.setDescription(description);
        this.setImage(image == null ? null : image.toString());
    }

    //TODO to be deleted
    public static String generateId(Page page)
    {
        return AbstractPageIndexEntry.generateId(page);
    }
    public static String generateId(URI subResource)
    {
        return AbstractPageIndexEntry.generateId(subResource);
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getParentId()
    {
        return parentId;
    }
    private void setParentId(String parentId)
    {
        this.parentId = parentId;
    }
    @Override
    public String getResource()
    {
        return resource;
    }
    private void setResource(String resource)
    {
        this.resource = resource;
    }
    @Override
    public String getCanonicalAddress()
    {
        return canonicalAddress;
    }
    private void setCanonicalAddress(String canonicalAddress)
    {
        this.canonicalAddress = canonicalAddress;
    }
    @Override
    public String getTypeOf()
    {
        return typeOf;
    }
    private void setTypeOf(String typeOfCurie)
    {
        this.typeOf = typeOfCurie;
    }
    @Override
    public String getLanguage()
    {
        return language;
    }
    private void setLanguage(String language)
    {
        this.language = language;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----
    @Override
    public String toString()
    {
        return "PageIndexEntry{" +
               "id='" + id + '\'' +
               ", title='" + label + '\'' +
               ", language='" + language + '\'' +
               ", resource='" + resource + '\'' +
               ", parentId='" + parentId + '\'' +
               '}';
    }
}
