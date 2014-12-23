package com.beligum.blocks.core.config;

import com.beligum.core.framework.base.R;

/**
 * Created by bas on 08.10.14.
 */
public class BlocksConfig
{
    /**the default language for this site, if no language is specified in the configuration-xml*/
    public static final String DEFAULT_LANGUAGE = "language_default";

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
                cachedLanguages[0] = DEFAULT_LANGUAGE;
            }
        }
        return cachedLanguages;
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
