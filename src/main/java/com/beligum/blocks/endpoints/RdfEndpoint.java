package com.beligum.blocks.endpoints;

import com.beligum.base.i18n.I18nFactory;
import com.beligum.base.server.R;
import com.beligum.blocks.config.RdfFactory;
import com.beligum.blocks.endpoints.ifaces.AutocompleteSuggestion;
import com.beligum.blocks.endpoints.ifaces.AutocompleteValue;
import com.beligum.blocks.endpoints.ifaces.RdfQueryEndpoint;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.security.Permissions;
import org.apache.http.HttpHeaders;
import org.apache.shiro.authz.annotation.RequiresRoles;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
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
    public Response getResources(@QueryParam("resourceTypeCurie") URI resourceTypeCurie, @QueryParam("maxResults") int maxResults, @QueryParam("query") String query) throws IOException
    {
        List<AutocompleteSuggestion> retVal = new ArrayList<>();

        RdfClass rdfClass = RdfFactory.getClassForResourceType(resourceTypeCurie);
        if (rdfClass!=null) {
            RdfQueryEndpoint endpoint = rdfClass.getEndpoint();
            if (endpoint!=null) {
                //note that we can't just use the requested URI because we're in an admin endpoint
                String referer = R.requestContext().getJaxRsRequest().getHeaders().getFirst(HttpHeaders.REFERER);
                if (org.apache.commons.lang.StringUtils.isEmpty(referer)) {
                    throw new IOException("We must have a referer URI to be able to detect the current language; "+referer);
                }
                retVal = endpoint.search(rdfClass, query, I18nFactory.instance().getOptimalLocale(URI.create(referer)), maxResults);
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
        AutocompleteValue retVal = null;

        RdfClass rdfClass = RdfFactory.getClassForResourceType(resourceTypeCurie);
        if (rdfClass!=null) {
            RdfQueryEndpoint endpoint = rdfClass.getEndpoint();
            if (endpoint!=null) {
                retVal = endpoint.getResource(rdfClass, resourceUri, I18nFactory.instance().getOptimalLocale());
            }
        }

        if (retVal==null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        else {
            return Response.ok(retVal).build();
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
