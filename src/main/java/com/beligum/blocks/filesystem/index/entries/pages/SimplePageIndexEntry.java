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
import com.beligum.blocks.filesystem.index.entries.IndexEntry;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import org.eclipse.rdf4j.model.IRI;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;

/**
 * Created by bram on 2/13/16.
 */
public class SimplePageIndexEntry extends AbstractPageIndexEntry implements PageIndexEntry
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private String parentId;
    private String resource;
    private String typeOf;
    private String language;
    private String canonicalAddress;

    //-----CONSTRUCTORS-----
    public SimplePageIndexEntry(String id, String parentId, URI resource, RdfClass typeOf, String title, Locale language, URI canonicalAddress, String description, URI image) throws IOException
    {
        this(id,
             parentId,
             resource == null ? null : resource.toString(),
             typeOf == null ? null : typeOf.getCurieName().toString(),
             title,
             language == null ? null : language.getLanguage(),
             canonicalAddress == null ? null : canonicalAddress.toString(),
             description,
             image == null ? null : image.toString());
    }
    //for serialization
    private SimplePageIndexEntry() throws IOException
    {
        this((String) null, (String) null, (URI) null, (RdfClass) null, (String) null, (Locale) null, (URI) null, (String) null, (URI) null);
    }
    private SimplePageIndexEntry(String id, String parentId, String resource, String typeOfCurie, String title, String language, String canonicalAddress, String description, String image) throws IOException
    {
        super(id);

        this.setResource(resource);
        this.setParentId(parentId);
        this.setTypeOf(typeOfCurie);
        this.setTitle(title);
        this.setLanguage(language);
        this.setCanonicalAddress(canonicalAddress);
        this.setDescription(description);
        this.setImage(image);
    }
    //    protected SimplePageIndexEntry(String documentId, Resource subject, Model rdfModel, String language) throws IOException
    //    {
    //        super(documentId);
    //
    //        Optional<IRI> typeOfIRI = Models.objectIRI(rdfModel.filter(subject, RDF.TYPE, null));
    //        RdfClass typeOf = !typeOfIRI.isPresent() ? null : RdfFactory.getClassForResourceType(RdfTools.fullToCurie(URI.create(typeOfIRI.get().toString())));
    //        if (typeOf == null) {
    //            throw new IOException("Trying to instance an index entry from an RDF model without a type, this shouldn't happen; " + subject);
    //        }
    //        else {
    //            //TODO check this
    //            this.setResource(documentId == null ? null : documentId);
    //            this.setTypeOf(typeOf.getCurieName().toString());
    //            this.setLanguage(language);
    //            //TODO check this
    //            this.setCanonicalAddress(documentId);
    //
    //            //note: the getResourceSummarizer() never returns null (has a SimpleResourceIndexer as fallback)
    //            ResourceSummarizer.SummarizedResource indexEntry = typeOf.getResourceSummarizer().summarize(rdfModel);
    //            this.setTitle(indexEntry.getTitle());
    //            this.setDescription(indexEntry.getDescription());
    //            this.setImage(indexEntry.getImage() == null ? null : indexEntry.getImage().toString());
    //
    //        }
    //    }

    //-----STATIC METHODS-----
    //These are a couple of ID factory methods, grouped for overview
    public static String generateId(IRI iri)
    {
        return generateId(URI.create(iri.toString()));
    }
    public static String generateId(Page page)
    {
        return generateId(page.getPublicRelativeAddress());
    }
    public static String generateId(URI id)
    {
        //since we treat all URIs as relative, we only take the path into account
        return StringFunctions.getRightOfDomain(id).toString();
    }
    public static String generateId(IndexEntry indexEntry)
    {
        return indexEntry.getId();
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
               ", title='" + title + '\'' +
               ", language='" + language + '\'' +
               ", resource='" + resource + '\'' +
               ", parentId='" + parentId + '\'' +
               '}';
    }
}
