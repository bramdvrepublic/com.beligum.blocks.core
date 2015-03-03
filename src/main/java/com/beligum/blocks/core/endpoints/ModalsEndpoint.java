package com.beligum.blocks.core.endpoints;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.usermanagement.Permissions;
import com.beligum.core.framework.base.R;
import com.beligum.core.framework.templating.ifaces.Template;
import org.apache.shiro.authz.annotation.RequiresRoles;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

/**
 * Created by bas on 18.02.15.
 */
@Path("/modals")
public class ModalsEndpoint
{
    public static final String CHANGE_URL_MODAL = "changeUrlModal.vm";

    @GET
    @Path("/changeurl")
    @RequiresRoles(Permissions.ADMIN_ROLE_NAME)
    public Response getModalView(
                    @PathParam("name")
                    String name,
                    @QueryParam("original")
                    String originalUrl) throws MalformedURLException
    {
        Template template = R.templateEngine().getEmptyTemplate("/views/modals/" + CHANGE_URL_MODAL);
        String originalPath = new URL(originalUrl).getPath();
        String [] splitted = originalPath.split("/");
        if(splitted.length>0) {
            template.set("originalUrlPathEnd", "/" + splitted[splitted.length - 1]);
        }
        else{
            template.set("originalUrlPathEnd", "");
        }
        template.set("languages", Arrays.asList(BlocksConfig.getLanguages()));
        return Response.ok(template.render()).build();
    }
}
