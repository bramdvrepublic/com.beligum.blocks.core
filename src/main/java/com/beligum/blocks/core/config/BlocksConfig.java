package com.beligum.blocks.core.config;

import com.beligum.blocks.core.internationalization.Languages;
import com.beligum.core.framework.base.R;
import org.apache.commons.configuration.ConfigurationRuntimeException;

import java.util.Locale;

/**
 * Created by bas on 08.10.14.
 */
public class BlocksConfig
{
    /**the path to the location of bootstrap*/
    public static final String BOOTSTRAP_JS_FILEPATH = "assets/media/js/bootstrap.min.js";
    public static final String BOOSTRAP_CSS_FILEPATH = "/assets/libs/bootstrap/css/bootstrap.css";

    /**the languages this site can work with, ordered from most preferred languages, to less preferred*/
    public static String[] cachedLanguages;

    /**the redis-sentinels*/
    public static String[] cachedRedisSentinels;

    public static String getTemplateFolder()
    {
        return getConfiguration("blocks.template-folder");
    }
    public static String getBlueprintsFolder()
    {
        return getConfiguration("blocks.blueprints-folder");
    }

    public static String getSiteDomain()
    {
        return getConfiguration("blocks.site.domain");
    }

    public static String getRedisMasterHost()
    {
        return getConfiguration("blocks.redis.master-host");
    }
    public static String getRedisMasterPort()
    {
        return getConfiguration("blocks.redis.master-port");
    }
    public static String getSiteDBAlias()
    {
        return getConfiguration("blocks.site.db-alias");
    }

    public static String getRedisMasterName(){
        return getConfiguration("blocks.redis.master-name");
    }

    /**
     *
     * @return The sentinel-locations for the redis-db (f.i. localhost:26379) specified in the configuration xml or null if no sentiles are specified in the configuration xml.
     */
    public static String[] getRedisSentinels(){
        if(cachedRedisSentinels==null){
            cachedRedisSentinels = R.configuration().getStringArray("blocks.redis.sentinels");
        }
        return cachedRedisSentinels;
    }

    /**
     *
     * @return The languages this site can work with, ordered from most preferred language, to less preferred. If no such languages are specified in the configuration xml, an array with a default language is returned.
     */
    public static String[] getLanguages(){
        if(cachedLanguages==null){
            cachedLanguages = R.configuration().getStringArray("blocks.site.languages");
            if(cachedLanguages.length == 0){
                cachedLanguages = new String[1];
                cachedLanguages[0] = Languages.NO_LANGUAGE;
            }
            else{
                for(int i=0; i<cachedLanguages.length; i++){
                    Locale locale = new Locale(cachedLanguages[i]);
                    String language = locale.getLanguage();
                    if(!Languages.containsLanguageCode(language)){
                        throw new ConfigurationRuntimeException("Found language-code which doesn't follow the proper standard (ISO 639).");
                    }
                    cachedLanguages[i] = language;
                }
            }
        }
        return cachedLanguages;
    }

    /**
     *
     * @return The first languages in the languages-list, or the no-language-constant if no such list is present in the configuration-xml.
     */
    public static String getDefaultLanguage(){
        return getLanguages()[0];
    }

    /**
     * return the text from the applications configuration file in specified tag
     * @param configTag the configuration-tag
     * @return the value present in the configuration-tag, if '/' is the last character, it is removed
     */
    private static String getConfiguration(String configTag){
        String retVal = R.configuration().getString(configTag);
        if (retVal != null) {
            if (retVal.charAt(retVal.length() - 1) == '/') {
                retVal = retVal.substring(0, retVal.length()-1);
            }
        }
        return retVal;
    }
}
