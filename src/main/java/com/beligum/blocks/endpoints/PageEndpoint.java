package com.beligum.blocks.endpoints;

///**
// * Created by bas on 07.10.14.
// */

import com.beligum.base.server.R;
import com.beligum.blocks.routing.Route;
import com.beligum.blocks.routing.ORouteController;
import com.beligum.blocks.security.Permissions;
import com.beligum.blocks.templating.blocks.HtmlParser;
import com.beligum.blocks.templating.blocks.PageTemplate;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.hibernate.validator.constraints.NotBlank;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

@Path("/blocks/admin/page")
@RequiresRoles(Permissions.ADMIN_ROLE_NAME)
public class PageEndpoint
{
    public static final String PAGE_TEMPLATE_NAME = "pageTemplateName";

    /**
     * Redirect back to the url where the page has to be created
     * We put the name of the pagetemplate in the flashcache
     */
    @GET
    @Path("/template")
    public Response getPageTemplate(
                    @QueryParam("page_url")
                    @NotBlank(message = "No url specified.")
                    String pageUrl,
                    @QueryParam("page_class_name")
                    @NotBlank(message = "No entity-class specified.")
                    String pageTemplateName)
                    throws Exception

    {
        PageTemplate pageTemplate = (PageTemplate) HtmlParser.getCachedTemplates().get(pageTemplateName);
        R.cacheManager().getFlashCache().put(PAGE_TEMPLATE_NAME, pageTemplateName);
        return Response.seeOther(new URI(pageUrl)).build();
    }

    @POST
    @Path("/save/{url:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response savePage(@PathParam("url") String url, String content)
                    throws Exception

    {
        Route route = new Route(new URI(url), ORouteController.instance());

        // parse html
        // 1. get text
        // 2. get filtered html
        // 3. get resources - update new resources with resource-tag
        // 4. get href and src attributes

        // create new WebPage object or fetch existing

        //

//        String x = content.get("html");
        return Response.ok().build();
    }


}
