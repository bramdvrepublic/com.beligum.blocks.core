package com.beligum.blocks.core.endpoints;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.usermanagement.Permissions;
import com.beligum.core.framework.base.R;
import com.beligum.core.framework.templating.ifaces.Template;
import org.apache.commons.io.FilenameUtils;
import org.apache.shiro.authz.annotation.RequiresRoles;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by bas on 18.02.15.
 */
@Path("/modals")
public class ModalsEndpoint
{
    public static final String TRANSLATION_MODAL = "translationModal.vm";

    @GET
    @Path("/translation")
    @RequiresRoles(Permissions.ADMIN_ROLE_NAME)
    public Response getModalView(
                    @PathParam("name")
                    String name,
                    @QueryParam("original")
                    String originalUrl)
    {
        Template template = R.templateEngine().getEmptyTemplate("/views/modals/" + TRANSLATION_MODAL);
        template.set("originalUrl", originalUrl);
        template.set("languages", Arrays.asList(BlocksConfig.getLanguages()));
        return Response.ok(template.render()).build();
    }
}
