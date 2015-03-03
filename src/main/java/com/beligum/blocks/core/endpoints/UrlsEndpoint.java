package com.beligum.blocks.core.endpoints;

import com.beligum.blocks.core.URLMapping.XMLUrlIdMapper;
import com.beligum.blocks.core.identifiers.BlocksID;
import com.beligum.blocks.core.internationalization.Languages;
import com.beligum.core.framework.i18n.I18n;

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
        String[] originalUrlAndLanguage = Languages.translateUrl(originalUrl, Languages.NO_LANGUAGE);
        String language = originalUrlAndLanguage[1];
        if(language.equals(Languages.NO_LANGUAGE)){
            language="";
        }
        else{
            language = "/" + language;
        }
        if(!newPath.startsWith("/")){
            newPath = "/" + newPath;
        }
        newPath = language + newPath;
        URL newURL = null;
        URL original = null;
        try {
            original = new URL(originalUrl);
            newURL = new URL(original, newPath);
        }catch(Exception e){
            return Response.status(Response.Status.BAD_REQUEST).entity(I18n.instance().getMessage("badChangeUrl")).build();
        }
        BlocksID id = XMLUrlIdMapper.getInstance().getId(original);
        XMLUrlIdMapper.getInstance().put(id, newURL);
        return Response.ok(newURL.toString()).build();
    }
}
