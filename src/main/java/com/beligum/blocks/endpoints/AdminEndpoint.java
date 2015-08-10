package com.beligum.blocks.endpoints;

import com.beligum.base.security.PermissionsConfigurator;
import com.beligum.base.server.R;
import com.beligum.base.validation.messages.DefaultFeedbackMessage;
import com.beligum.base.validation.messages.FeedbackMessage;
import com.beligum.blocks.templating.blocks.HtmlParser;
import org.apache.shiro.authz.annotation.RequiresRoles;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.net.URI;

@Path("/admin")
@RequiresRoles(PermissionsConfigurator.ADMIN_ROLE_NAME)
public class AdminEndpoint
{
    private static final String RESET_TEMPLATES = "templates";

    @Path("/reset/{type:.*}")
    @GET
    public Response reset(@PathParam("type") String type)
    {
        switch (type) {
            case RESET_TEMPLATES:
                HtmlParser.resetTemplateCache();
                R.cacheManager().getFlashCache().addMessage(new DefaultFeedbackMessage(FeedbackMessage.Level.SUCCESS, "Template cache reset successfully"));
                break;
            default:
                throw new NotFoundException("Supplied action '"+type+"' not found");
        }

        return Response.seeOther(URI.create("/")).build();
    }
}