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
