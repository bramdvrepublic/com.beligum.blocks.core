package com.beligum.blocks.endpoints;

import com.beligum.base.resources.RegisteredMimeType;
import com.beligum.base.resources.ifaces.Resource;
import com.beligum.base.server.R;
import com.beligum.blocks.templating.blocks.HtmlParser;
import gen.com.beligum.blocks.core.fs.html.views.snippets.side;
import org.apache.commons.io.FilenameUtils;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

/**
 * Created by bas on 21.10.14.
 * Class extending assets-endpoint to enable template-loading from the "templates"-package instead of from the "assets"-package
 */
@Path(TemplatesEndpoint.ENDPOINT_PREFIX)
public class TemplatesEndpoint
{
    protected static final String ENDPOINT_PREFIX = "/templates";

    @GET
    @Path("/sidebar")
    public Response getSidebar()
    {
        return Response.ok(side.get()).build();
    }

    @GET
    //note: the .less is needed to use @include in other less files
    @Path("/styles/imports/reset.css")
    public Response getResources()
    {
        return Response.ok(HtmlParser.getTemplateCache().getCssReset(), RegisteredMimeType.CSS.getMimeType().toString()).build();
    }

    @GET
    //note: the .less is needed to use @include in other less files
    @Path("/scripts/imports/all.js")
    public Response getImportsArray()
    {
        return Response.ok(HtmlParser.getTemplateCache().getJsArray(), RegisteredMimeType.JAVASCRIPT.getMimeType().toString()).build();
    }

    /**
     * Enables us to eg. load the main template without using the GeneratedFile interface by loading "/templates/main.html" in a universal (change-proof) way
     *
     * @param name
     * @return
     * @throws Exception
     */
    @GET
    @Path("/{name: .*}")
    public Resource getTemplate(@Context final UriInfo uriInfo, @PathParam("name") String name)
    {
        //SECURITY (both the prefix and the normalize)
        final String resourcePath = "/templates/" + FilenameUtils.normalize(name);
        URI requestUri = UriBuilder.fromUri(uriInfo.getRequestUri()).replacePath(resourcePath).build();

        Resource resource = R.resourceManager().get(requestUri);
        if (resource == null) {
            throw new NotFoundException("Resource not found; " + requestUri);
        }
        else {
            return resource;
        }
    }
}
