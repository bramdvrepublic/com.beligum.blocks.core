package com.beligum.blocks.config;

import com.beligum.base.security.Authentication;
import com.beligum.base.security.Principal;
import com.beligum.base.server.RequestContext;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.google.common.base.Charsets;
import com.beligum.blocks.base.Blocks;
import org.apache.shiro.UnavailableSecurityManagerException;

import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by bas on 08.10.14.
 */
public class BlocksConfig
{

    public static final String PROJECT_VERSION_KEY = "appVersion";
    public static final String PROPERTIES_FILE = "blocks.properties";

    /**the languages this site can work with, ordered from most preferred languages, to less preferred*/
    private HashMap<String, Locale> cachedLanguages;
    private Locale defaultLanguage;
    public String projectVersion = null;
    private URI siteDomain;
    private URI defaultRdfSchema;

    public BlocksConfig()
    {
    }

    public String getLuceneIndex()
    {
        return getConfiguration("blocks.lucene-index");
    }

    public String getTemplateFolder()
    {
        return getConfiguration("blocks.template-folder");
    }
    public String getBlueprintsFolder()
    {
        return getConfiguration("blocks.blueprints-folder");
    }

    public String getDefaultPageTitle()
    {
        String retVal = getConfiguration("blocks.default-page-title");
        if (retVal == null)
            retVal = "";
        return retVal;
    }


    public URI getSiteDomain()
    {
        if (this.siteDomain == null) {
            try {
                this.siteDomain = new URI(getConfiguration("blocks.site.domain"));
            } catch (Exception e) {
                Logger.error("Site domain in blocks config is not valid. We return null");
            }
        }
        return this.siteDomain;
    }

    public String getBlocksDBHost()
    {
        return getConfiguration("blocks.db.host");
    }
    public int getBlocksDBPort()
    {
        return Integer.parseInt(getConfiguration("blocks.db.port"));
    }

    public URI getDefaultRdfSchema()
    {
        if (this.defaultRdfSchema == null) {
            try {
                this.defaultRdfSchema = new URI(getConfiguration("blocks.rdf.schema.url"));
            } catch (Exception e) {
                Logger.error("Default RDF Schema is not valid");
            }
        }
        return this.defaultRdfSchema;
    }

    public String getDefaultRdfPrefix()
    {
        return getConfiguration("blocks.rdf.schema.prefix");
    }

    public String getFrontEndScripts()
    {
        return getConfiguration("blocks.scripts.frontend");
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
     * @return The languages this site can work with, ordered from most preferred language, to less preferred. If no such languages are specified in the configuration xml, an array with a default language is returned.
     */
    public HashMap<String, Locale> getLanguages(){
        if(cachedLanguages==null){
            cachedLanguages = new HashMap<>();
            ArrayList<String> cachedLanguagesTemp = new ArrayList<String>(Arrays.asList(R.configuration().getStringArray("blocks.site.languages")));

            for (String l : cachedLanguagesTemp) {

                Locale locale = new Locale(l);
                if (this.defaultLanguage == null) this.defaultLanguage = locale;
//                String language = locale;
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
        Locale retVal = this.cachedLanguages.get(language);
        if (retVal == null) retVal = this.getDefaultLanguage();
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
     * @return The first languages in the languages-list, or the no-language-constant if no such list is present in the configuration-xml.
     */
    public Locale getDefaultLanguage(){

        return defaultLanguage;
    }

    public Locale getRequestDefaultLanguage() {
        Locale retVal = null;
        List<Locale> languages = RequestContext.getJaxRsRequest().getAcceptableLanguages();
        while (retVal == null && languages.iterator().hasNext()) {
            Locale loc = languages.iterator().next();
            if (Blocks.config().getLocaleForLanguage(loc.getLanguage()) != null) {
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
