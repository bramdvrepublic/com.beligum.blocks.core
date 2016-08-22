package com.beligum.blocks.endpoints;

import com.beligum.base.resources.ifaces.Parser;
import com.beligum.base.resources.ifaces.Resource;
import com.beligum.base.security.PermissionsConfigurator;
import com.beligum.base.server.R;
import com.beligum.base.validation.messages.DefaultFeedbackMessage;
import com.beligum.base.validation.messages.FeedbackMessage;
import com.beligum.blocks.templating.blocks.HtmlParser;
import org.apache.shiro.authz.annotation.RequiresRoles;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path("/admin")
@RequiresRoles(PermissionsConfigurator.ADMIN_ROLE_NAME)
public class AdminEndpoint
{
    private static final String RESET_TEMPLATES = "templates";
    private static final String EVOLUTION_PAGESTORE = "pagestore";

    @Path("/")
    @GET
    public Response get(@Context HttpServletRequest httpRequest) throws IOException
    {
        if (!httpRequest.getRequestURI().endsWith("/")) {
            //works because of the @Path("/") above
            return Response.seeOther(gen.com.beligum.blocks.endpoints.AdminEndpointRoutes.get().getUri()).build();
        }
        else {
            return Response.ok(gen.com.beligum.blocks.core.fs.html.views.admin.get()).build();
        }
    }

    @Path("/reset/{type:.*}")
    @GET
    public Response reset(@PathParam("type") String type) throws IOException
    {
        switch (type) {
            case RESET_TEMPLATES:
                Parser htmlParser = R.resourceFactory().getParserFor(Resource.MimeType.HTML);
                if (htmlParser instanceof HtmlParser) {
                    //((HtmlParser) htmlParser).resetTemplateCache();

                    //we might as well load it immediately; easier for debugging (note that it's a static function)
                    HtmlParser.getTemplateCache();
                    R.cacheManager().getFlashCache().addMessage(new DefaultFeedbackMessage(FeedbackMessage.Level.SUCCESS, "Template cache reset successfully"));
                }
                else {
                    throw new IOException("Found a HTML parser that's not a HtmlParse; this shouldn't happen" + htmlParser);
                }

                break;

            default:
                throw new NotFoundException("Supplied action '" + type + "' not found");
        }

        return Response.seeOther(gen.com.beligum.blocks.endpoints.AdminEndpointRoutes.get().getUri()).build();
    }
}