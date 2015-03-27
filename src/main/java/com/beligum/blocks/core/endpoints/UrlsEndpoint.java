package com.beligum.blocks.core.endpoints;

import com.beligum.blocks.core.base.Blocks;
import com.beligum.blocks.core.urlmapping.redis.SiteMap;
import com.beligum.blocks.core.urlmapping.redis.XMLUrlIdMapper;
import com.beligum.blocks.core.exceptions.UrlIdMappingException;
import com.beligum.blocks.core.identifiers.redis.BlocksID;
import com.beligum.core.framework.i18n.I18n;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
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
            if(!newPath.startsWith("/")){
                newPath = "/" + newPath;
            }
            //construct the new url relative to the old one (so http://www.beligum.com/home/apage becomes http://www.beligum.com/home/anotherpage when the new path is "anotherpage")
            newPath = "." + newPath;
            newURL = new URL(original, newPath);
        }catch(Exception e){
            return Response.status(Response.Status.BAD_REQUEST).entity(I18n.instance().getMessage("badChangeUrl")).build();
        }
//        BlocksID id = XMLUrlIdMapper.getInstance().getId(original);
//        XMLUrlIdMapper.getInstance().put(id, newURL);
        return Response.ok(newURL.toString()).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public SiteMap getSiteMap(@QueryParam("lang") String language) throws UrlIdMappingException
    {
        if(language == null){
            language = Blocks.config().getDefaultLanguage();
        }
        return XMLUrlIdMapper.getInstance().renderSiteMap(language);
    }


}
