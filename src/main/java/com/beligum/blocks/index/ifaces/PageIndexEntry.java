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

import com.beligum.base.server.R;
import com.beligum.base.utils.toolkit.StringFunctions;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.utils.RdfTools;
import org.eclipse.rdf4j.model.IRI;

import java.net.URI;
import java.util.Locale;

/**
 * Instances of this class are the only values that are guaranteed to get written to the index.
 *
 * Created by bram on 2/23/16.
 */
public interface PageIndexEntry extends ResourceIndexEntry
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----

    /**
     * Goes through the supplied page search results and selects the single most fitting result, taking into account the supplied language.
     * Note that we always try to return a result. Language selection is based on the supplied language and the configured default language.
     *
     * @param searchResult the search results to look through
     * @param language the requested language (note that the selected result may not be this language)
     * @return the best fitting result
     */
    static ResourceProxy selectBestForLanguage(IndexSearchResult searchResult, Locale language)
    {
        ResourceProxy retVal = null;

        for (ResourceProxy entry : searchResult) {

            if (retVal == null) {
                retVal = entry;
            }
            else {
                int entryLangScore = PageIndexEntry.getLanguageScore(entry, language);
                int selectedLangScore = PageIndexEntry.getLanguageScore(retVal, language);
                if (entryLangScore > selectedLangScore) {
                    retVal = entry;
                }
            }
        }

        return retVal;
    }
    static ResourceProxy selectBestLanguage(IndexSearchResult searchResult)
    {
        //TODO check this
        return searchResult.getTotalHits() > 0 ? searchResult.iterator().next() : null;
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
    static int getLanguageScore(ResourceProxy entry, Locale requestLanguage)
    {
        int retVal = 1;

        if (entry.getLanguage().equals(requestLanguage)) {
            retVal = 3;
        }
        else if (entry.getLanguage().equals(R.configuration().getDefaultLanguage())) {
            retVal = 2;
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
