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

package com.beligum.blocks.rdf.ontology.vocabularies.endpoints;

import com.beligum.blocks.rdf.ifaces.RdfClass;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by bram on 3/14/16.
 */
public class LanguageEnumQueryEndpoint extends EnumQueryEndpoint
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private static Map<String, EnumAutocompleteSuggestion> languageSuggestions;

    //-----CONSTRUCTORS-----
    public LanguageEnumQueryEndpoint(RdfClass resourceType)
    {
        super(resourceType);

        //we can make the languageSuggestions static and save some space because it won't change across instances anyway
        if (languageSuggestions==null) {
            languageSuggestions = new LinkedHashMap<>();
            String[] allLangs = Locale.getISOLanguages();
            for (String lang : allLangs) {
                LanguageEnumSuggestion sugg = new LanguageEnumSuggestion(Locale.forLanguageTag(lang));
                languageSuggestions.put(sugg.getValue(), sugg);
            }
        }
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----
    @Override
    protected Map<String, EnumAutocompleteSuggestion> getSuggestions()
    {
        return languageSuggestions;
    }

    //-----PRIVATE METHODS-----
    public class LanguageEnumSuggestion extends SimpleEnumSuggestion
    {
        private Locale locale;

        public LanguageEnumSuggestion(Locale locale)
        {
            this.locale = locale;
            this.value = locale.getLanguage();
        }

        @Override
        public String getTitleFor(Locale lang)
        {
            return this.locale.getDisplayLanguage(lang);
        }
    }
}
