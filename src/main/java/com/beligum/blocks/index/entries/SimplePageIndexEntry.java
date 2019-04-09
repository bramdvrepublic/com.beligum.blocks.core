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
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.google.common.collect.Sets;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Locale;

/**
 * Created by bram on 2/13/16.
 */
public class SimplePageIndexEntry extends AbstractPageIndexEntry
{
    //-----CONSTANTS-----
    private static Collection<IndexEntryField> INTERNAL_FIELDS = Sets.newHashSet(idField,
                                                                                 tokenisedIdField,
                                                                                 labelField,
                                                                                 descriptionField,
                                                                                 imageField,
                                                                                 parentIdField,
                                                                                 resourceField,
                                                                                 typeOfField,
                                                                                 languageField,
                                                                                 canonicalAddressField
    );

    //-----VARIABLES-----

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

    //-----PUBLIC METHODS-----
    @Override
    public Iterable<IndexEntryField> getInternalFields()
    {
        return INTERNAL_FIELDS;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----
    @Override
    public String toString()
    {
        return "PageIndexEntry{" +
               "id='" + idField + '\'' +
               ", title='" + labelField + '\'' +
               ", language='" + languageField + '\'' +
               ", resource='" + resourceField + '\'' +
               ", parentId='" + parentIdField + '\'' +
               '}';
    }
}
