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
import com.beligum.blocks.filesystem.index.entries.IndexEntryFieldImpl;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
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
    IndexEntryField parentId = new IndexEntryFieldImpl("parentId")
    {
        @Override
        public String getValue(IndexEntry indexEntry)
        {
            return ((PageIndexEntry)indexEntry).getParentId();
        }
    };
    IndexEntryField resource = new IndexEntryFieldImpl("resource")
    {
        @Override
        public String getValue(IndexEntry indexEntry)
        {
            return ((PageIndexEntry)indexEntry).getResource();
        }
    };
    IndexEntryField typeOf = new IndexEntryFieldImpl("typeOf")
    {
        @Override
        public String getValue(IndexEntry indexEntry)
        {
            return ((PageIndexEntry)indexEntry).getTypeOf();
        }
    };
    IndexEntryField language = new IndexEntryFieldImpl("language")
    {
        @Override
        public String getValue(IndexEntry indexEntry)
        {
            return ((PageIndexEntry)indexEntry).getLanguage();
        }
    };
    IndexEntryField canonicalAddress = new IndexEntryFieldImpl("canonicalAddress")
    {
        @Override
        public String getValue(IndexEntry indexEntry)
        {
            return ((PageIndexEntry)indexEntry).getCanonicalAddress();
        }
    };
    IndexEntryField object = new IndexEntryFieldImpl("object")
    {
        @Override
        public String getValue(IndexEntry indexEntry)
        {
            //don't really know what to return here...
            return null;
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
     * The string representation of the most basic resource URI (eg. for a public page, this is the low-level interconnecting "about" URI, not the public SEO-friendly one)
     */
    String getResource();

    /**
     * The string representation of the CURIE of the RdfClass type of the page
     */
    String getTypeOf();

    /**
     * What gets returned by the Locale.getLanguage() method of the page's language locale.
     */
    String getLanguage();

    /**
     * The string representation of the canonical address of this page (eg. the standardized form of the publicly visible address)
     */
    String getCanonicalAddress();

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
