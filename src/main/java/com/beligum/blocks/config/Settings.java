package com.beligum.blocks.config;

import com.beligum.base.security.Authentication;
import com.beligum.base.security.Principal;
import com.beligum.base.server.R;
import com.beligum.base.server.RequestContext;
import com.beligum.base.utils.Logger;
import com.google.common.base.Charsets;
import org.apache.commons.configuration.HierarchicalConfiguration;
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
public class Settings
{
    public static final String PROJECT_VERSION_KEY = "appVersion";
    public static final String PROPERTIES_FILE = "blocks.properties";

    private static final String PAGES_HDFS_PROPERTIES_KEY = "blocks.core.pages.hdfs.properties.property";
    private static final String ELASTIC_SEARCH_PROPERTIES_KEY = "blocks.core.elastic-search.properties.property";

    private static final String DEFAULT_FILE_EXT = ".html";
    private static final String DEFAULT_LOCK_FILE_EXT = ".lock";

    private static Settings instance;
    /**
     * the languages this site can work with, ordered from most preferred languages, to less preferred
     */
    private LinkedHashMap<String, Locale> cachedLanguages;
    private Locale cachedDefaultLanguage;
    public String projectVersion = null;
    private URI cachedSiteDomain;
    private URI defaultRdfSchema;
    private URI cachedPagesStorePath;
    protected HashMap<String, String> cachedHdfsProperties = null;
    protected HashMap<String, String> cachedEsProperties = null;

    private Settings()
    {
    }

    public static Settings instance()
    {
        if (Settings.instance == null) {
            Settings.instance = new Settings();
        }
        return Settings.instance;
    }

    public URI getSiteDomain()
    {
        if (this.cachedSiteDomain == null) {
            String schema = R.configuration().getString("blocks.core.domain");
            try {
                this.cachedSiteDomain = URI.create(schema);
            }
            catch (Exception e) {
                throw new RuntimeException("Site domain in blocks config is not valid. This setting is vital, can't proceed.");
            }
        }

        return this.cachedSiteDomain;
    }
    /**
     * @return The languages this site can work with, ordered from most preferred getLanguage, to less preferred. If no such languages are specified in the configuration xml, an array with a default getLanguage is returned.
     */
    public LinkedHashMap<String, Locale> getLanguages()
    {
        if (cachedLanguages == null) {
            cachedLanguages = new LinkedHashMap<>();
            ArrayList<String> cachedLanguagesTemp = new ArrayList<String>(Arrays.asList(R.configuration().getStringArray("blocks.core.languages")));

            for (String l : cachedLanguagesTemp) {

                Locale locale = new Locale(l);
                if (this.cachedDefaultLanguage == null)
                    this.cachedDefaultLanguage = locale;
                //                String getLanguage = locale;
                cachedLanguages.put(locale.getLanguage(), locale);
            }
            if (cachedLanguages.size() == 0) {
                this.cachedDefaultLanguage = Locale.ENGLISH;
                cachedLanguages.put(cachedDefaultLanguage.getLanguage(), cachedDefaultLanguage);
            }
        }
        return cachedLanguages;
    }
    /**
     * @return The first languages in the languages-list, or the no-getLanguage-constant if no such list is present in the configuration-xml.
     */
    public Locale getDefaultLanguage()
    {
        if (cachedDefaultLanguage == null) {
            this.getLanguages();
        }
        return cachedDefaultLanguage;
    }
    public Locale getLocaleForLanguage(String language)
    {
        Locale retVal = this.getDefaultLanguage();
        if (this.cachedLanguages.containsKey(language)) {
            retVal = this.cachedLanguages.get(language);
        }
        return retVal;
    }
    public URI getPagesStorePath()
    {
        if (this.cachedPagesStorePath == null) {
            String path = R.configuration().getString("blocks.core.pages.store-path");
            //this is used to resolve files on, so make sure it's always a formal directory
            if (!path.endsWith("/")) {
                path += "/";
            }
            this.cachedPagesStorePath = URI.create(path);
        }

        return this.cachedPagesStorePath;
    }
    public Map<String, String> getPagesHdfsProperties()
    {
        if (this.cachedHdfsProperties == null) {
            this.cachedHdfsProperties = new HashMap<String, String>();
            List<HierarchicalConfiguration> properties = R.configuration().configurationsAt(PAGES_HDFS_PROPERTIES_KEY);
            for (HierarchicalConfiguration property : properties) {
                String propertyKey = property.getString("name");
                String propertyValue = property.getString("value");
                this.cachedHdfsProperties.put(propertyKey, propertyValue);
            }
        }

        return this.cachedHdfsProperties;
    }
    public String getPagesFileExtension()
    {
        return R.configuration().getString("blocks.core.pages.file-ext", DEFAULT_FILE_EXT);
    }
    public String getPagesLockFileExtension()
    {
        return R.configuration().getString("blocks.core.pages.lock-file-ext", DEFAULT_LOCK_FILE_EXT);
    }
    public URI getDefaultRdfSchema()
    {
        if (this.defaultRdfSchema == null) {
            String schema = R.configuration().getString("blocks.core.rdf.com.beligum.blocks.schema.schema.url");
            try {
                this.defaultRdfSchema = URI.create(schema);
            }
            catch (Exception e) {
                throw new RuntimeException("Wrong default RDF com.beligum.blocks.schema.schema configured; "+schema, e);
            }
        }
        return this.defaultRdfSchema;
    }

    public String getDefaultRdfPrefix()
    {
        return R.configuration().getString("blocks.core.rdf.com.beligum.blocks.schema.schema.prefix");
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

    public boolean getElasticSearchLaunchEmbedded()
    {
        return R.configuration().getBoolean("blocks.core.elastic-search.launch-embedded", true);
    }

    public String getElasticSearchClusterName()
    {
        return R.configuration().getString("blocks.core.elastic-search.cluster-name");
    }

    public String getElasticSearchHostName()
    {
        return R.configuration().getString("blocks.core.elastic-search.host", "localhost");
    }

    public Integer getElasticSearchPort()
    {
        return R.configuration().getInt("blocks.core.elastic-search.port", 9000);
    }

    public HashMap<String, String> getElasticSearchProperties()
    {
        if (this.cachedEsProperties == null) {
            this.cachedEsProperties = new HashMap<String, String>();
            List<HierarchicalConfiguration> properties = R.configuration().configurationsAt(ELASTIC_SEARCH_PROPERTIES_KEY);
            for (HierarchicalConfiguration property : properties) {
                String propertyKey = property.getString("name");
                String propertyValue = property.getString("value");
                this.cachedEsProperties.put(propertyKey, propertyValue);
            }
        }

        return this.cachedEsProperties;
    }

    public Locale getRequestDefaultLanguage()
    {
        Locale retVal = null;
        List<Locale> languages = RequestContext.getJaxRsRequest().getAcceptableLanguages();
        while (retVal == null && languages.iterator().hasNext()) {
            Locale loc = languages.iterator().next();
            if (Settings.instance().getLocaleForLanguage(loc.getLanguage()) != null) {
                retVal = loc;
            }
        }
        if (retVal == null)
            retVal = getDefaultLanguage();
        return retVal;
    }
}
