/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import com.beligum.blocks.rdf.ifaces.Format;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.importers.SesameImporter;
import com.beligum.blocks.rdf.ontology.vocabularies.GN;
import com.beligum.blocks.rdf.ontology.vocabularies.geo.AbstractGeoname;
import com.beligum.blocks.rdf.ontology.vocabularies.geo.GeonameResourceInfo;
import com.beligum.blocks.utils.RdfTools;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.beligum.blocks.caching.CacheKeys.GEONAMES_CACHED_RESULTS;
import static com.beligum.blocks.rdf.ontology.factories.Classes.City;

/**
 * Created by bram on 3/14/16.
 */
public class GeonameQueryEndpoint implements RdfQueryEndpoint
{
    //-----CONSTANTS-----
    private static final Pattern CITY_ZIP_COUNTRY_PATTERN = Pattern.compile("([^,]*),(\\d+),(.*)");

    //Note: don't make this static; it messes with the RdfFactory initialization
    //Also: don't initialize it in the constructor; it suffers from the same problem
    private RdfProperty[] cachedLabelProps;

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

    @Override
    public boolean isExternal()
    {
        return true;
    }
    //-----PUBLIC METHODS-----
    @Override
    //Note: check the inner cache class if you add variables
    public Collection<AutocompleteSuggestion> search(RdfClass resourceType, final String query, QueryType queryType, Locale language, int maxResults, SearchOption... options) throws IOException
    {
        Collection<AutocompleteSuggestion> retVal = new ArrayList<>();

        //I guess an empty query can't yield any results, right?
        if (!StringUtils.isEmpty(query)) {

            //use a cached result if it's there
            CachedSearch cacheKey = new CachedSearch(this.geonameType, resourceType, query, queryType, language, options);
            Collection<AutocompleteSuggestion> cachedResult = this.getCachedEntry(cacheKey);

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
                                               //note: we select MEDIUM instead of SHORT to get the full country name (for cities)
                                               //note: we select FULL instead of MEDIUM to get the parents-names to disambiguate in GeonameCitySuggestion.getSubTitle()
                                               .queryParam("style", "FULL")
                                               .queryParam("maxRows", maxResults)
                                               //I think the default is "population", which seems to be more natural
                                               // (better to find a large, more-or-less-good match, than to find the very specific wrong match)
                                               //can be any of [population,elevation,relevance]
                                               //Note: reverted to relevance (inspired by eg. Tielt-Winge, who kept on suggesting Houwaart because it has a higher population)
                                               //Note 2: reverted to nothing (by the way, population doesn't seem to be the default) because of "Ger" in Normandy;
                                               //        impossible to find in combination with name_startsWith (note: the API docs seem to say the orderby is only relevant for name_startsWith)
                                               //.queryParam("orderby", "relevance")
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

                //if we didn't find any result, use some heuristics to search a little deeper...
                if (retVal.isEmpty()) {
                    if (resourceType.equals(City)) {
                        retVal = this.deeperCitySearch(httpClient, resourceType, query, queryType, language, maxResults, options);
                    }
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
                Client httpClient = null;
                try {
                    httpClient = ClientBuilder.newClient(new ClientConfig());

                    String geonamesId = RdfTools.extractResourceId(resourceId);
                    UriBuilder builder = UriBuilder.fromUri("http://api.geonames.org/get")
                                                   .queryParam("username", this.username)
                                                   //we pass only the id, not the entire URI
                                                   .queryParam("geonameId", geonamesId)
                                                   //when we query, we query for a lot
                                                   .queryParam("style", "FULL")
                                                   .queryParam("type", "json");

                    if (language != null) {
                        builder.queryParam("lang", language.getLanguage());
                    }

                    //Logger.info("Requesting "+builder.build());
                    Response response = httpClient.target(builder.build()).request(MediaType.APPLICATION_JSON).get();
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
                finally {
                    if (httpClient != null) {
                        httpClient.close();
                    }
                }
            }
        }

        return retVal;
    }
    @Override
    public RdfProperty[] getLabelCandidates(RdfClass localResourceType)
    {
        if (this.cachedLabelProps == null) {
            this.cachedLabelProps = new RdfProperty[] { GN.officialName, GN.name, GN.alternateName };
        }

        return this.cachedLabelProps;
    }
    @Override
    public URI getExternalResourceId(URI resourceId, Locale language)
    {
        //TODO do we need the language?
        return AbstractGeoname.toGeonamesUri(RdfTools.extractResourceId(resourceId));
    }
    @Override
    public Model getExternalRdfModel(RdfClass resourceType, URI resourceId, Locale language) throws IOException
    {
        Model retVal = null;

        if (resourceId != null && !resourceId.toString().isEmpty()) {

            //use a cached result if it's there
            CachedExternalModel cacheKey = new CachedExternalModel(resourceType, resourceId, language);
            Model cachedResult = this.getCachedEntry(cacheKey);
            if (cachedResult != null) {
                retVal = cachedResult;
            }
            else {
                Client httpClient = null;

                try {
                    httpClient = ClientBuilder.newClient(new ClientConfig());

                    //Note: when opened in the browser, this will actually redirect to an "about.rdf" page,
                    // but this is the main subject of our RDF we'll use in the SPARQL below too
                    URI rdfUri = this.getExternalResourceId(resourceId, language);

                    //this seems to be the only RDF format that's accepted
                    //TODO the geonames endpoint is limited to 2000 requests per hour, we should probably throttle this
                    Response response = httpClient.target(rdfUri).request("application/rdf+xml").get();
                    if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                        //this will hold subjects about the resource and it's relationship with the about page,
                        //but we're only interested in the raw resource statements, so filter out the rest
                        //because it makes more sense: we're asking for a specific resource,
                        //so it's normal all statements have that resource as subject
                        Model remoteModel = new SesameImporter(Format.RDF_XML).importDocument(rdfUri, new ByteArrayInputStream(response.readEntity(String.class).getBytes()));

                        // For debug
                        //                Exporter exporter = new SesameExporter(Format.NTRIPLES);
                        //                ByteArrayOutputStream exportData = new ByteArrayOutputStream();
                        //                exporter.exportModel(remoteModel, exportData);
                        //                Logger.info(exportData);

                        //if we get here, it means we have a model, empty or not
                        retVal = new LinkedHashModel();

                        String rdfUriStr = rdfUri.toString();
                        for (Statement statement : remoteModel) {
                            //Filter only those statements about the external resource id returned by getExternalResourceId()
                            //Note that this works together with the getExternalResourceId() method,
                            //so it's possible to query the triplestore using the return value of that method
                            if (statement.getSubject().stringValue().equals(rdfUriStr)) {
                                retVal.add(statement);
                            }
                        }
                    }
                    else {
                        throw new IOException("Geonames RDF endpoint returned unexpected http code (" + response.getStatus() + ") when fetching dependency model for " + resourceId + "; " +
                                              response.getStatusInfo().getReasonPhrase());
                    }

                    this.putCachedEntry(cacheKey, retVal);
                }
                finally {
                    if (httpClient != null) {
                        httpClient.close();
                    }
                }
            }
        }

        return retVal;
    }
    @Override
    public RdfClass getExternalClasses(RdfClass localResourceType)
    {
        return GN.Feature;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private synchronized Collection<AutocompleteSuggestion> getCachedEntry(CachedSearch query)
    {
        return (Collection<AutocompleteSuggestion>) this.getGeonameCache().get(query);
    }
    private synchronized void putCachedEntry(CachedSearch key, Collection<AutocompleteSuggestion> results)
    {
        this.getGeonameCache().put(key, results);
    }
    private synchronized GeonameResourceInfo getCachedEntry(CachedResource query)
    {
        return (GeonameResourceInfo) this.getGeonameCache().get(query);
    }
    private synchronized void putCachedEntry(CachedResource key, GeonameResourceInfo results)
    {
        this.getGeonameCache().put(key, results);
    }
    private synchronized Model getCachedEntry(CachedExternalModel externalModel)
    {
        return (Model) this.getGeonameCache().get(externalModel);
    }
    private synchronized void putCachedEntry(CachedExternalModel key, Model model)
    {
        this.getGeonameCache().put(key, model);
    }
    private synchronized Cache getGeonameCache()
    {
        if (!R.cacheManager().cacheExists(GEONAMES_CACHED_RESULTS.name())) {
            //we instance a cache where it's entries live for one hour (both from creation time as from last accessed time),
            //doesn't overflow to disk and keep at most 100 results
            R.cacheManager().registerCache(new EhCacheAdaptor(GEONAMES_CACHED_RESULTS.name(), 100, false, false, 60 * 60, 60 * 60));
        }

        return R.cacheManager().getCache(GEONAMES_CACHED_RESULTS.name());
    }
    /**
     * Geonames allows for search queries like 'Valkenburg,6305,Netherlands' where the postalCode is specified to disambiguate between places with the same name,
     * but from time to time, the name of the city doesn't match with the official name of the city for that specific postal code.
     * This method allows us to use queries like the one above, and use the Geonames postalCodeSearch endpoint to query the official name of that city.
     * Note that we have to do a two-step search because the postalCodeSearch doesn't seem to return the geoname ID...
     */
    private Collection<AutocompleteSuggestion> deeperCitySearch(Client httpClient, RdfClass resourceType, final String query, QueryType queryType, Locale language, int maxResults,
                                                                SearchOption... options)
    {
        Collection<AutocompleteSuggestion> retVal = new ArrayList<>();

        try {
            //this format allows us to specify an exact zip code, but if the known name doesn't match
            Matcher matcher = CITY_ZIP_COUNTRY_PATTERN.matcher(query);
            if (matcher.matches() && matcher.groupCount() == 3) {
                String city = matcher.group(1);
                String zipCode = matcher.group(2);
                String country = matcher.group(3);

                //TODO fast and silly first implementation for our specific needs
                //see http://www.geonames.org/countries/
                String countryCode = null;
                if (country.equals("Belgium")) {
                    countryCode = "BE";
                }
                else if (country.equals("Netherlands")) {
                    countryCode = "NL";
                }
                else if (country.equals("France")) {
                    countryCode = "FR";
                }
                else if (country.equals("Germany")) {
                    countryCode = "DE";
                }
                else if (country.equals("United Kingdom")) {
                    countryCode = "GB";
                }
                else if (country.equals("Hungary")) {
                    countryCode = "HU";
                }

                if (countryCode != null) {
                    Logger.info("Didn't find any Geonames city result for '" + query + "', searching a bit deeper because we have a postal code.");

                    UriBuilder builder = UriBuilder.fromUri("http://api.geonames.org/postalCodeSearch")
                                                   .queryParam("username", this.username)
                                                   //.queryParam("postalcode", zipCode)
                                                   .queryParam("placename", city)
                                                   .queryParam("country", countryCode)
                                                   //reasonable value?
                                                   .queryParam("maxRows", 20)
                                                   .queryParam("type", "json");
                    if (language != null) {
                        builder.queryParam("lang", language.getLanguage());
                    }

                    String placeName = null;
                    URI target = builder.build();
                    Response response = httpClient.target(target).request(MediaType.APPLICATION_JSON).get();
                    if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                        JsonNode jsonNode = Json.getObjectMapper().readTree(response.readEntity(String.class));
                        Iterator<JsonNode> postalCodes = jsonNode.path("postalCodes").elements();
                        //we're only searching for one
                        while (postalCodes.hasNext()) {
                            JsonNode pc = postalCodes.next();
                            JsonNode postalCodeNode = pc.get("postalCode");
                            JsonNode placeNameNode = pc.get("placeName");
                            if (postalCodeNode != null && placeNameNode != null && postalCodeNode.textValue().equals(zipCode)) {
                                placeName = placeNameNode.textValue();
                            }
                        }
                    }

                    if (placeName != null) {
                        //use the new, queried name to search again
                        //Note that this is a source for infinite recursion if this query doesn't yield any results, but it should, cause we just looked it up (but will be using a different endpoint during search())
                        //Update: encountered a lot of errors when including the zipCode again, omitting it and hoping for the best...
                        //String newQuery = placeName+","+zipCode+","+countryCode;
                        String newQuery = placeName + "," + countryCode;
                        retVal = this.search(resourceType, newQuery, queryType, language, maxResults, options);
                    }
                }
                else {
                    Logger.warn("Unknown country '" + country + "'; can't translate it to a country code and can't use deeper search");
                }

                //as a last try, we omit the postal code and re-launch the search query one more time without postal code
                if (retVal.isEmpty()) {
                    String newQuery = city + "," + country;
                    retVal = this.search(resourceType, newQuery, queryType, language, maxResults, options);
                }
            }
        }
        //don't let this additional search ruin the query, log and eat it
        catch (Exception e) {
            Logger.error("Error happened while search a bit deeper for a city resource for '" + query + "'; ", e);
        }

        return retVal;
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
            if (this == o) {
                return true;
            }
            if (!(o instanceof CachedSearch)) {
                return false;
            }

            CachedSearch that = (CachedSearch) o;

            if (geonameType != that.geonameType) {
                return false;
            }
            if (resourceType != null ? !resourceType.equals(that.resourceType) : that.resourceType != null) {
                return false;
            }
            if (query != null ? !query.equals(that.query) : that.query != null) {
                return false;
            }
            if (queryType != that.queryType) {
                return false;
            }
            if (language != null ? !language.equals(that.language) : that.language != null) {
                return false;
            }
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
            if (this == o) {
                return true;
            }
            if (!(o instanceof CachedResource)) {
                return false;
            }

            CachedResource that = (CachedResource) o;

            if (resourceType != null ? !resourceType.equals(that.resourceType) : that.resourceType != null) {
                return false;
            }
            if (resourceId != null ? !resourceId.equals(that.resourceId) : that.resourceId != null) {
                return false;
            }
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

    private static class CachedExternalModel
    {
        private RdfClass resourceType;
        private URI resourceId;
        private Locale language;

        public CachedExternalModel(RdfClass resourceType, URI resourceId, Locale language)
        {
            this.resourceType = resourceType;
            this.resourceId = resourceId;
            this.language = language;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) {
                return true;
            }
            if (!(o instanceof CachedExternalModel)) {
                return false;
            }

            CachedExternalModel that = (CachedExternalModel) o;

            if (resourceType != null ? !resourceType.equals(that.resourceType) : that.resourceType != null) {
                return false;
            }
            if (resourceId != null ? !resourceId.equals(that.resourceId) : that.resourceId != null) {
                return false;
            }
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
