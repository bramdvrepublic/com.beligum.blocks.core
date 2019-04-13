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

import com.beligum.blocks.index.ifaces.IndexEntryField;
import com.beligum.blocks.index.ifaces.ResourceIndexEntry;
import com.beligum.blocks.rdf.ifaces.RdfClass;

import java.net.URI;
import java.util.Locale;

/**
 * Simple implementation that provides accessors to all the required fields and offers a basic equals() implementation
 *
 * Created by bram on 2/14/16.
 */
public abstract class AbstractIndexEntry implements ResourceIndexEntry
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    protected URI uri;
    protected String resource;
    protected RdfClass typeOf;
    protected Locale language;
    protected URI parentUri;
    protected String label;
    protected String description;
    protected URI image;

    //-----CONSTRUCTORS-----
    protected AbstractIndexEntry()
    {
    }
    protected AbstractIndexEntry(URI uri)
    {
        this.setUri(uri);
    }

    //-----PUBLIC METHODS-----
    @Override
    public URI getUri()
    {
        return uri;
    }
    public void setUri(URI uri)
    {
        this.uri = uri;
    }
    @Override
    public String getResource()
    {
        return resource;
    }
    public void setResource(String resource)
    {
        this.resource = resource;
    }
    @Override
    public RdfClass getTypeOf()
    {
        return typeOf;
    }
    public void setTypeOf(RdfClass typeOf)
    {
        this.typeOf = typeOf;
    }
    @Override
    public Locale getLanguage()
    {
        return language;
    }
    public void setLanguage(Locale language)
    {
        this.language = language;
    }
    @Override
    public URI getParentUri()
    {
        return parentUri;
    }
    public void setParentUri(URI parentUri)
    {
        this.parentUri = parentUri;
    }
    @Override
    public String getLabel()
    {
        return label;
    }
    public void setLabel(String label)
    {
        this.label = label;
    }
    @Override
    public String getDescription()
    {
        return description;
    }
    public void setDescription(String description)
    {
        this.description = description;
    }
    @Override
    public URI getImage()
    {
        return image;
    }
    public void setImage(URI image)
    {
        this.image = image;
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
