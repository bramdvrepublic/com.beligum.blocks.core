package com.beligum.blocks.config;

import com.beligum.base.server.R;
import org.apache.commons.configuration.HierarchicalConfiguration;

import java.net.URI;
import java.util.*;

/**
 * Created by bas on 08.10.14.
 */
public class Settings
{
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



    public URI getDefaultRdfSchema()
    {
        if (this.defaultRdfSchema == null) {
            String schema = R.configuration().getString("blocks.core.rdf.schema.url");
            try {
                this.defaultRdfSchema = URI.create(schema);
            }
            catch (Exception e) {
                throw new RuntimeException("Wrong default RDF blocks.core.rdf.schema.url configured; "+schema, e);
            }
        }
        return this.defaultRdfSchema;
    }
    public String getDefaultRdfPrefix()
    {
        return R.configuration().getString("blocks.core.rdf.schema.prefix");
    }
}
