package com.beligum.blocks.rdf.ontology.vocabularies.endpoints;

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
import org.glassfish.jersey.client.ClientConfig;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Created by bram on 3/14/16.
 */
public class GeonameQueryEndpoint implements RdfQueryEndpoint
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private String username;
    private AbstractGeoname.Type geonameType;

    //-----CONSTRUCTORS-----
    public GeonameQueryEndpoint(AbstractGeoname.Type geonameType)
    {
        this.username = Settings.instance().getGeonamesUsername();
        this.geonameType = geonameType;
    }

    //-----PUBLIC METHODS-----
    @Override
    public List<AutocompleteSuggestion> search(RdfClass resourceType, final String query, QueryType queryType, Locale language, int maxResults, SearchOption... options) throws IOException
    {
        List<AutocompleteSuggestion> retVal = new ArrayList<>();

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
                throw new IOException("Unsupported or unimplemented query type encountered, can't proceed; "+queryType);
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

        return retVal;
    }
    @Override
    public ResourceInfo getResource(RdfClass resourceType, URI resourceId, Locale language) throws IOException
    {
        GeonameResourceInfo retVal = null;

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

}
