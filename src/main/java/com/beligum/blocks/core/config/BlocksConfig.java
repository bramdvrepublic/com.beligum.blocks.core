package com.beligum.blocks.core.config;

import com.beligum.core.framework.base.R;

/**
 * Created by bas on 08.10.14.
 */
public class BlocksConfig
{
    /**name of the scheme used for uri-identification of objects in the blocks-project*/
    public static final String SCHEME_NAME = "blocks";
    /**name of the folder where page-templates (page-classes) can be found*/
    public static final String ENTITIES_FOLDER = "entities";
    /**name of the folder where block-templates (block-classes) can be found*/
    public static final String PAGE_TEMPLATES_FOLDER = "pageTemplates";
    /**standard name of the html-file a page-class- or block-class-template must have to be recognized as such*/
    public static final String INDEX_FILE_NAME = "index.html";

    /**the languages this site can work with, ordered from most preferred languages, to less preferred*/
    public static String[] cachedLanguages;

    /**the redis-sentinels*/
    public static String[] cachedRedisSentinels;

    public static String getTemplateFolder()
    {
        return getConfiguration("blocks.template-folder");
    }

    public static String getSiteDomain()
    {
        return getConfiguration("blocks.site.domain");
    }

    public static String getSiteDBAlias()
    {
        return getConfiguration("blocks.site.db-alias");
    }

    public static String getEntitiesFolder() {
        return getTemplateFolder() + "/" + ENTITIES_FOLDER;
    }

    public static String getPageTemplatesFolder() {
        return getTemplateFolder() + "/" + PAGE_TEMPLATES_FOLDER;
    }

    public static String getRedisMasterName(){
        return getConfiguration("blocks.redis.master-name");
    }

    /**
     *
     * @return the sentinel-locations for the redis-db (f.i. localhost:26379)
     */
    public static String[] getRedisSentinels(){
        if(cachedRedisSentinels==null){
            cachedRedisSentinels = R.configuration().getStringArray("blocks.redis.sentinels");
        }
        return cachedRedisSentinels;
    }

    /**
     *
     * @return the languages this site can work with, ordered from most preferred language, to less preferred
     */
    public static String[] getLanguages(){
        if(cachedLanguages==null){
            cachedLanguages = R.configuration().getStringArray("blocks.site.languages");
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
