package com.beligum.blocks.endpoints;

import com.beligum.base.i18n.I18nFactory;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.net.URL;

/**
 * Created by bas on 18.02.15.
 */
@Path("/urls")
public class UrlsEndpoint
{
    @POST
    public Response changeUrlIdMapping(
                    @QueryParam("original")
                    String originalUrl,
                    @QueryParam("newpath")
                    String newPath) throws Exception
    {
        URL newURL = null;
        URL original = null;
        try {
            original = new URL(originalUrl);
            //if no user filled in "/" is present in the newPath, we need to add one
            if (!newPath.startsWith("/")) {
                newPath = "/" + newPath;
            }
            //construct the new url relative to the old one (so http://www.beligum.com/home/apage becomes http://www.beligum.com/home/anotherpage when the new path is "anotherpage")
            newPath = "." + newPath;
            newURL = new URL(original, newPath);
        }
        catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(I18nFactory.instance().getBrowserResourceBundle().getMessage("badChangeUrl")).build();
        }
        //        BlocksID id = XMLUrlIdMapper.getInstance().getId(original);
        //        XMLUrlIdMapper.getInstance().put(id, newURL);
        return Response.ok(newURL.toString()).build();
    }



}
