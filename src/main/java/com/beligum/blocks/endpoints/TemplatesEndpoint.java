package com.beligum.blocks.endpoints;

import com.beligum.base.annotations.JavascriptPackage;
import com.beligum.base.endpoints.AssetsEndpoint;
import com.beligum.base.resources.Asset;
import com.beligum.base.resources.ResourceDescriptor;
import com.beligum.base.server.R;
import com.beligum.blocks.templating.blocks.HtmlParser;
import gen.com.beligum.blocks.core.fs.html.views.snippets.side;
import org.apache.commons.io.FilenameUtils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

/**
 * Created by bas on 21.10.14.
 * Class extending assets-endpoint to enable template-loading from the "templates"-package instead of from the "assets"-package
 */
@Path("/templates")
public class TemplatesEndpoint extends AssetsEndpoint
{
    @GET
    @Path("/sidebar")
    public Response getSidebar()
    {
        return Response.ok(side.get().getNewTemplate().render()).build();
    }

    @GET
    //note: the .less is needed to use @include in other less files
    @Path("/styles/imports/reset.css")
    public Response getResources()
    {
        return Response.ok(HtmlParser.getTemplateCache().getCssReset(), Asset.MimeType.CSS.getMimeType().toString()).build();
    }

    @GET
    //note: the .less is needed to use @include in other less files
    @Path("/scripts/imports/all.js")
    public Response getImportsArray()
    {
        return Response.ok(HtmlParser.getTemplateCache().getJsArray(), Asset.MimeType.JAVASCRIPT.getMimeType().toString()).build();
    }

    @GET
    @Path("/{name: .*}")
    @JavascriptPackage
    public Response getTemplate(
                    @PathParam("name")
                    String name) throws Exception
    {
        //SECURITY (both the prefix and the normalize)
        String resourcePath = "/templates/" + FilenameUtils.normalize(name);

        ResourceDescriptor resource = R.resourceLoader().getResource(resourcePath, true);

        return loadResource(resource);
    }
}
