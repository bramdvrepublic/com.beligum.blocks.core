package com.beligum.blocks.core.endpoints;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * Created by bas on 18.02.15.
 */
@Path("/translations")
public class TranslationsEndpoint
{
    @POST
    public Response newTranslation(@QueryParam("translation") String urlTranslation, @QueryParam("original") String originalUrl){
        //TODO BAS SH: you just implemented the round trip to translate a url, now the real work begins:
        //TODO 1) we need a xml holding a site-map which essentially is a tree of hashmaps holding translations of every url-depth
        //TODO 2) that tree will eventually point us to a db-id, where we can fetch the correct template
        //TODO 3) while rendering a template to html, all reference-to's encountered while parsing will hold the direct db-id, however all page-block resource-attributes should be translated to the most fitting url
        //TODO 4) when updating a page, in db a new field should be saved in a template's hash, it's parent-array as follows [20, 30, 50, 1] which holds references to translated words in the sitemap, which we will need when renaming entities that are already referred to in other blocks
        return Response.ok().build();
    }
}
