package com.beligum.blocks.endpoints;

import com.beligum.base.utils.Logger;
import com.beligum.base.utils.json.Json;
import com.beligum.blocks.config.RdfFactory;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.fs.index.entries.IndexEntry;
import com.beligum.blocks.fs.index.entries.PageIndexEntry;
import com.beligum.blocks.fs.index.ifaces.PageIndexConnection;
import com.beligum.blocks.rdf.ontology.Classes;
import com.beligum.blocks.security.Permissions;
import com.fasterxml.jackson.databind.JsonNode;
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

/**
 * Created by bram on 2/25/16.
 */
@Path("/blocks/admin/rdf")
public class RdfEndpoint
{
    //-----CONSTANTS-----

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

        //if the requested type is inside our own ontology, search the title and urls of the local site pages
        if (resourceTypeCurie.getScheme().equals(Settings.instance().getRdfOntologyPrefix())) {

            if (resourceTypeCurie.equals(Classes.Country.getCurieName())) {
                ClientConfig config = new ClientConfig();
                Client httpClient = ClientBuilder.newClient(config);
                final String USERNAME = "beligum";
                URI target = UriBuilder.fromUri("http://api.geonames.org/search")
                                       .queryParam("username", USERNAME)
                                       .queryParam("name_startsWith", query)
                                       .queryParam("featureClass", "A")
                                       .queryParam("maxRows", 10)
                                       .queryParam("type", "json")
                                       .build();

                Response response = httpClient.target(target).request(MediaType.APPLICATION_JSON).get();
                if (response.getStatus()==Response.Status.OK.getStatusCode()) {
                    JsonNode jsonNode = Json.getObjectMapper().readTree(response.readEntity(String.class));
                    Iterator<JsonNode> results = jsonNode.path("geonames").elements();
                    retVal = new ArrayList<>();
                    while (results.hasNext()) {
                        JsonNode result = results.next();
                        retVal.add(new AutocompleteSuggestion(result.get("name").asText(), result.get("toponymName").asText(), result.get("countryCode").asText()));
                    }
                }
                else {
                    Logger.error("Error status returned while searching for geonames country '"+query+"'; "+response);;
                }
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
                    retVal.add(new AutocompleteSuggestion(page.getTitle(), page.getId().toString(), page.getId().toString()));
                }
            }
        }

        return Response.ok(retVal).build();
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----INNER CLASSES-----
    public class AutocompleteSuggestion
    {
        public String title;
        public String subTitle;
        public String value;

        public AutocompleteSuggestion(String title, String subTitle, String value)
        {
            this.title = title;
            this.subTitle = subTitle;
            this.value = value;
        }
    }
}
