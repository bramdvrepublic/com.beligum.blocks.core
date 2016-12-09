package com.beligum.blocks.endpoints;

import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.RdfFactory;
import com.beligum.blocks.endpoints.ifaces.AutocompleteSuggestion;
import com.beligum.blocks.endpoints.ifaces.RdfQueryEndpoint;
import com.beligum.blocks.endpoints.ifaces.ResourceInfo;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ontology.RdfClassImpl;
import com.beligum.blocks.rdf.ontology.vocabularies.endpoints.SettingsQueryEndpoint;
import com.beligum.blocks.security.Permissions;
import gen.com.beligum.blocks.core.messages.blocks.core;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresRoles;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import static gen.com.beligum.blocks.core.constants.blocks.core.*;

/**
 * Created by bram on 2/25/16.
 */
@Path("/blocks/admin/rdf")
public class RdfEndpoint
{
    //-----CONSTANTS-----
    //Note: the null-valued vocabulary indicates a dummy class to support search-all functionality
    public static final RdfClass SEARCH_ALL = new RdfClassImpl("All",
                                                               null,
                                                               core.Entries.searchClassAllTitle,
                                                               core.Entries.searchClassAllLabel,
                                                               new URI[] {},
                                                               false,
                                                               new SettingsQueryEndpoint(),
                                                               null);

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @GET
    @Path("/classes/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresRoles(Permissions.ADMIN_ROLE_NAME)
    public Response getClasses() throws IOException
    {
        return Response.ok(RdfFactory.getClasses()).build();
    }

    @GET
    @Path("/properties/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresRoles(Permissions.ADMIN_ROLE_NAME)
    public Response getProperties(@QueryParam(RDF_RES_TYPE_CURIE_PARAM) URI resourceTypeCurie) throws IOException
    {
        Set<RdfProperty> retVal = null;

        if (resourceTypeCurie != null) {
            RdfClass rdfClass = RdfFactory.getClassForResourceType(resourceTypeCurie);
            if (rdfClass != null) {
                Set<RdfProperty> classProps = rdfClass.getProperties();
                //note that the javadoc of getProperties() says that we returns all properties if this returns null (which will be true later on),
                // but if it returns the empty array, no properties should be returned.
                if (classProps != null) {
                    retVal = classProps;
                }
            }
        }

        //if nothing happened, we just return all properties known to this classpath
        if (retVal == null) {
            retVal = RdfFactory.getProperties();
        }

        return Response.ok(retVal).build();
    }

    @GET
    @Path("/resources/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresRoles(Permissions.ADMIN_ROLE_NAME)
    //Note: the "query" parameter needs to be last, because the JS side just appends the query string to this URL
    public Response getResources(@QueryParam(RDF_RES_TYPE_CURIE_PARAM) URI resourceTypeCurie, @QueryParam(RDF_MAX_RESULTS_PARAM) int maxResults,
                                 @QueryParam(RDF_PREFIX_SEARCH_PARAM) @DefaultValue("true") boolean prefixSearch, /* keep this last */@QueryParam(RDF_QUERY_PARAM) String query) throws IOException
    {
        Collection<AutocompleteSuggestion> retVal = new ArrayList<>();

        //support a search-all-types-query when the type is empty
        RdfClass rdfClass = null;
        if (resourceTypeCurie == null || StringUtils.isEmpty(resourceTypeCurie.toString())) {
            rdfClass = SEARCH_ALL;
        }
        else {
            rdfClass = RdfFactory.getClassForResourceType(resourceTypeCurie);
        }

        if (rdfClass != null) {
            RdfQueryEndpoint endpoint = rdfClass.getEndpoint();
            if (endpoint != null) {
                RdfQueryEndpoint.QueryType queryType = RdfQueryEndpoint.QueryType.FULL;
                if (prefixSearch) {
                    queryType = RdfQueryEndpoint.QueryType.STARTS_WITH;
                }

                retVal = endpoint.search(rdfClass == SEARCH_ALL ? null : rdfClass, query, queryType, R.i18nFactory().getOptimalRefererLocale(), maxResults);
            }
        }
        else {
            Logger.warn("Encountered unknown resource type; " + resourceTypeCurie);
        }

        return Response.ok(retVal).build();
    }

    @GET
    @Path("/resource/")
    @Produces(MediaType.APPLICATION_JSON)
    //We disabled this because some javascript initialization code needs to access it,
    // and I think it's not really a security issue. Let's hope I'm right...
    //@RequiresRoles(Permissions.ADMIN_ROLE_NAME)
    public Response getResource(@QueryParam(RDF_RES_TYPE_CURIE_PARAM) URI resourceTypeCurie, @QueryParam(RDF_RES_URI_PARAM) URI resourceUri) throws IOException
    {
        ResourceInfo retVal = null;

        RdfClass rdfClass = RdfFactory.getClassForResourceType(resourceTypeCurie);
        if (rdfClass != null) {
            RdfQueryEndpoint endpoint = rdfClass.getEndpoint();
            if (endpoint != null) {
                retVal = endpoint.getResource(rdfClass, resourceUri, R.i18nFactory().getOptimalRefererLocale());
            }
        }
        else {
            Logger.warn("Encountered unknown resource type; " + resourceTypeCurie);
        }

        if (retVal == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        else {
            return Response.ok(retVal).build();
        }
    }

    @GET
    @Path("/search/classes/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresRoles(Permissions.ADMIN_ROLE_NAME)
    public Response getSearchClasses() throws IOException
    {
        Set<RdfClass> retVal = RdfFactory.getClasses();
        retVal.add(SEARCH_ALL);
        return Response.ok(retVal).build();
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
