package com.beligum.blocks.core.config;

import com.beligum.blocks.core.dbs.AbstractBlockDatabase;
import com.beligum.blocks.core.dbs.BlocksDatabase;
import com.beligum.blocks.core.dbs.BlocksTemplateCache;
import com.beligum.blocks.core.dbs.BlocksUrlDispatcher;
import com.beligum.blocks.core.internationalization.Languages;
import com.beligum.core.framework.base.R;
import com.beligum.core.framework.cache.CacheKey;
import com.beligum.core.framework.utils.Logger;
import com.beligum.core.framework.cache.CacheManager;
import org.apache.commons.configuration.ConfigurationRuntimeException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by bas on 08.10.14.
 */
public class BlocksConfig
{
    private enum BlocksConfigCacheKey implements CacheKey
    {
        BLOCKS_CONFIG_CACHE_KEY
    }

    private static BlocksConfig instance = null;
    /**the path to the location of bootstrap*/
    public static final String BOOTSTRAP_JS_FILEPATH = "assets/media/js/bootstrap.min.js";
    public static final String BOOSTRAP_CSS_FILEPATH = "/assets/libs/bootstrap/css/bootstrap.css";

    public static final String PROJECT_VERSION_KEY = "appVersion";
    public static final String PROPERTIES_FILE = "blocks.properties";



    /**the languages this site can work with, ordered from most preferred languages, to less preferred*/
    public static ArrayList<String> cachedLanguages;
    public static String projectVersion = null;

    /**the redis-sentinels*/
    public static String[] cachedRedisSentinels;

    private AbstractBlockDatabase database;
    private BlocksUrlDispatcher urlDispatcher;
    private BlocksTemplateCache templateCache;

    private BlocksConfig() {

    }

    public static BlocksConfig getInstance() {
        if (R.cacheManager().getApplicationCache().get(BlocksConfigCacheKey.BLOCKS_CONFIG_CACHE_KEY) == null) {
            R.cacheManager().getApplicationCache().put(BlocksConfigCacheKey.BLOCKS_CONFIG_CACHE_KEY, new BlocksConfig());
        }
        return (BlocksConfig)R.cacheManager().getApplicationCache().get(BlocksConfigCacheKey.BLOCKS_CONFIG_CACHE_KEY);
    }

    public static String getTemplateFolder()
    {
        return getConfiguration("blocks.template-folder");
    }
    public static String getBlueprintsFolder()
    {
        return getConfiguration("blocks.blueprints-folder");
    }

    public static String getDefaultPageTitle() {
        String retVal = getConfiguration("blocks.default-page-title");
        if (retVal == null) retVal = "";
        return retVal;
    }

    public static String getSiteDomain()
    {
        return getConfiguration("blocks.site.domain");
    }

    public static URL getSiteDomainUrl() throws MalformedURLException
    {
        return new URL(getSiteDomain());
    }

    public static String getRedisMasterHost()
    {
        return getConfiguration("blocks.redis.master-host");
    }
    public static String getSiteDBAlias()
    {
        return getConfiguration("blocks.site.db-alias");
    }

    public static String getMongoHost()
    {
        return getConfiguration("blocks.mongodb.host");
    }
    public static int getMongoPort()
    {
        return Integer.parseInt(getConfiguration("blocks.mongodb.port"));
    }

    public static String getRedisMasterName(){
        return getConfiguration("blocks.redis.master-name");
    }

    public static String getFrontEndScripts(){
        return getConfiguration("blocks.scripts.frontend");
    }

    public static String getSolrServerUrl() {return getConfiguration("blocks.solr.url");}

    public static String getProjectVersion(){
        if(projectVersion == null) {
            Properties prop = new Properties();
            InputStream input = null;
            try {
                input = BlocksConfig.class.getResourceAsStream("/" + PROPERTIES_FILE);
                // load a properties file
                prop.load(input);
                // get the property value and print it out
                projectVersion = prop.getProperty(PROJECT_VERSION_KEY);
            }
            catch (IOException e) {
                Logger.error("Couldn't determine project-version, used current time.", e);
                //if an io-exception occured, just give the current date as project-version, so at least it can be checked afterwards
                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                //get current date time with Date()
                Date date = new Date();
                projectVersion = dateFormat.format(date);
            }
            finally {
                if (input != null) {
                    try {
                        input.close();
                    }
                    catch (IOException e) {
                        Logger.error("Error while closing inputstream of properties-file.", e);
                    }
                }
            }
        }
        return projectVersion;
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
    public static ArrayList<String> getLanguages(){
        if(cachedLanguages==null){
            cachedLanguages = new ArrayList<String>();
            ArrayList<String> cachedLanguagesTemp = new ArrayList<String>(Arrays.asList(R.configuration().getStringArray("blocks.site.languages")));

            for(String l: cachedLanguagesTemp){
                Locale locale = new Locale(l);
                String language = locale.getLanguage();
                if(!Languages.isLanguageCode(language)){
                    throw new ConfigurationRuntimeException("Found language-code which doesn't follow the proper standard (ISO 639).");
                }
                cachedLanguages.add(language);
            }
            if(cachedLanguages.size() == 0){
                Locale locale = new Locale("en");
                cachedLanguages.add(locale.getLanguage());
            }
        }
        return cachedLanguages;
    }

    /**
     *
     * @return The first languages in the languages-list, or the no-language-constant if no such list is present in the configuration-xml.
     */
    public static String getDefaultLanguage(){
        return getLanguages().get(0);
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

    public AbstractBlockDatabase getDatabase()
    {
        return database;
    }
    public void setDatabase(AbstractBlockDatabase database)
    {
        this.database = database;
    }
    public BlocksUrlDispatcher getUrlDispatcher()
    {
        return urlDispatcher;
    }
    public void setUrlDispatcher(BlocksUrlDispatcher urlDispatcher)
    {
        this.urlDispatcher = urlDispatcher;
    }
    public BlocksTemplateCache getTemplateCache()
    {
        return templateCache;
    }
    public void setTemplateCache(BlocksTemplateCache templateCache)
    {
        this.templateCache = templateCache;
    }
}
