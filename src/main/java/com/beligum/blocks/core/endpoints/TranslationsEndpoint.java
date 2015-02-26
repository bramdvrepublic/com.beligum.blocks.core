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
        //TODO BAS!: while rendering a template to html, all reference-to's encountered while parsing will hold the direct db-id, however all page-block resource-attributes should be translated to the most fitting url
        //TODO BAS!: when updating a page, in db a new field should be saved in a template's hash, it's parent-array as follows [1.20, 2.30, 3.50, 4.1] which holds references to translated words in the sitemap, which we will need when renaming entities that are already referred to in other blocks
        URL translation = new URL(urlTranslation);
        URL original = new URL(originalUrl);
        BlocksID id = XMLUrlIdMapper.getInstance().getId(original);
        XMLUrlIdMapper.getInstance().put(id, translation);
        return Response.ok().build();
    }
}
