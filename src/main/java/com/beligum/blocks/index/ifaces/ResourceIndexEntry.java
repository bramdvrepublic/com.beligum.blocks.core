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

package com.beligum.blocks.index.ifaces;

import com.beligum.blocks.index.fields.*;
import com.google.common.collect.Sets;

import java.util.Set;

/**
 * This is the general superclass for all entries in any kind of index.
 * Note that this super interface (compared to PageIndexEntry) mainly exists to support more than page searching later on (eg. media metadata indexing/searching)
 *
 * Created by bram on 2/14/16.
 */
public interface ResourceIndexEntry extends ResourceProxy
{
    //-----CONSTANTS-----
    //note: sync the names of these below with the getter names of AbstractIndexEntry and the setters of the implementations
    UriField uriField = new UriField();
    TokenizedUriField tokenisedUriField = new TokenizedUriField();
    ResourceTypeField resourceTypeField = new ResourceTypeField();
    ResourceField resourceField = new ResourceField();
    TypeOfField typeOfField = new TypeOfField();
    LanguageField languageField = new LanguageField();
    ParentUriField parentUriField = new ParentUriField();
    LabelField labelField = new LabelField();
    DescriptionField descriptionField = new DescriptionField();
    ImageField imageField = new ImageField();

    /**
     * Note: sync this with the constants above
     */
    Set<IndexEntryField> INTERNAL_FIELDS = Sets.newHashSet(uriField,
                                                           tokenisedUriField,
                                                           resourceTypeField,
                                                           resourceField,
                                                           typeOfField,
                                                           languageField,
                                                           parentUriField,
                                                           labelField,
                                                           descriptionField,
                                                           imageField);

    /**
     * This provides us with some more metadata about the type of index entry,
     * without having it to add to the (much more general) ResourceProxy interface.
     */
    enum Type
    {
        /**
         * Used when the resource is of no special type
         */
        DEFAULT,

        /**
         * Used when the resource is a sub-resource inside a parent
         */
        SUB,

        /**
         * Used when the resource is an indexed proxy of another resource
         */
        PROXY;
    }

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----

    /**
     * Specifies the type of this resource, see Type enum for details
     */
    Type getResourceType();

}
