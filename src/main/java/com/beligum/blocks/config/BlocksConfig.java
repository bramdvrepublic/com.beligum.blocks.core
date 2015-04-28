package com.beligum.blocks.config;

import com.beligum.base.security.Authentication;
import com.beligum.base.security.Principal;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import org.apache.shiro.UnavailableSecurityManagerException;

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

    public static final String PROJECT_VERSION_KEY = "appVersion";
    public static final String PROPERTIES_FILE = "blocks.properties";

    /**
     * the languages this site can work with, ordered from most preferred languages, to less preferred
     */
    public static ArrayList<String> cachedLanguages;
    public static String projectVersion = null;

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

    public String getSiteDomain()
    {
        return getConfiguration("blocks.site.domain");
    }

    public URL getSiteDomainUrl() throws MalformedURLException
    {
        return new URL(getSiteDomain());
    }

    public String getBlocksDBHost()
    {
        return getConfiguration("blocks.db.host");
    }
    public int getBlocksDBPort()
    {
        return Integer.parseInt(getConfiguration("blocks.db.port"));
    }

    public String getDefaultRdfSchema()
    {
        return getConfiguration("blocks.rdf.schema.url");
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
     * @return The languages this site can work with, ordered from most preferred language, to less preferred. If no such languages are specified in the configuration xml, an array with a default language is returned.
     */
    public ArrayList<String> getLanguages()
    {
        if (cachedLanguages == null) {
            cachedLanguages = new ArrayList<String>();
            ArrayList<String> cachedLanguagesTemp = new ArrayList<String>(Arrays.asList(R.configuration().getStringArray("blocks.site.languages")));

            for (String l : cachedLanguagesTemp) {
                Locale locale = new Locale(l);
                String language = locale.getLanguage();

                cachedLanguages.add(language);
            }
            if (cachedLanguages.size() == 0) {
                Locale locale = new Locale("en");
                cachedLanguages.add(locale.getLanguage());
            }
        }
        return cachedLanguages;
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
     * @return The first languages in the languages-list, or the no-language-constant if no such list is present in the configuration-xml.
     */
    public String getDefaultLanguage()
    {
        return getLanguages().get(0);
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
