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

package com.beligum.blocks.filesystem.index.ifaces;

import com.beligum.base.server.R;
import com.beligum.base.utils.toolkit.StringFunctions;
import com.beligum.blocks.filesystem.index.entries.JsonField;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.eclipse.rdf4j.model.IRI;

import java.net.URI;
import java.util.Locale;

/**
 * Instances of this class are the only values that are guaranteed to get written to the index.
 *
 * Created by bram on 2/23/16.
 */
public interface PageIndexEntry extends IndexEntry
{
    //-----CONSTANTS-----
    //note: sync these with the getter names below (and the setters of the implementations)
    IndexEntryField parentId = new JsonField("parentId")
    {
        @Override
        public String getValue(IndexEntry indexEntry)
        {
            return ((PageIndexEntry)indexEntry).getParentId();
        }
        @Override
        public boolean hasValue(IndexEntry indexEntry)
        {
            return ((PageIndexEntry)indexEntry).hasParentId();
        }
        @Override
        public void setValue(IndexEntry indexEntry, String value)
        {
            ((PageIndexEntry)indexEntry).setParentId(value);
        }
    };
    IndexEntryField resource = new JsonField("resource")
    {
        @Override
        public String getValue(IndexEntry indexEntry)
        {
            return ((PageIndexEntry)indexEntry).getResource();
        }
        @Override
        public boolean hasValue(IndexEntry indexEntry)
        {
            return ((PageIndexEntry)indexEntry).hasResource();
        }
        @Override
        public void setValue(IndexEntry indexEntry, String value)
        {
            ((PageIndexEntry)indexEntry).setResource(value);
        }
    };
    IndexEntryField typeOf = new JsonField("typeOf")
    {
        @Override
        public String getValue(IndexEntry indexEntry)
        {
            return ((PageIndexEntry)indexEntry).getTypeOf();
        }
        @Override
        public boolean hasValue(IndexEntry indexEntry)
        {
            return ((PageIndexEntry)indexEntry).hasTypeOf();
        }
        @Override
        public void setValue(IndexEntry indexEntry, String value)
        {
            ((PageIndexEntry)indexEntry).setTypeOf(value);
        }
    };
    IndexEntryField language = new JsonField("language")
    {
        @Override
        public String getValue(IndexEntry indexEntry)
        {
            return ((PageIndexEntry)indexEntry).getLanguage();
        }
        @Override
        public boolean hasValue(IndexEntry indexEntry)
        {
            return ((PageIndexEntry)indexEntry).hasLanguage();
        }
        @Override
        public void setValue(IndexEntry indexEntry, String value)
        {
            ((PageIndexEntry)indexEntry).setLanguage(value);
        }
    };
    IndexEntryField canonicalAddress = new JsonField("canonicalAddress")
    {
        @Override
        public String getValue(IndexEntry indexEntry)
        {
            return ((PageIndexEntry)indexEntry).getCanonicalAddress();
        }
        @Override
        public boolean hasValue(IndexEntry indexEntry)
        {
            return ((PageIndexEntry)indexEntry).hasCanonicalAddress();
        }
        @Override
        public void setValue(IndexEntry indexEntry, String value)
        {
            ((PageIndexEntry)indexEntry).setCanonicalAddress(value);
        }
    };

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    /**
     * Only for sub-resources: the id of the parent index entry or null if this entry is not a sub-resource
     */
    String getParentId();

    /**
     * Returns true if this value has been set once (even though it might be null)
     */
    @JsonIgnore
    boolean hasParentId();

    /**
     * Sets the value of this field in this indexEntry
     */
    @JsonIgnore
    void setParentId(String value);

    /**
     * The string representation of the most basic resource URI (eg. for a public page, this is the low-level interconnecting "about" URI, not the public SEO-friendly one)
     */
    String getResource();

    /**
     * Returns true if this value has been set once (even though it might be null)
     */
    @JsonIgnore
    boolean hasResource();

    /**
     * Sets the value of this field in this indexEntry
     */
    @JsonIgnore
    void setResource(String value);

    /**
     * The string representation of the CURIE of the RdfClass type of the page
     */
    String getTypeOf();

    /**
     * Returns true if this value has been set once (even though it might be null)
     */
    @JsonIgnore
    boolean hasTypeOf();

    /**
     * Sets the value of this field in this indexEntry
     */
    @JsonIgnore
    void setTypeOf(String value);

    /**
     * What gets returned by the Locale.getLanguage() method of the page's language locale.
     */
    String getLanguage();

    /**
     * Returns true if this value has been set once (even though it might be null)
     */
    @JsonIgnore
    boolean hasLanguage();

    /**
     * Sets the value of this field in this indexEntry
     */
    @JsonIgnore
    void setLanguage(String value);

    /**
     * The string representation of the canonical address of this page (eg. the standardized form of the publicly visible address)
     */
    String getCanonicalAddress();

    /**
     * Returns true if this value has been set once (even though it might be null)
     */
    @JsonIgnore
    boolean hasCanonicalAddress();

    /**
     * Sets the value of this field in this indexEntry
     */
    @JsonIgnore
    void setCanonicalAddress(String value);

    /**
     * These are a couple of ID factory methods, grouped for overview
     * and make static so they can be used from the constructors
     */
    static String generateId(IRI iri)
    {
        return generateId(URI.create(iri.toString()));
    }
    static String generateId(Page page)
    {
        return generateId(page.getPublicRelativeAddress());
    }
    static String generateId(URI id)
    {
        //since we treat all URIs as relative, we only take the path into account
        return StringFunctions.getRightOfDomain(id).toString();
    }
    static String generateParentId(IndexEntry parent)
    {
        return parent == null ? null : parent.getId();
    }
    static String generateResource(URI rootResourceUri)
    {
        //Note that we index all addresses relatively
        return StringFunctions.getRightOfDomain(rootResourceUri).toString();
    }
    static String generateTypeOf(RdfClass rdfClass)
    {
        return rdfClass.getCurie().toString();
    }
    static String generateLanguage(Locale language)
    {
        return language.getLanguage();
    }
    static String generateCanonicalAddress(URI canonicalAddress)
    {
        //the canonical address is indexed as-is, we don't modify it
        return canonicalAddress.toString();
    }

    /**
     * Goes through the supplied page search results and selects the single most fitting result, taking into account the supplied language.
     * Note that we always try to return a result. Language selection is based on the supplied language and the configured default language.
     *
     * @param searchResult the search results to look through
     * @param language the requested language (note that the selected result may not be this language)
     * @return the best fitting result
     */
    static PageIndexEntry selectBestForLanguage(IndexSearchResult searchResult, Locale language)
    {
        PageIndexEntry retVal = null;

        for (IndexEntry entry : searchResult) {
            if (entry instanceof PageIndexEntry) {
                PageIndexEntry page = (PageIndexEntry) entry;

                if (retVal == null) {
                    retVal = page;
                }
                else {
                    int entryLangScore = PageIndexEntry.getLanguageScore(page, language);
                    int selectedLangScore = PageIndexEntry.getLanguageScore(retVal, language);
                    if (entryLangScore > selectedLangScore) {
                        retVal = page;
                    }
                }
            }
        }

        return retVal;
    }

    /**
     * This will return an int value according to the language of the entry;
     *
     * 1) entry language = no special language
     * 2) entry language = default language
     * 3) entry language = requested language
     *
     * --> higher means better
     */
    static int getLanguageScore(PageIndexEntry entry, Locale requestLanguage)
    {
        int retVal = 1;

        if (entry.getLanguage().equals(requestLanguage.getLanguage())) {
            retVal = 3;
        }
        else if (entry.getLanguage().equals(R.configuration().getDefaultLanguage().getLanguage())) {
            retVal = 2;
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
