package com.beligum.blocks.config;

import com.beligum.base.security.Authentication;
import com.beligum.base.security.Principal;
import com.beligum.base.server.R;
import com.beligum.base.server.RequestContext;
import com.beligum.base.utils.Logger;
import com.google.common.base.Charsets;
import org.apache.shiro.UnavailableSecurityManagerException;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by bas on 08.10.14.
 */
public class BlocksConfig
{
    public static BlocksConfig instance;
    public static final String PROJECT_VERSION_KEY = "appVersion";
    public static final String PROPERTIES_FILE = "blocks.properties";

    /**the languages this site can work with, ordered from most preferred languages, to less preferred*/
    private LinkedHashMap<String, Locale> cachedLanguages;
    private Locale defaultLanguage;
    public String projectVersion = null;
    private URI siteDomain;
    private URI defaultRdfSchema;

    private BlocksConfig()
    {
    }

    public static BlocksConfig instance() {
        if (BlocksConfig.instance == null) {
            BlocksConfig.instance = new BlocksConfig();
        }
        return BlocksConfig.instance;
    }

    public URI getSiteDomain()
    {
        if (this.siteDomain == null) {
            try {
                this.siteDomain = new URI(getConfiguration("blocks.core.site.domain"));
            } catch (Exception e) {
                Logger.error("Site domain in blocks config is not valid. We return null");
            }
        }
        return this.siteDomain;
    }

    public URI getDefaultRdfSchema()
    {
        if (this.defaultRdfSchema == null) {
            try {
                this.defaultRdfSchema = new URI(getConfiguration("blocks.core.rdf.schema.url"));
            } catch (Exception e) {
                Logger.error("Default RDF Schema is not valid");
            }
        }
        return this.defaultRdfSchema;
    }

    public String getDefaultRdfPrefix()
    {
        return getConfiguration("blocks.core.rdf.schema.prefix");
    }

    public String getProjectVersion()
    {
        if (projectVersion == null) {
            Properties prop = new Properties();
            try (Reader reader = Files.newBufferedReader(R.resourceLoader().getResource("/" + PROPERTIES_FILE).getParsedPath(), Charsets.UTF_8)) {
                // load a properties file
                prop.load(reader);
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
        }
        return projectVersion;
    }

    /**
     * @return The languages this site can work with, ordered from most preferred getLanguage, to less preferred. If no such languages are specified in the configuration xml, an array with a default getLanguage is returned.
     */
    public LinkedHashMap<String, Locale> getLanguages(){
        if(cachedLanguages==null){
            cachedLanguages = new LinkedHashMap<>();
            ArrayList<String> cachedLanguagesTemp = new ArrayList<String>(Arrays.asList(R.configuration().getStringArray("blocks.core.site.languages")));

            for (String l : cachedLanguagesTemp) {

                Locale locale = new Locale(l);
                if (this.defaultLanguage == null) this.defaultLanguage = locale;
//                String getLanguage = locale;
                cachedLanguages.put(locale.getLanguage(), locale);
            }
            if(cachedLanguages.size() == 0){
                this.defaultLanguage = Locale.ENGLISH;
                cachedLanguages.put(defaultLanguage.getLanguage(), defaultLanguage);
            }
        }
        return cachedLanguages;
    }

    public Locale getLocaleForLanguage(String language) {
        Locale retVal = this.getDefaultLanguage();
        if (this.cachedLanguages.containsKey(language)) {
            retVal = this.cachedLanguages.get(language);
        }
        return retVal;
    }

    public String getCurrentUserName()
    {
        Principal currentPrincipal;
        try {
            currentPrincipal = Authentication.getCurrentPrincipal();
            if (currentPrincipal != null) {
                return currentPrincipal.getUsername();
            }
            else {
                return DatabaseConstants.SERVER_USER_NAME;
            }
        }
        //if no Shiro securitymanager is present, this means we're still starting up the server (and thus no securitymanager is configured yet)
        catch (UnavailableSecurityManagerException e) {
            return DatabaseConstants.SERVER_START_UP;
        }
    }

    /**
     *
     * @return The first languages in the languages-list, or the no-getLanguage-constant if no such list is present in the configuration-xml.
     */
    public Locale getDefaultLanguage(){
        if (defaultLanguage == null) {
            this.getLanguages();
        }
        return defaultLanguage;
    }

    public String getElasticSearchClusterName() {
        String retVal = R.configuration().getString("elastic-search.cluster");
        return retVal;
    }

    public String getElasticSearchHostName() {
        String retVal = R.configuration().getString("elastic-search.host");
        if (retVal == null) {
            retVal = "localhost";
        }
        return retVal;
    }

    public Integer getElasticSearchPort() {
        Integer retVal = R.configuration().getInt("elastic-search.port");
        if (retVal == null) {
            retVal = 9300;
        }
        return retVal;
    }

    public Locale getRequestDefaultLanguage() {
        Locale retVal = null;
        List<Locale> languages = RequestContext.getJaxRsRequest().getAcceptableLanguages();
        while (retVal == null && languages.iterator().hasNext()) {
            Locale loc = languages.iterator().next();
            if (BlocksConfig.instance().getLocaleForLanguage(loc.getLanguage()) != null) {
                retVal = loc;
            }
        }
        if (retVal == null) retVal = getDefaultLanguage();
        return retVal;
    }

    /**
     * return the text from the applications configuration file in specified tag
     *
     * @param configTag the configuration-tag
     * @return the value present in the configuration-tag, if '/' is the last character, it is removed
     */
    private String getConfiguration(String configTag)
    {
        String retVal = R.configuration().getString(configTag);
        if (retVal != null) {
            if (retVal.charAt(retVal.length() - 1) == '/') {
                retVal = retVal.substring(0, retVal.length() - 1);
            }
        }
        return retVal;
    }
}
