package com.beligum.blocks.endpoints;

import com.beligum.base.i18n.I18nFactory;
import com.beligum.base.utils.json.Json;
import com.beligum.base.utils.xml.XML;
import com.beligum.blocks.config.RdfFactory;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.endpoints.ifaces.AutocompleteSuggestion;
import com.beligum.blocks.endpoints.ifaces.AutocompleteValue;
import com.beligum.blocks.endpoints.beans.AbstractGeoname;
import com.beligum.blocks.endpoints.beans.GeonameResource;
import com.beligum.blocks.endpoints.beans.ResourceSuggestion;
import com.beligum.blocks.fs.index.entries.IndexEntry;
import com.beligum.blocks.fs.index.entries.PageIndexEntry;
import com.beligum.blocks.fs.index.ifaces.PageIndexConnection;
import com.beligum.blocks.rdf.ontology.Classes;
import com.beligum.blocks.security.Permissions;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.lucene.search.BooleanClause;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.glassfish.jersey.client.ClientConfig;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
 * Created by bram on 2/25/16.
 */
@Path("/blocks/admin/rdf")
public class RdfEndpoint
{
    //-----CONSTANTS-----
    private static final String GEONAMES_USERNAME = "beligum";

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @GET
    @Path("/properties/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresRoles(Permissions.ADMIN_ROLE_NAME)
    public Response getProperties() throws IOException
    {
        return Response.ok(RdfFactory.getProperties()).build();
    }

    @GET
    @Path("/classes/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresRoles(Permissions.ADMIN_ROLE_NAME)
    public Response getClasses() throws IOException
    {
        return Response.ok(RdfFactory.getClasses()).build();
    }

    @GET
    @Path("/resources/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresRoles(Permissions.ADMIN_ROLE_NAME)
    public Response getResources(@QueryParam("resourceTypeCurie") URI resourceTypeCurie, @QueryParam("query") String query) throws IOException
    {
        //tested with client side: ok
        List<AutocompleteSuggestion> retVal = null;

        Locale requestLocale = I18nFactory.instance().getOptimalLocale();

        //if the requested type is inside our own ontology, search the title and urls of the local site pages
        if (resourceTypeCurie.getScheme().equals(Settings.instance().getRdfOntologyPrefix())) {
            if (resourceTypeCurie.equals(Classes.Country.getCurieName())) {
                retVal = this.searchGeonamesOrg(resourceTypeCurie, query, AbstractGeoname.Type.COUNTRY, requestLocale, 10);
            }
            else if (resourceTypeCurie.equals(Classes.City.getCurieName())) {
                retVal = this.searchGeonamesOrg(resourceTypeCurie, query, AbstractGeoname.Type.CITY, requestLocale, 10);
            }
            else {
                PageIndexConnection.FieldQuery[] queries =
                                new PageIndexConnection.FieldQuery[] { new PageIndexConnection.FieldQuery(PageIndexEntry.Field.typeOf, resourceTypeCurie.toString(), BooleanClause.Occur.FILTER,
                                                                                                          PageIndexConnection.FieldQuery.Type.EXACT, null),
                                                                       new PageIndexConnection.FieldQuery(IndexEntry.Field.tokenisedId, query, BooleanClause.Occur.SHOULD,
                                                                                                          PageIndexConnection.FieldQuery.Type.WILDCARD, 1),
                                                                       new PageIndexConnection.FieldQuery(PageIndexEntry.Field.title, query, BooleanClause.Occur.SHOULD,
                                                                                                          PageIndexConnection.FieldQuery.Type.WILDCARD, 1) };

                List<PageIndexEntry> matchingPages = StorageFactory.getMainPageIndexer().connect().search(queries, 10);
                //TODO this iterates and re-packages the results yet another time -> avoid this
                for (PageIndexEntry page : matchingPages) {
                    //Note: the ID of a page is also it's public address
                    retVal.add(new ResourceSuggestion(page.getId(), resourceTypeCurie, page.getTitle(), page.getId().getPath()));
                }
            }
        }

        return Response.ok(retVal).build();
    }
    @GET
    @Path("/resource/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresRoles(Permissions.ADMIN_ROLE_NAME)
    public Response getResource(@QueryParam("resourceTypeCurie") URI resourceTypeCurie, @QueryParam("resourceUri") URI resourceUri) throws IOException
    {
        //tested with client side: ok
        AutocompleteValue retVal = null;

        Locale requestLocale = I18nFactory.instance().getOptimalLocale();

        //if the requested type is inside our own ontology, search the title and urls of the local site pages
        if (resourceTypeCurie.getScheme().equals(Settings.instance().getRdfOntologyPrefix())) {
            if (resourceTypeCurie.equals(Classes.Country.getCurieName()) || resourceTypeCurie.equals(Classes.City.getCurieName())) {
                retVal = this.getGeonamesOrg(resourceTypeCurie, AbstractGeoname.fromGeonamesUri(resourceUri), requestLocale);
            }
            else {
                //TODO
            }
        }

        return Response.ok(retVal).build();
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private List<AutocompleteSuggestion> searchGeonamesOrg(URI resourceTypeCurie, String query, AbstractGeoname.Type type, Locale language, int maxResults) throws IOException
    {
        List<AutocompleteSuggestion> retVal = new ArrayList<>();

        ClientConfig config = new ClientConfig();
        Client httpClient = ClientBuilder.newClient(config);
        UriBuilder builder = UriBuilder.fromUri("http://api.geonames.org/search")
                                       .queryParam("username", GEONAMES_USERNAME)
                                       .queryParam("name_startsWith", query)
                                       .queryParam("featureClass", type.featureClass)
                                       //no need to fetch the entire node; we'll do that during selection
                                       //note: we selct MEDIUM instead of SHORT to get the full country name (for cities)
                                       .queryParam("style", "MEDIUM")
                                       .queryParam("maxRows", maxResults)
                                       .queryParam("type", "json");

        if (language != null) {
            builder.queryParam("lang", language.getLanguage());
        }

        URI target = builder.build();
        Response response = httpClient.target(target).request(MediaType.APPLICATION_JSON).get();
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            JsonNode jsonNode = Json.getObjectMapper().readTree(response.readEntity(String.class));
            Iterator<JsonNode> geonames = jsonNode.path("geonames").elements();

            InjectableValues inject = new InjectableValues.Std().addValue(AbstractGeoname.RESOURCE_TYPE_INJECTABLE, resourceTypeCurie);
            ObjectReader reader = Json.getObjectMapper().readerFor(type.suggestionClass).with(inject);

            while (geonames.hasNext()) {
                retVal.add((AutocompleteSuggestion) reader.readValue(geonames.next()));
            }
        }
        else {
            throw new IOException("Error status returned while searching for geonames resource '" + query + "'; " + response);
        }

        return retVal;
    }
    private GeonameResource getGeonamesOrg(URI resourceTypeCurie, String geonameId, Locale language) throws IOException
    {
        GeonameResource retVal = null;

        ClientConfig config = new ClientConfig();
        Client httpClient = ClientBuilder.newClient(config);
        UriBuilder builder = UriBuilder.fromUri("http://api.geonames.org/get")
                                       .queryParam("username", GEONAMES_USERNAME)
                                       .queryParam("geonameId", geonameId)
                                       .queryParam("style", "FULL")
                                       .queryParam("type", "json");

        if (language != null) {
            builder.queryParam("lang", language.getLanguage());
        }

        URI target = builder.build();
        Response response = httpClient.target(target).request(MediaType.APPLICATION_JSON).get();
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {

            InjectableValues inject = new InjectableValues.Std().addValue(AbstractGeoname.RESOURCE_TYPE_INJECTABLE, resourceTypeCurie);
            ObjectReader reader = XML.getObjectMapper().readerFor(GeonameResource.class).with(inject);

            //note: the '/get' endpoint is XML only!
            retVal = reader.readValue(response.readEntity(String.class));
        }
        else {
            throw new IOException("Error status returned while searching for geonames id '" + geonameId + "'; " + response);
        }

        return retVal;
    }

    //-----INNER CLASSES-----
    public class ResourceValue
    {
        public URI resource;
        public String text;
        public URI link;

        public ResourceValue(URI resource, String text)
        {
            this(resource, text, null);
        }
        public ResourceValue(URI resource, String text, URI link)
        {
            this.resource = resource;
            this.text = text;
            this.link = link;
        }
    }
}
