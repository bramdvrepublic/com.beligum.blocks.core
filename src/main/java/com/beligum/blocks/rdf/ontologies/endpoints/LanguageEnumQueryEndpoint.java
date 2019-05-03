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

package com.beligum.blocks.rdf.ontologies.endpoints;

import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.ibm.icu.util.ULocale;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by bram on 3/14/16.
 */
public class LanguageEnumQueryEndpoint extends EnumQueryEndpoint
{
    //-----CONSTANTS-----
    private static final String UNKNOWN_LANGUAGE_CODE = "und";

    //-----VARIABLES-----
    private static Map<String, EnumSuggestion> languageSuggestions;

    //-----CONSTRUCTORS-----
    public LanguageEnumQueryEndpoint()
    {
        this(null);
    }
    public LanguageEnumQueryEndpoint(RdfClass resourceType)
    {
        super(resourceType);
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----
    @Override
    protected Map<String, EnumSuggestion> getSuggestions()
    {
        //we can make the languageSuggestions static and save some space because it won't change across instances anyway
        if (languageSuggestions == null) {
            languageSuggestions = new LinkedHashMap<>();
            //we switched from the built-in java list to the list of the more complete ICU4J project
            //String[] allLangs = Locale.getISOLanguages();

            for (String langCode : ULocale.getISOLanguages()) {
                //we'll generally won't allow the unknown language in the selection
                if (!langCode.equals(UNKNOWN_LANGUAGE_CODE)) {
                    LanguageEnumSuggestion sugg = new LanguageEnumSuggestion(langCode);
                    String engName = sugg.getLabelFor(Locale.ENGLISH);
                    //Note: we'll do a little filtering: the language's name must exist in english
                    //and it's name can't be the same as the language code (which is returned by getDisplayLanguage() when no such entry is present in the DB)
                    //this is a good reference, but not all translations have been added to ICU(4j) so it seems
                    //http://www.loc.gov/standards/iso639-2/php/code_list.php
                    if (!StringUtils.isEmpty(engName) && !engName.equals(langCode)) {
                        languageSuggestions.put(langCode, sugg);
                    }
                }
            }
        }

        return languageSuggestions;
    }

    //-----PRIVATE METHODS-----
    public class LanguageEnumSuggestion extends SimpleEnumSuggestion
    {
        private static final String EMPTY_TITLE_RETVAL = "?????";
        private ULocale locale;

        public LanguageEnumSuggestion(String isoCode)
        {
            this.locale = new ULocale(isoCode);
            this.value = isoCode;
        }

        @Override
        public String getLabelFor(Locale lang)
        {
            String retVal = this.locale.getDisplayLanguage(ULocale.forLocale(lang));

            //this shouldn't happen, but it did in case of the und lang, so let's catch it
            if (StringUtils.isEmpty(retVal)) {
                retVal = EMPTY_TITLE_RETVAL;
            }

            return retVal;
        }
    }
}
