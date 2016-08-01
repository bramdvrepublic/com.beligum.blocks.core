package com.beligum.blocks.rdf.ontology.vocabularies.endpoints;

import com.beligum.base.cache.Cache;
import com.beligum.base.cache.EhCacheAdaptor;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.base.utils.json.Json;
import com.beligum.base.utils.xml.XML;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.endpoints.ifaces.AutocompleteSuggestion;
import com.beligum.blocks.endpoints.ifaces.RdfQueryEndpoint;
import com.beligum.blocks.endpoints.ifaces.ResourceInfo;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ontology.vocabularies.geo.AbstractGeoname;
import com.beligum.blocks.rdf.ontology.vocabularies.geo.GeonameResourceInfo;
import com.beligum.blocks.utils.RdfTools;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.client.ClientConfig;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.*;

import static com.beligum.blocks.caching.CacheKeys.GEONAMES_CACHED_RESULTS;

/**
 * Created by bram on 3/14/16.
 */
public class GeonameQueryEndpoint implements RdfQueryEndpoint
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private String username;
    private AbstractGeoname.Type geonameType;
    //note: check the inner cache class if you add variables

    //-----CONSTRUCTORS-----
    public GeonameQueryEndpoint(AbstractGeoname.Type geonameType)
    {
        this.username = Settings.instance().getGeonamesUsername();
        this.geonameType = geonameType;
    }

    //-----PUBLIC METHODS-----
    @Override
    //Note: check the inner cache class if you add variables
    public Collection<AutocompleteSuggestion> search(RdfClass resourceType, final String query, QueryType queryType, Locale language, int maxResults, SearchOption... options) throws IOException
    {
        List<AutocompleteSuggestion> retVal = new ArrayList<>();

        //I guess an empty query can't yield any results, right?
        if (!StringUtils.isEmpty(query)) {

            //use a cached result if it's there
            CachedSearch cacheKey = new CachedSearch(this.geonameType, resourceType, query, queryType, language, options);
            List<AutocompleteSuggestion> cachedResult = this.getCachedEntry(cacheKey);
            if (cachedResult != null) {
                retVal = cachedResult;
            }
            else {
                ClientConfig config = new ClientConfig();
                Client httpClient = ClientBuilder.newClient(config);
                //for details, see http://www.geonames.org/export/geonames-search.html
                UriBuilder builder = UriBuilder.fromUri("http://api.geonames.org/search")
                                               .queryParam("username", this.username)
                                               //no need to fetch the entire node; we'll do that during selection
                                               //note: we selct MEDIUM instead of SHORT to get the full country name (for cities)
                                               .queryParam("style", "MEDIUM")
                                               .queryParam("maxRows", maxResults)
                                               //I think the default is "population", which seems to be more natural
                                               // (better to find a large, more-or-less-good match, than to find the very specific wrong match)
                                               //can be any of [population,elevation,relevance]
                                               //Note: reverted to relevance (inspired by eg. Tielt-Winge, who kept on suggesting Houwaart because it has a higher population)
                                               .queryParam("orderby", "relevance")
                                               .queryParam("type", "json");

                //from the Geoname docs: needs to be query encoded (but builder.queryParam() does that for us, so don't encode twice!)!
                switch (queryType) {
                    case STARTS_WITH:
                        builder.queryParam("name_startsWith", query);
                        break;
                    case NAME:
                        builder.queryParam("name", query);
                        break;
                    case FULL:
                        //Note that 'q' searches over everything (capital, continent, etc) of a place or country,
                        // often resulting in a too-broad result set (often not sorted the way we want, so if we take the first, it's often very wrong)
                        // but it does allow us to use terms like 'Halen,Belgium' to specify more precisely what we want.
                        builder.queryParam("q", query);
                        break;
                    default:
                        throw new IOException("Unsupported or unimplemented query type encountered, can't proceed; " + queryType);
                }

                if (geonameType.featureClasses != null) {
                    for (String c : geonameType.featureClasses) {
                        builder.queryParam("featureClass", c);
                    }
                }

                if (geonameType.featureCodes != null) {
                    for (String c : geonameType.featureCodes) {
                        builder.queryParam("featureCode", c);
                    }
                }

                if (language != null) {
                    builder.queryParam("lang", language.getLanguage());
                }

                URI target = builder.build();
                Response response = httpClient.target(target).request(MediaType.APPLICATION_JSON).get();
                if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                    JsonNode jsonNode = Json.getObjectMapper().readTree(response.readEntity(String.class));
                    Iterator<JsonNode> geonames = jsonNode.path("geonames").elements();

                    InjectableValues inject = new InjectableValues.Std().addValue(AbstractGeoname.RESOURCE_TYPE_INJECTABLE, resourceType.getCurieName());
                    ObjectReader reader = Json.getObjectMapper().readerFor(geonameType.suggestionClass).with(inject);

                    while (geonames.hasNext()) {
                        try {
                            retVal.add((AutocompleteSuggestion) reader.readValue(geonames.next()));
                        }
                        catch (Exception e) {
                            Logger.error(query, e);
                        }
                    }
                }
                else {
                    throw new IOException("Error status returned while searching for geonames resource '" + query + "'; " + response);
                }

                this.putCachedEntry(cacheKey, retVal);
            }
        }

        return retVal;
    }
    @Override
    public ResourceInfo getResource(RdfClass resourceType, URI resourceId, Locale language) throws IOException
    {
        GeonameResourceInfo retVal = null;

        if (resourceId != null && !resourceId.toString().isEmpty()) {

            //use a cached result if it's there
            CachedResource cacheKey = new CachedResource(resourceType, resourceId, language);
            GeonameResourceInfo cachedResult = this.getCachedEntry(cacheKey);
            if (cachedResult != null) {
                retVal = cachedResult;
            }
            else {
                ClientConfig config = new ClientConfig();
                Client httpClient = ClientBuilder.newClient(config);
                UriBuilder builder = UriBuilder.fromUri("http://api.geonames.org/get")
                                               .queryParam("username", this.username)
                                               //we pass only the id, not the entire URI
                                               .queryParam("geonameId", RdfTools.extractResourceId(resourceId))
                                               //when we query, we query for a lot
                                               .queryParam("style", "FULL")
                                               .queryParam("type", "json");

                if (language != null) {
                    builder.queryParam("lang", language.getLanguage());
                }

                URI target = builder.build();
                Response response = httpClient.target(target).request(MediaType.APPLICATION_JSON).get();
                if (response.getStatus() == Response.Status.OK.getStatusCode()) {

                    InjectableValues inject = new InjectableValues.Std().addValue(AbstractGeoname.RESOURCE_TYPE_INJECTABLE, resourceType.getCurieName());
                    ObjectReader reader = XML.getObjectMapper().readerFor(GeonameResourceInfo.class).with(inject);

                    //note: the Geonames '/get' endpoint is XML only!
                    retVal = reader.readValue(response.readEntity(String.class));

                    //API doesn't seem to return this -> set it manually
                    retVal.setLanguage(language);
                }
                else {
                    throw new IOException("Error status returned while searching for geonames id '" + resourceId + "'; " + response);
                }

                this.putCachedEntry(cacheKey, retVal);
            }
        }

        return retVal;
    }
    @Override
    public URI getExternalResourceRedirect(URI resourceId, Locale language)
    {
        //TODO what about the language?
        return AbstractGeoname.toGeonamesUri(RdfTools.extractResourceId(resourceId));
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private List<AutocompleteSuggestion> getCachedEntry(CachedSearch query)
    {
        return (List<AutocompleteSuggestion>) this.getGeonameCache().get(query);
    }
    private void putCachedEntry(CachedSearch query, List<AutocompleteSuggestion> results)
    {
        this.getGeonameCache().put(query, results);
    }
    private GeonameResourceInfo getCachedEntry(CachedResource query)
    {
        return (GeonameResourceInfo) this.getGeonameCache().get(query);
    }
    private void putCachedEntry(CachedResource query, GeonameResourceInfo results)
    {
        this.getGeonameCache().put(query, results);
    }
    private Cache getGeonameCache()
    {
        if (!R.cacheManager().cacheExists(GEONAMES_CACHED_RESULTS.name())) {
            //we create a cache where it's entries live for one hour (both from creation time as from last accessed time),
            //doesn't overflow to disk and keep at most 100 results
            R.cacheManager().registerCache(new EhCacheAdaptor(GEONAMES_CACHED_RESULTS.name(), 100, false, false, 60 * 60, 60 * 60));
        }

        return R.cacheManager().getCache(GEONAMES_CACHED_RESULTS.name());
    }

    /**
     * This class makes sure the hashmap takes all query parameters into account while caching the resutls
     */
    private static class CachedSearch
    {
        private AbstractGeoname.Type geonameType;
        private RdfClass resourceType;
        private String query;
        private QueryType queryType;
        private Locale language;
        private SearchOption[] options;

        public CachedSearch(AbstractGeoname.Type geonameType, RdfClass resourceType, String query, QueryType queryType, Locale language, SearchOption[] options)
        {
            this.geonameType = geonameType;
            this.resourceType = resourceType;
            this.query = query;
            this.queryType = queryType;
            this.language = language;
            this.options = options;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
                return true;
            if (!(o instanceof CachedSearch))
                return false;

            CachedSearch that = (CachedSearch) o;

            if (geonameType != that.geonameType)
                return false;
            if (resourceType != null ? !resourceType.equals(that.resourceType) : that.resourceType != null)
                return false;
            if (query != null ? !query.equals(that.query) : that.query != null)
                return false;
            if (queryType != that.queryType)
                return false;
            if (language != null ? !language.equals(that.language) : that.language != null)
                return false;
            // Probably incorrect - comparing Object[] arrays with Arrays.equals
            return Arrays.equals(options, that.options);

        }
        @Override
        public int hashCode()
        {
            int result = geonameType != null ? geonameType.hashCode() : 0;
            result = 31 * result + (resourceType != null ? resourceType.hashCode() : 0);
            result = 31 * result + (query != null ? query.hashCode() : 0);
            result = 31 * result + (queryType != null ? queryType.hashCode() : 0);
            result = 31 * result + (language != null ? language.hashCode() : 0);
            result = 31 * result + Arrays.hashCode(options);
            return result;
        }
    }

    private static class CachedResource
    {
        private RdfClass resourceType;
        private URI resourceId;
        private Locale language;

        public CachedResource(RdfClass resourceType, URI resourceId, Locale language)
        {
            this.resourceType = resourceType;
            this.resourceId = resourceId;
            this.language = language;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
                return true;
            if (!(o instanceof CachedResource))
                return false;

            CachedResource that = (CachedResource) o;

            if (resourceType != null ? !resourceType.equals(that.resourceType) : that.resourceType != null)
                return false;
            if (resourceId != null ? !resourceId.equals(that.resourceId) : that.resourceId != null)
                return false;
            return language != null ? language.equals(that.language) : that.language == null;

        }
        @Override
        public int hashCode()
        {
            int result = resourceType != null ? resourceType.hashCode() : 0;
            result = 31 * result + (resourceId != null ? resourceId.hashCode() : 0);
            result = 31 * result + (language != null ? language.hashCode() : 0);
            return result;
        }
    }
}
