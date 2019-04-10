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

import com.beligum.blocks.index.ifaces.ResourceIndexEntry;
import com.beligum.blocks.index.ifaces.IndexEntryField;

/**
 * Simple implementation that provides accessors to all the required fields and offers a basic equals() implementation
 *
 * Created by bram on 2/14/16.
 */
public abstract class AbstractIndexEntry implements ResourceIndexEntry
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    //Notes:
    // - the booleans is to detect unset fields while supporting null values
    protected String uri;
    protected boolean hasUri;
    protected String resource;
    protected boolean hasResource;
    protected String typeOf;
    protected boolean hasTypeOf;
    protected String language;
    protected boolean hasLanguage;
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
    protected AbstractIndexEntry(String uri)
    {
        this.setUri(uri);
    }

    //-----PUBLIC METHODS-----
    @Override
    public String getUri()
    {
        return uri;
    }
    public boolean hasUri()
    {
        return hasUri;
    }
    public void setUri(String uri)
    {
        this.uri = uri;
        this.hasUri = true;
    }
    @Override
    public String getResource()
    {
        return resource;
    }
    public boolean hasResource()
    {
        return hasResource;
    }
    public void setResource(String resource)
    {
        this.resource = resource;
        this.hasResource = true;
    }
    @Override
    public String getTypeOf()
    {
        return typeOf;
    }
    public boolean hasTypeOf()
    {
        return hasTypeOf;
    }
    public void setTypeOf(String typeOf)
    {
        this.typeOf = typeOf;
        this.hasTypeOf = true;
    }
    @Override
    public String getLanguage()
    {
        return language;
    }
    public boolean hasLanguage()
    {
        return hasLanguage;
    }
    public void setLanguage(String language)
    {
        this.language = language;
        this.hasLanguage = true;
    }
    @Override
    public String getLabel()
    {
        return label;
    }
    public boolean hasLabel()
    {
        return hasLabel;
    }
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
    public boolean hasDescription()
    {
        return hasDescription;
    }
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
    public boolean hasImage()
    {
        return hasImage;
    }
    public void setImage(String image)
    {
        this.image = image;
        this.hasImage = true;
    }

    //-----PROTECTED METHODS-----
    /**
     * This should return the list of internal fields, that will be added to the public RDF fields, in order
     * to make this entry function.
     */
    protected abstract Iterable<IndexEntryField> getInternalFields();

    /**
     * This is a generic getter to get the value associated with the internal field key
     */
    protected String getFieldValue(IndexEntryField field)
    {
        return field.getValue(this);
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

        return getUri() != null ? getUri().equals(that.getUri()) : that.getUri() == null;

    }
    @Override
    public int hashCode()
    {
        return getUri() != null ? getUri().hashCode() : 0;
    }
}
