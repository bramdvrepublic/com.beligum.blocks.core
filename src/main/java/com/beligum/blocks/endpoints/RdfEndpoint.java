package com.beligum.blocks.endpoints;

import com.beligum.blocks.rdf.ontology.Term;
import com.beligum.blocks.rdf.ontology.Terms;
import com.beligum.blocks.security.Permissions;
import org.apache.shiro.authz.annotation.RequiresRoles;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

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
        Set<Term> properties = new HashSet<>();

        properties.add(Terms.postalCode);
        properties.add(Terms.isVerified);

        return Response.ok(properties).build();
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----INNER CLASSES-----
}
