package com.beligum.blocks.core.endpoints;

import com.beligum.core.framework.base.R;
import com.beligum.core.framework.templating.ifaces.Template;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
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
    @Path("/demopage")
    @GET
    public Response demo()
    {
        Template indexTemplate = R.templateEngine().getEmptyTemplate("/views/demo.html");

        return Response.ok(indexTemplate.render()).build();
    }
}