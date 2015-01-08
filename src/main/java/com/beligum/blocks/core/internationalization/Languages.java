package com.beligum.blocks.core.internationalization;

import com.beligum.blocks.core.config.BlocksConfig;
import org.apache.commons.beanutils.converters.ArrayConverter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.SetUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * Created by bas on 06.01.15.
 */
public class Languages
{
    /**string representing the absence of a language*/
    public static final String NO_LANGUAGE = "no_language";
    /**set with all permitted languages*/
    public static Set<String> cachedPermittedLanguages = new HashSet<>();

    /**
     *
     * @param languageCode
     * @return a standardized language-code for the language-code specified, or the no-language-string if no language-code was specified
     */
    static public String getStandardizedLanguage(String languageCode){
        if(containsLanguageCode(languageCode)){
            return new Locale(languageCode).getLanguage();
        }
        else{
            return NO_LANGUAGE;
        }
    }

    /**
     *
     * @param s
     * @return true if the specified string contains a preferred language or another ISO-standard language-code
     */
    static public boolean containsLanguageCode(String s){
        if(StringUtils.isEmpty(s) || s.equals(NO_LANGUAGE)){
            return false;
        }
        else{
            s = new Locale(s).getLanguage();
            boolean foundLanguage = false;
            String[] preferredLanguages = BlocksConfig.getLanguages();
            int i = 0;
            while(!foundLanguage && i<preferredLanguages.length){
                foundLanguage = s.equals(new Locale(preferredLanguages[i]).getLanguage());
                i++;
            }
            if(!foundLanguage) {
                i = 0;
                String[] isoLanguages = Locale.getISOLanguages();
                while(!foundLanguage && i<isoLanguages.length){
                    foundLanguage = s.equals(isoLanguages[i]);
                    i++;
                }
            }
            return foundLanguage;
        }
    }

    /**
     *
     * @return all language-codes that are permitted (ISO 639) and a constant representing the absence of a language
     */
    static public Set<String> getPermittedLanguageCodes(){
        if(cachedPermittedLanguages.isEmpty()) {
            String[] isoLanguages = Locale.getISOLanguages();
            Set<String> permittedLanguages = new HashSet(Arrays.asList(isoLanguages));
            permittedLanguages.add(NO_LANGUAGE);
            cachedPermittedLanguages = permittedLanguages;
        }
        return cachedPermittedLanguages;
    }

    /**
     * Determines which language is the preferred language starting from a set of languages.
     * Uses the site's preferred languages, and if none of the languages can be found, it returns a random one.
     * If an empty set is specified, the constant Languages.NO_LANGUAGE is returned.
     * @param languages
     * @return
     */
    static public String determinePrimaryLanguage(Set<String> languages){
        String[] preferredLanguages = BlocksConfig.getLanguages();
        String primaryLanguage = null;
        if(languages == null || languages.isEmpty()){
            primaryLanguage = NO_LANGUAGE;
        }
        int i = 0;
        while(primaryLanguage == null && i<preferredLanguages.length){
            if(languages.contains(preferredLanguages[i])){
                primaryLanguage = preferredLanguages[i];
            }
        }
        if(primaryLanguage == null){
            primaryLanguage = languages.iterator().next();
        }
        return primaryLanguage;
    }

}
