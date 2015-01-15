package com.beligum.blocks.core.internationalization;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.exceptions.LanguageException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.RedisID;
import org.apache.commons.beanutils.converters.ArrayConverter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.SetUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URL;
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
     * @param s
     * @return true if the specified string is a language-code
     */
    static public boolean isNonEmptyLanguageCode(String s){
        return !StringUtils.isEmpty(s) && !s.equals(NO_LANGUAGE) && getPermittedLanguageCodes().contains(s);
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
            i++;
        }
        if(primaryLanguage == null){
            primaryLanguage = languages.iterator().next();
        }
        return primaryLanguage;
    }

    /**
     * Determine which of the language-ids is the site's preferred one (primary)
     * This method uses the preferred order of languages specified in the configuration-xml.
     * @param languageIds a set of languages to choose from, if this set is empty, null will be returned
     * @return
     */
    public static RedisID determinePrimaryLanguageId(Set<RedisID> languageIds)
    {
        Map<String, RedisID> languages = new HashMap();
        for(RedisID languageId : languageIds){
            languages.put(languageId.getLanguage(), languageId);
        }
        String primaryLanguage = Languages.determinePrimaryLanguage(languages.keySet());
        return languages.get(primaryLanguage);
    }

    /**
     * Method translating the string representation of a url to the specified language. Relative url's must start with a '/' for them to be properly translated.
     * Relative urls without '/' will not be translated, nor will absolute urls to other sites then the one specified in the configuration xml.
     * @param urlString a url absolute or relative (and starting with '/'), to be translated
     * @param language the language to be added to the url
     * @return a string-representation of the absolute or relative url, or the urlString itself if it is empty (or null)
     * @throws LanguageException
     */
    static public String translateUrl(String urlString, String language) throws LanguageException{
        try {
            if(StringUtils.isEmpty(urlString)){
                return urlString;
            }
            /*
             * Check language
             */
            Set<String> permittedLanguages = Languages.getPermittedLanguageCodes();
            if (!Languages.getPermittedLanguageCodes().contains(language)) {
                throw new LanguageException("Found unknown language: " + language);
            }
            /*
             * Check url
             */
            URL url;
            boolean isAbsolute;
            boolean startsWithSlash;
            //since the specified url could be a relative one, we first use a uri to detect that
            URI uri = new URI(urlString);
            if(uri.isAbsolute()) {
                url = uri.toURL();
                isAbsolute = true;
                //only http-protocols will be translated (so f.i. a mailto-protocol will stay unchanged)
                //only absolute links of this very site will be translated
                if(!"http".equals(url.getProtocol()) || !new URL(BlocksConfig.getSiteDomain()).getAuthority().equals(url.getAuthority())){
                    return urlString;
                }
            }
            //relative urls are first turned into absolute one's
            else{
                url = new URL(new URL(BlocksConfig.getSiteDomain()), uri.toString());
                isAbsolute = false;
                startsWithSlash = urlString.startsWith("/");
                if(!isAbsolute && !startsWithSlash){
                    return urlString;
                }
            }
            /*
             * Remove present language-information and add the new language, or remove it if Languages.NO_LANGUAGE was specified
             */
            String urlPath = url.getFile();
            if (!StringUtils.isEmpty(url.getRef())) {
                urlPath += "#" + url.getRef();
            }
            String[] splitted = urlPath.split("/");
            //the uri-path always starts with "/", so the first index in the splitted-array always will be empty ""
            if (splitted.length > 1) {
                String foundLanguage = splitted[1];
                if (permittedLanguages.contains(foundLanguage)) {
                    //remove the language-information from the middle of the id
                    urlPath = "";
                    for (int j = 2; j < splitted.length; j++) {
                        urlPath += "/" + splitted[j];
                    }
                }
            }
            if(!NO_LANGUAGE.equals(language)) {
                urlPath = "/" + language + urlPath;
            }
            /*
             * Revert the absolute to relative urls, if needed
             */
            if(isAbsolute) {
                return new URL(url.getProtocol(), url.getHost(), url.getPort(), urlPath).toString();
            }
            else{
                return urlPath;
            }
        }catch(Exception e){
            throw new LanguageException("Could not translate url '" + urlString + "' into '" + language + "'.", e);
        }
    }

}
