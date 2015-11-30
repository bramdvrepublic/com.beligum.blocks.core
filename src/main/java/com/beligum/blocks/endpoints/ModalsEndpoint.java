package com.beligum.blocks.endpoints;

import com.beligum.blocks.security.Permissions;
import org.apache.shiro.authz.annotation.RequiresRoles;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;

/**
 * Created by bas on 18.02.15.
 */
@Path("/modals")
public class ModalsEndpoint
{
    @GET
    @Path("/changeurl")
    @RequiresRoles(Permissions.ADMIN_ROLE_NAME)
    public Response getChangeUrlModal(
                    @QueryParam("original")
                    String originalUrl) throws MalformedURLException
    {
        //        Template template = change_url_modal.get().getNewTemplate();
        //        String originalPath = new URL(originalUrl).getPath();
        //        String[] splitted = originalPath.split("/");
        //        if (splitted.length > 2) {
        //            template.set("originalUrlPathEnd", "/" + splitted[splitted.length - 1]);
        //        }
        //        else if (splitted.length == 2) {
        //            //            if(Languages.isNonEmptyLanguageCode(splitted[1])){
        //            //                template.set("originalUrlPathEnd", "");
        //            //            }
        //            //            else{
        //            //                template.set("originalUrlPathEnd", "/" + splitted[splitted.length - 1]);
        //            //            }
        //        }
        //        else {
        //            template.set("originalUrlPathEnd", "");
        //        }
        //        template.set("languages", Arrays.asList(BlocksConfig.instance().getLanguages()));
        return Response.ok().build();
    }

}
