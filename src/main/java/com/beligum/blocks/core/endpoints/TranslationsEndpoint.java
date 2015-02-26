package com.beligum.blocks.core.endpoints;

import com.beligum.blocks.core.URLMapping.XMLUrlIdMapper;
import com.beligum.blocks.core.identifiers.BlocksID;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.net.URL;

/**
 * Created by bas on 18.02.15.
 */
@Path("/translations")
public class TranslationsEndpoint
{
    @POST
    public Response newTranslation(@QueryParam("translation") String urlTranslation, @QueryParam("original") String originalUrl) throws Exception
    {
        URL translation = new URL(urlTranslation);
        URL original = new URL(originalUrl);
        BlocksID id = XMLUrlIdMapper.getInstance().getId(original);
        XMLUrlIdMapper.getInstance().put(id, translation);
        return Response.ok().build();
    }
}
