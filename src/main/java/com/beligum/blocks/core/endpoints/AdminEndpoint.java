package com.beligum.blocks.core.endpoints;

import com.beligum.core.framework.base.R;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by bas on 15.01.15.
 */
@Path("/admin")
public class AdminEndpoint
{
    @GET
    @Path("login")
    @Produces(MediaType.TEXT_HTML)
    public Response getLogin() {
        return Response.ok().entity(R.templateEngine().getEmptyTemplate("/views/users/login.html")).build();
    }
}
