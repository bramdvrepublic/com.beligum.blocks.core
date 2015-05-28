package com.beligum.blocks.endpoints;

import com.beligum.base.templating.ifaces.Template;
import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.security.Permissions;
import gen.com.beligum.blocks.core.fs.html.views.modals.change_url_modal;
import org.apache.shiro.authz.annotation.RequiresRoles;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
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
    public static final String CHANGE_URL_MODAL = "change-url-modal.vm";

    @GET
    @Path("/changeurl")
    @RequiresRoles(Permissions.ADMIN_ROLE_NAME)
    public Response getChangeUrlModal(
                    @QueryParam("original")
                    String originalUrl) throws MalformedURLException
    {
        Template template = change_url_modal.get().getNewTemplate();
        String originalPath = new URL(originalUrl).getPath();
        String[] splitted = originalPath.split("/");
        if (splitted.length > 2) {
            template.set("originalUrlPathEnd", "/" + splitted[splitted.length - 1]);
        }
        else if (splitted.length == 2) {
            //            if(Languages.isNonEmptyLanguageCode(splitted[1])){
            //                template.set("originalUrlPathEnd", "");
            //            }
            //            else{
            //                template.set("originalUrlPathEnd", "/" + splitted[splitted.length - 1]);
            //            }
        }
        else {
            template.set("originalUrlPathEnd", "");
        }
        template.set("languages", Arrays.asList(Blocks.config().getLanguages()));
        return Response.ok(template.render()).build();
    }

    //    @GET
    //    @Path("/newpage")
    //    @RequiresRoles(Permissions.ADMIN_ROLE_NAME)
    //    public Response getNewPageModal(
    //                    @QueryParam("entityurl")
    //                    String entityUrl) throws Exception
    //    {
    //        Template template = R.templateEngine().getEmptyTemplate("/views/modals/" + NEW_PAGE_MODAL);
    //        template.set("entityClasses", Blocks.templateCache().getPagetemplates(Blocks.putConfig().getDefaultLanguage()));
    //        template.set("entityUrl", entityUrl);
    //        return Response.ok(template.renderContent()).build();
    //    }
}
