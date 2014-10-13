package com.beligum.blocks.core.endpoints;

import com.beligum.core.framework.base.R;
import com.beligum.core.framework.templating.ifaces.Template;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("/")
public class ApplicationEndpoint
{
    @Path("/")
    @GET
    public Response index()
    {
        Template indexTemplate = R.templateEngine().getEmptyTemplate("/views/index.html");

        return Response.ok(indexTemplate.render()).build();
    }

    @Path("/{pageClassName}/{pageId}")
    @GET
    public Response getPageWithId(@PathParam("pageClassName") String pageClassName, @PathParam("pageId") String pageId){
        return Response.ok("Hello World!").build();
    }
}