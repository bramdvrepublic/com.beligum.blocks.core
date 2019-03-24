/*
 * Copyright 2018 Republic of Reinvention bvba. All Rights Reserved.
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

package com.beligum.blocks.filesystem.pages;

import com.beligum.base.utils.toolkit.StringFunctions;
import com.beligum.blocks.filesystem.index.ifaces.PageIndexEntry;
import com.beligum.blocks.filesystem.index.entries.pages.SimplePageIndexEntry;
import com.beligum.blocks.filesystem.index.ifaces.ResourceSummarizer;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import org.eclipse.rdf4j.model.Model;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;

/**
 * This is a simple wrapper class for RDF models that can hold some extra
 * information to cache calculated values and pass them along.
 * Note that a pagemodel can hold both the main model (the rdf-ised version of the entire page)
 * or the rdf representation of a sub-resource inside that page.
 * This class was introduced to easier parse and index sub-resources.
 */
public class PageModel
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private String id;
    private Page page;
    private URI mainResource;
    private URI subResource;
    private RdfClass subType;
    private Model subModel;
    private boolean isMain;

    //-----CONSTRUCTORS-----
    public PageModel(Page page, URI mainResource, URI subResource, RdfClass subType, Model subModel)
    {
        this.page = page;
        this.mainResource = mainResource;
        this.subResource = subResource;
        this.subType = subType;
        this.subModel = subModel;

        this.isMain = this.mainResource.equals(this.subResource);
        this.id = this.generateId(this.isMain());
    }

    //-----PUBLIC METHODS-----
    public String getId()
    {
        return id;
    }
    public Page getPage()
    {
        return page;
    }
    public URI getMainResource()
    {
        return mainResource;
    }
    public URI getSubResource()
    {
        return subResource;
    }
    public RdfClass getSubType()
    {
        return subType;
    }
    public Model getSubModel()
    {
        return subModel;
    }
    public boolean isMain()
    {
        return isMain;
    }

    /**
     * Converts this (sub-)model of a page to an index entry.
     */
    public PageIndexEntry toPageIndexEntry() throws IOException
    {
        PageIndexEntry retVal = null;

        if (this.getSubType() == null) {
            throw new IOException("Trying to instance an index entry from an RDF model without a type, can't proceed; " + this.getSubResource());
        }
        else {

            String id = this.generateId(this.isMain());

            //Note: the whole point is that page resources don't have a parent, so if this is a main resource, set it to null
            String parentId = this.isMain() ? null : this.generateId(true);

            URI resource = this.getSubResource();
            //Note that we index all addresses relatively, including the resource uri
            if (resource.isAbsolute()) {
                resource = StringFunctions.getRightOfDomain(resource);
            }

            RdfClass type = this.getSubType();

            //First, note that the canonicalAddress is relative (like all URIs in the index)
            //Secondly, just like with the id, the c-address of the main page is just the c-address of that page,
            //but sub-resources only have one URI: the resource URI
            URI canonicalAddress = this.isMain() ? this.getPage().getCanonicalAddress() : resource;

            //Note that the language is set per page, including all it's sub resources, because the sub-resource
            //is newly created for every resource (and not shared among pages like main resource URIs)
            Locale lang = this.getPage().getLanguage();

            String title = null;
            String description = null;
            URI image = null;

            //note: the getSummarizer() never returns null (has a SimpleResourceIndexer as fallback),
            //but let's play safe
            ResourceSummarizer summarizer = type.getSummarizer();
            if (summarizer != null) {
                ResourceSummarizer.SummarizedResource summary = summarizer.summarize(type, this.getSubModel());
                if (summary != null) {
                    title = summary.getTitle();
                    description = summary.getDescription();
                    image = summary.getImage();
                }
                else {
                    throw new IOException("RDF class summarizer returned a null summary; this shouldn't happen; " + type);
                }
            }
            else {
                throw new IOException("Encountered an RDF class with a null summarizer; this shouldn't happen; " + type);
            }

            retVal = new SimplePageIndexEntry(id, parentId, resource, type, title, lang, canonicalAddress, description, image);
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private String generateId(boolean forMain)
    {
        return forMain ? SimplePageIndexEntry.generateId(this.getPage()) : SimplePageIndexEntry.generateId(this.getSubResource());
    }
}
